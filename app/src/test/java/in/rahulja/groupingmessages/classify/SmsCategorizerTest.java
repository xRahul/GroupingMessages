package in.rahulja.groupingmessages.classify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SmsCategorizerTest {

  private static final double DELTA = 1e-9;
  private static final List<String> STOPWORDS =
      Collections.unmodifiableList(Arrays.asList("the", "is", "your", "to"));

  private static SmsCategorizer.Batch balancedBatch(String... exemplars) {
    return SmsCategorizer.Batch.build(Arrays.asList(exemplars), STOPWORDS,
        SmsCategorizer.MODE_BALANCED);
  }

  @Test
  public void resolveModeKeepsSupportedModes() {
    assertEquals(SmsCategorizer.MODE_BALANCED, SmsCategorizer.resolveMode("balanced"));
    assertEquals(SmsCategorizer.MODE_WORDS_ONLY, SmsCategorizer.resolveMode("wordsOnly"));
    assertEquals(SmsCategorizer.MODE_CHARACTERS_ONLY,
        SmsCategorizer.resolveMode("charactersOnly"));
    assertEquals(SmsCategorizer.MODE_LEGACY_LEVENSHTEIN,
        SmsCategorizer.resolveMode("legacyLevenshtein"));
  }

  @Test
  public void resolveModeMapsLegacyStoredValuesToLegacyLevenshtein() {
    assertEquals(SmsCategorizer.MODE_LEGACY_LEVENSHTEIN,
        SmsCategorizer.resolveMode("levenshtein"));
    assertEquals(SmsCategorizer.MODE_LEGACY_LEVENSHTEIN,
        SmsCategorizer.resolveMode("jaroWinkler"));
  }

  @Test
  public void resolveModeMapsUnsetAndStaleValuesToBalanced() {
    assertEquals(SmsCategorizer.MODE_BALANCED, SmsCategorizer.resolveMode(null));
    assertEquals(SmsCategorizer.MODE_BALANCED, SmsCategorizer.resolveMode(""));
    assertEquals(SmsCategorizer.MODE_BALANCED, SmsCategorizer.resolveMode("dice"));
    assertEquals(SmsCategorizer.MODE_BALANCED,
        SmsCategorizer.resolveMode("normalizedLevenshtein"));
    assertEquals(SmsCategorizer.MODE_BALANCED, SmsCategorizer.resolveMode("notARealAlgorithm"));
  }

  @Test
  public void defaultThresholdIsRebaselinedForHybridModesAndPreservedForLegacy() {
    assertEquals(25, SmsCategorizer.defaultThresholdPercent(SmsCategorizer.MODE_BALANCED));
    assertEquals(25, SmsCategorizer.defaultThresholdPercent(SmsCategorizer.MODE_WORDS_ONLY));
    assertEquals(25,
        SmsCategorizer.defaultThresholdPercent(SmsCategorizer.MODE_CHARACTERS_ONLY));
    assertEquals(80,
        SmsCategorizer.defaultThresholdPercent(SmsCategorizer.MODE_LEGACY_LEVENSHTEIN));
  }

  @Test
  public void identicalQueryScoresPerfectAgainstItsOwnExemplarEntry() {
    SmsCategorizer.Batch batch = balancedBatch(
        "rs 2500 debited from account balance low",
        "get flat 50 percent off sale now");
    double[] scores = batch.scores("rs 2500 debited from account balance low");
    assertEquals(2, scores.length);
    assertEquals(1.0, scores[0], DELTA);
    assertTrue(scores[1] < 1.0);
  }

  @Test
  public void scoresAreReturnedInPipelineOrderAcrossDisjointExemplars() {
    SmsCategorizer.Batch batch = balancedBatch("bank debit account", "sale discount offer");
    double[] scores = batch.scores("bank debit account extra fees");
    assertTrue(scores[0] > scores[1]);
    double[] reversed = batch.scores("mega sale discount offer today");
    assertTrue(reversed[1] > reversed[0]);
  }

  @Test
  public void wordsOnlyModeIgnoresCharacterNoise() {
    // note: a 1-document corpus makes every idf ln(2/2)=0, so >=2 exemplars required
    SmsCategorizer.Batch words = SmsCategorizer.Batch.build(
        Arrays.asList(
            "your savings account has been credited successfully today morning",
            "weather report rain expected city outskirts"),
        STOPWORDS, SmsCategorizer.MODE_WORDS_ONLY);
    double[] typoScores = words.scores(
        "savings account has been creditedd successfully today morning extra");
    double[] disjointScores = words.scores("weather report rain expected city outskirts");
    assertTrue("typo'd overlap should still score: " + typoScores[0], typoScores[0] > 0.3);
    assertTrue(typoScores[0] > disjointScores[0]);
  }

  @Test
  public void charactersOnlyModeScoresTyposAboveWordMismatch() {
    SmsCategorizer.Batch chars = SmsCategorizer.Batch.build(
        Arrays.asList("meetng tommrw ok"), STOPWORDS, SmsCategorizer.MODE_CHARACTERS_ONLY);
    double[] typo = chars.scores("meeting tomorrow ok");
    double[] unrelated = chars.scores("xyz qqq www");
    assertTrue(typo[0] > 0.5);
    assertTrue(unrelated[0] < typo[0]);
  }

  @Test
  public void legacyModeMatchesSimmetricsLevenshteinOnCleanedStrings() {
    SmsCategorizer.Batch legacy = SmsCategorizer.Batch.build(
        Arrays.asList("hello world"), STOPWORDS, SmsCategorizer.MODE_LEGACY_LEVENSHTEIN);
    assertEquals(1.0, legacy.scores("hello world")[0], DELTA);
    assertEquals(0.0, legacy.scores("abc def ghi")[0], 1e-6);
  }

  @Test
  public void emptyCorpusYieldsNoScores() {
    SmsCategorizer.Batch batch = SmsCategorizer.Batch.build(
        Collections.<String>emptyList(), STOPWORDS, SmsCategorizer.MODE_BALANCED);
    assertEquals(0, batch.size());
  }

  @Test
  public void documentFrequencyCountsDocumentsContainingEachTermOnce() {
    List<String> documents = Arrays.asList("alpha beta", "alpha gamma", "delta alpha beta beta");
    Map<String, Integer> df = SmsCategorizer.buildDocumentFrequency(documents,
        new TextVectorizer(STOPWORDS));
    assertEquals(Integer.valueOf(3), df.get("alpha"));
    assertEquals(Integer.valueOf(2), df.get("beta"));
    assertEquals(Integer.valueOf(1), df.get("gamma"));
    assertEquals(Integer.valueOf(1), df.get("delta"));
    LinkedHashMap<String, Integer> unused = new LinkedHashMap<>();
    assertTrue(unused.isEmpty());
  }
}
