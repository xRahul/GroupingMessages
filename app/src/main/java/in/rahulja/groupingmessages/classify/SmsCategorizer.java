package in.rahulja.groupingmessages.classify;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.Levenshtein;

/**
 * Mode-based scoring for the 1-NN categorization pipeline. A Batch precomputes
 * per-exemplar features once (tf-idf vectors + char trigram profiles over the
 * exemplar corpus) and scores any number of query messages against it.
 *
 * Modes: balanced = 0.6 word-cosine + 0.4 char-trigram-dice; wordsOnly = 1/0;
 * charactersOnly = 0/1; legacyLevenshtein = whole-string simmetrics Levenshtein
 * (semantics of the historical default pipeline).
 */
public final class SmsCategorizer {

  public static final String MODE_BALANCED = "balanced";
  public static final String MODE_WORDS_ONLY = "wordsOnly";
  public static final String MODE_CHARACTERS_ONLY = "charactersOnly";
  public static final String MODE_LEGACY_LEVENSHTEIN = "legacyLevenshtein";

  private static final double WORD_WEIGHT_BALANCED = 0.6;
  private static final double CHAR_WEIGHT_BALANCED = 0.4;
  private static final double WEIGHT_DISABLED = 0.0;
  private static final double WEIGHT_EXCLUSIVE = 1.0;

  private SmsCategorizer() {
    // empty constructor
  }

  /**
   * Maps stored preference values to a supported mode. Legacy stored values
   * levenshtein/jaroWinkler map to legacyLevenshtein so their threshold
   * semantics stay valid; unset or stale values fall back to balanced.
   */
  public static String resolveMode(String storedPrefValue) {
    if (storedPrefValue == null || storedPrefValue.isEmpty()) {
      return MODE_BALANCED;
    }
    switch (storedPrefValue) {
      case MODE_BALANCED:
      case MODE_WORDS_ONLY:
      case MODE_CHARACTERS_ONLY:
      case MODE_LEGACY_LEVENSHTEIN:
        return storedPrefValue;
      case "levenshtein":
      case "jaroWinkler":
        return MODE_LEGACY_LEVENSHTEIN;
      default:
        return MODE_BALANCED;
    }
  }

  /**
   * Default threshold percent per mode: hybrid scores live on a lower absolute
   * scale than whole-string similarity, so balanced-family modes re-baseline to
   * 25 while legacyLevenshtein keeps the historical default of 80.
   */
  public static int defaultThresholdPercent(String mode) {
    return MODE_LEGACY_LEVENSHTEIN.equals(mode) ? 80 : 25;
  }

  public static Map<String, Integer> buildDocumentFrequency(List<String> documents,
      TextVectorizer vectorizer) {
    Map<String, Integer> documentFrequency = new LinkedHashMap<>();
    if (documents == null) {
      return documentFrequency;
    }
    for (String document : documents) {
      for (String term : vectorizer.tfIdfVector(document, documentFrequency, 0).keySet()) {
        Integer count = documentFrequency.get(term);
        documentFrequency.put(term, count == null ? 1 : count + 1);
      }
    }
    return documentFrequency;
  }

  /** Immutable batch of exemplar features; one instance per classification pass. */
  public static final class Batch {

    private final String mode;
    private final TextVectorizer vectorizer;
    private final List<String> exemplarTexts;
    private final List<Map<String, Double>> exemplarVectors;
    private final List<Map<String, Integer>> exemplarProfiles;
    private final Map<String, Integer> documentFrequency;
    private final StringMetric legacyMetric;

    private Batch(String mode, TextVectorizer vectorizer, List<String> exemplarTexts,
        List<Map<String, Double>> exemplarVectors, List<Map<String, Integer>> exemplarProfiles,
        Map<String, Integer> documentFrequency, StringMetric legacyMetric) {
      this.mode = mode;
      this.vectorizer = vectorizer;
      this.exemplarTexts = exemplarTexts;
      this.exemplarVectors = exemplarVectors;
      this.exemplarProfiles = exemplarProfiles;
      this.documentFrequency = documentFrequency;
      this.legacyMetric = legacyMetric;
    }

    public static Batch build(List<String> exemplarTexts, List<String> stopwords, String mode) {
      List<String> texts = exemplarTexts == null
          ? new ArrayList<String>()
          : new ArrayList<>(exemplarTexts);
      TextVectorizer vectorizer = new TextVectorizer(stopwords);
      if (MODE_LEGACY_LEVENSHTEIN.equals(mode)) {
        return new Batch(mode, vectorizer, texts, null, null, null, new Levenshtein());
      }
      Map<String, Integer> documentFrequency = buildDocumentFrequency(texts, vectorizer);
      long corpusSize = texts.size();
      int size = texts.size();
      List<Map<String, Double>> vectors = new ArrayList<>(size);
      List<Map<String, Integer>> profiles = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        vectors.add(vectorizer.tfIdfVector(texts.get(i), documentFrequency, corpusSize));
        profiles.add(CharTrigramProfile.of(TextVectorizer.normalize(texts.get(i))));
      }
      return new Batch(mode, vectorizer, texts, vectors, profiles, documentFrequency, null);
    }

    public int size() {
      return exemplarTexts.size();
    }

    public String getMode() {
      return mode;
    }

    /**
     * Scores the cleaned query message against every exemplar in pipeline order.
     * Index i of the result corresponds to the i-th exemplar passed to build.
     */
    public double[] scores(String queryCleanedText) {
      double[] scores = new double[exemplarTexts.size()];
      if (legacyMetric != null) {
        for (int i = 0; i < scores.length; i++) {
          scores[i] = legacyMetric.compare(queryCleanedText, exemplarTexts.get(i));
        }
        return scores;
      }
      Map<String, Double> queryVector =
          vectorizer.tfIdfVector(queryCleanedText, documentFrequency, exemplarTexts.size());
      Map<String, Integer> queryProfile =
          CharTrigramProfile.of(TextVectorizer.normalize(queryCleanedText));
      for (int i = 0; i < scores.length; i++) {
        double wordScore = TextVectorizer.cosine(queryVector, exemplarVectors.get(i));
        double charScore = CharTrigramProfile.dice(queryProfile, exemplarProfiles.get(i));
        switch (mode) {
          case MODE_WORDS_ONLY:
            scores[i] = WEIGHT_EXCLUSIVE * wordScore + WEIGHT_DISABLED * charScore;
            break;
          case MODE_CHARACTERS_ONLY:
            scores[i] = WEIGHT_DISABLED * wordScore + WEIGHT_EXCLUSIVE * charScore;
            break;
          case MODE_BALANCED:
          default:
            scores[i] = WORD_WEIGHT_BALANCED * wordScore + CHAR_WEIGHT_BALANCED * charScore;
            break;
        }
      }
      return scores;
    }
  }
}
