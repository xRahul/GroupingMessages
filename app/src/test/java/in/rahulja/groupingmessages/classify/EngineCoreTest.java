package in.rahulja.groupingmessages.classify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EngineCoreTest {

  private static final double DELTA = 1e-9;
  private static final double WORD_WEIGHT = 0.6;
  private static final double CHAR_WEIGHT = 0.4;
  private static final double THRESHOLD = 0.20;
  private static final String UNKNOWN = "Unknown";

  private static final List<String> STOPWORDS = Collections.unmodifiableList(Arrays.asList(
      "without", "see", "unless", "due", "also", "must", "might", "like", "will", "may", "can",
      "much", "every", "the", "in", "other", "this", "many", "any", "an", "or", "for", "is", "a",
      "about", "above", "after", "again", "against", "all", "am", "and", "are", "as", "at", "be",
      "because", "been", "before", "being", "below", "between", "both", "but", "by", "cant",
      "cannot", "could", "did", "didnt", "do", "does", "doesnt", "doing", "dont", "down", "each",
      "few", "from", "had", "has", "have", "having", "he", "her", "here", "hers", "him", "his",
      "how", "if", "into", "it", "its", "itself", "lets", "me", "more", "most", "my", "myself",
      "no", "nor", "not", "of", "off", "on", "once", "only", "our", "out", "over", "own", "same",
      "she", "should", "so", "some", "such", "than", "that", "their", "them", "then", "there",
      "these", "they", "those", "through", "to", "too", "under", "until", "up", "very", "was",
      "we", "well", "were", "what", "when", "where", "which", "while", "who", "whom", "why",
      "with", "would", "you", "your", "yours"));

  private TextVectorizer vectorizer;

  @Before
  public void setUp() {
    vectorizer = new TextVectorizer(STOPWORDS);
  }

  private static Map<String, Integer> buildDocumentFrequency(List<String> documents) {
    TextVectorizer corpusVectorizer = new TextVectorizer(STOPWORDS);
    Map<String, Integer> documentFrequency = new LinkedHashMap<>();
    for (String document : documents) {
      for (String term : corpusVectorizer.tfIdfVector(document, documentFrequency, 0).keySet()) {
        Integer count = documentFrequency.get(term);
        documentFrequency.put(term, count == null ? 1 : count + 1);
      }
    }
    return documentFrequency;
  }

  private static String[] classify(TextVectorizer vectorizer,
      Map<String, List<String>> trainingCorpus, Map<String, Integer> documentFrequency,
      long corpusSize, String message) {
    Map<String, Double> messageVector =
        vectorizer.tfIdfVector(message, documentFrequency, corpusSize);
    Map<String, Integer> messageProfile =
        CharTrigramProfile.of(TextVectorizer.normalize(message));
    String bestCategory = UNKNOWN;
    double bestScore = 0.0;
    for (Map.Entry<String, List<String>> entry : trainingCorpus.entrySet()) {
      for (String exemplar : entry.getValue()) {
        double score = hybridScore(vectorizer, documentFrequency, corpusSize, messageVector,
            messageProfile, exemplar);
        if (score >= bestScore) {
          bestScore = score;
          bestCategory = entry.getKey();
        }
      }
    }
    if (bestScore < THRESHOLD) {
      bestCategory = UNKNOWN;
    }
    return new String[] {bestCategory, String.valueOf(bestScore)};
  }

  private static double hybridScore(TextVectorizer vectorizer,
      Map<String, Integer> documentFrequency, long corpusSize,
      Map<String, Double> messageVector, Map<String, Integer> messageProfile, String exemplar) {
    double wordScore = TextVectorizer.cosine(messageVector,
        vectorizer.tfIdfVector(exemplar, documentFrequency, corpusSize));
    double charScore = CharTrigramProfile.dice(messageProfile,
        CharTrigramProfile.of(TextVectorizer.normalize(exemplar)));
    return WORD_WEIGHT * wordScore + CHAR_WEIGHT * charScore;
  }

  private static Map<String, Double> vec(Double... weights) {
    Map<String, Double> vector = new LinkedHashMap<>();
    for (int i = 0; i < weights.length; i++) {
      vector.put("t" + i, weights[i]);
    }
    return vector;
  }

  @Test
  public void normalizationIsLocaleIndependent() {
    Map<String, Double> referenceVector = vectorizer.tfIdfVector("LOGIN OTP IS 1234", null, 5);
    Locale originalLocale = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      assertEquals("login otp is 1234", TextVectorizer.normalize("LOGIN OTP IS 1234"));
      assertTrue(referenceVector.containsKey("login"));
      assertTrue(referenceVector.containsKey("otp"));
      assertFalse(referenceVector.containsKey("is"));
      assertEquals(referenceVector,
          vectorizer.tfIdfVector("LOGIN OTP IS 1234", null, 5));
    } finally {
      Locale.setDefault(originalLocale);
    }
  }

  @Test
  public void normalizeLowercasesAndCollapsesNonAlphanumerics() {
    assertEquals("hello world 123", TextVectorizer.normalize("Hello, WORLD!! 123..."));
    assertEquals("", TextVectorizer.normalize("!!!"));
    assertEquals("", TextVectorizer.normalize(""));
    assertEquals("", TextVectorizer.normalize(null));
  }

  @Test
  public void cosineIdenticalVectorsIsOne() {
    Map<String, Double> a = vec(0.5, 1.2, 0.3);
    assertEquals(1.0, TextVectorizer.cosine(a, a), 1e-9);
  }

  @Test
  public void cosineDisjointVectorsIsZero() {
    Map<String, Double> a = new LinkedHashMap<>();
    a.put("x", 1.0);
    Map<String, Double> b = new LinkedHashMap<>();
    b.put("y", 1.0);
    assertEquals(0.0, TextVectorizer.cosine(a, b), DELTA);
  }

  @Test
  public void cosineWithEmptyVectorIsZero() {
    assertEquals(0.0, TextVectorizer.cosine(new HashMap<String, Double>(), vec(1.0)), DELTA);
    assertEquals(0.0, TextVectorizer.cosine(vec(1.0), new HashMap<String, Double>()), DELTA);
  }

  @Test
  public void diceIdenticalProfilesIsOne() {
    Map<String, Integer> profile = CharTrigramProfile.of("_ok_");
    assertEquals(1.0, CharTrigramProfile.dice(profile, profile), DELTA);
  }

  @Test
  public void diceDisjointProfilesIsZero() {
    Map<String, Integer> a = CharTrigramProfile.of("_aa_");
    Map<String, Integer> b = CharTrigramProfile.of("_bb_");
    assertEquals(0.0, CharTrigramProfile.dice(a, b), DELTA);
  }

  @Test
  public void diceWithEmptyProfileIsZero() {
    Map<String, Integer> profile = CharTrigramProfile.of("_ok_");
    assertEquals(0.0, CharTrigramProfile.dice(
        CharTrigramProfile.of(""), profile), DELTA);
    assertEquals(0.0, CharTrigramProfile.dice(profile,
        CharTrigramProfile.of("")), DELTA);
  }

  @Test
  public void trigramProfilesIncludePaddedBoundariesWithCounts() {
    Map<String, Integer> profile = CharTrigramProfile.of("ok");
    assertEquals(Integer.valueOf(1), profile.get("_ok"));
    assertEquals(Integer.valueOf(1), profile.get("ok_"));
    assertEquals(2, profile.size());
    Map<String, Integer> repeated = CharTrigramProfile.of("aaa");
    assertEquals(Integer.valueOf(1), repeated.get("_aa"));
    assertEquals(Integer.valueOf(1), repeated.get("aaa"));
    assertEquals(Integer.valueOf(1), repeated.get("aa_"));
    assertEquals(3, repeated.size());
  }

  @Test
  public void diceBeatsWordCosineOnTypos() {
    String clean = "meeting tomorrow ok";
    String typo = TextVectorizer.normalize("meetng tommrw ok");
    double charScore = CharTrigramProfile.dice(
        CharTrigramProfile.of(clean), CharTrigramProfile.of(typo));
    Map<String, Integer> documentFrequency =
        buildDocumentFrequency(Arrays.asList(clean, typo, "ok fine done"));
    double wordScore = TextVectorizer.cosine(
        vectorizer.tfIdfVector(clean, documentFrequency, 3),
        vectorizer.tfIdfVector(typo, documentFrequency, 3));
    assertTrue("expected dice " + charScore + " > 0.5", charScore > 0.5);
    assertTrue("expected word cosine " + wordScore + " < dice " + charScore,
        wordScore < charScore);
  }

  @Test
  public void rareTermsGetHigherTfIdfWeightsWithinCorpusFixture() {
    List<String> corpus = Arrays.asList("alpha beta", "alpha gamma", "delta");
    Map<String, Integer> documentFrequency = buildDocumentFrequency(corpus);
    Map<String, Double> vector = vectorizer.tfIdfVector("alpha beta", documentFrequency, 3);
    assertEquals(Integer.valueOf(2), documentFrequency.get("alpha"));
    assertEquals(Integer.valueOf(1), documentFrequency.get("beta"));
    assertTrue(vector.get("beta") > vector.get("alpha"));
    Map<String, Double> deltaVector = vectorizer.tfIdfVector("delta", documentFrequency, 3);
    assertTrue(deltaVector.get("delta") > vector.get("alpha"));
  }

  @Test
  public void stopwordsAreDroppedFromVector() {
    Map<String, Double> vector = vectorizer.tfIdfVector("this is secret stuff", null, 5);
    assertFalse(vector.containsKey("this"));
    assertFalse(vector.containsKey("is"));
    assertTrue(vector.containsKey("secret"));
    assertTrue(vector.containsKey("stuff"));
    assertTrue(vectorizer.tfIdfVector("the and or", null, 5).isEmpty());
  }

  @Test
  public void emptyAndNullTextProduceEmptyVectors() {
    assertTrue(vectorizer.tfIdfVector("", null, 5).isEmpty());
    assertTrue(vectorizer.tfIdfVector(null, null, 5).isEmpty());
    assertTrue(vectorizer.tfIdfVector("!!! ...", null, 5).isEmpty());
  }

  @Test
  public void seededCorpusClassifiesUnseenMessagesIntoExpectedCategories() {
    Map<String, List<String>> corpus = TrainingCorpus.exemplars();
    List<String> documents = new ArrayList<>();
    for (List<String> exemplars : corpus.values()) {
      documents.addAll(exemplars);
    }
    Map<String, Integer> documentFrequency = buildDocumentFrequency(documents);
    long corpusSize = documents.size();

    Map<String, String> expectations = TrainingCorpus.unseenExpectations();
    StringBuilder table = new StringBuilder();
    boolean allCorrect = true;
    for (Map.Entry<String, String> expectation : expectations.entrySet()) {
      String[] result = classify(vectorizer, corpus, documentFrequency, corpusSize,
          expectation.getKey());
      boolean correct = expectation.getValue().equals(result[0]) && THRESHOLD <= Double
          .parseDouble(result[1]);
      allCorrect &= correct;
      table.append(String.format("%-58s -> %-10s score=%-8s expected %-10s %s%n",
          "\"" + expectation.getKey() + "\"", result[0], result[1], expectation.getValue(),
          correct ? "PASS" : "FAIL"));
    }
    System.out.println("=== Seeded corpus accuracy ===");
    System.out.print(table);
    assertTrue(allCorrect);
  }

  @Test
  public void belowThresholdMessagesFallBackToUnknown() {
    Map<String, List<String>> corpus = TrainingCorpus.exemplars();
    List<String> documents = new ArrayList<>();
    for (List<String> exemplars : corpus.values()) {
      documents.addAll(exemplars);
    }
    Map<String, Integer> documentFrequency = buildDocumentFrequency(documents);
    long corpusSize = documents.size();

    List<String> unrelated = Arrays.asList(
        "quick brown fox jumps over lazy dog near riverside",
        "lorem ipsum dolor sit amet consectetur adipiscing elit sed",
        "weather nice today enjoy your day friend");
    for (String message : unrelated) {
      String[] result = classify(vectorizer, corpus, documentFrequency, corpusSize, message);
      double bestScore = Double.parseDouble(result[1]);
      System.out.println("=== Fallback: \"" + message + "\" -> " + result[0]
          + " score=" + result[1]);
      assertEquals(UNKNOWN, result[0]);
      assertTrue("expected best score " + bestScore + " below threshold", bestScore < THRESHOLD);
    }
  }

  @Test
  public void classificationScoresAreDeterministicAcrossRuns() {
    Map<String, List<String>> corpus = TrainingCorpus.exemplars();
    List<String> documents = new ArrayList<>();
    for (List<String> exemplars : corpus.values()) {
      documents.addAll(exemplars);
    }
    Map<String, Integer> documentFrequency = buildDocumentFrequency(documents);
    long corpusSize = documents.size();

    Map<String, List<Double>> firstRun = scoreAll(corpus, documentFrequency, corpusSize);
    Map<String, List<Double>> secondRun = scoreAll(corpus, documentFrequency, corpusSize);
    assertEquals(firstRun, secondRun);
  }

  private Map<String, List<Double>> scoreAll(Map<String, List<String>> corpus,
      Map<String, Integer> documentFrequency, long corpusSize) {
    Map<String, List<Double>> scores = new LinkedHashMap<>();
    for (Map.Entry<String, String> expectation : TrainingCorpus.unseenExpectations().entrySet()) {
      List<Double> perCategory = new ArrayList<>();
      Map<String, Double> messageVector = vectorizer.tfIdfVector(expectation.getKey(),
          documentFrequency, corpusSize);
      Map<String, Integer> messageProfile =
          CharTrigramProfile.of(TextVectorizer.normalize(expectation.getKey()));
      for (Map.Entry<String, List<String>> category : corpus.entrySet()) {
        double best = 0.0;
        for (String exemplar : category.getValue()) {
          best = Math.max(best, hybridScore(vectorizer, documentFrequency, corpusSize,
              messageVector, messageProfile, exemplar));
        }
        perCategory.add(best);
      }
      scores.put(expectation.getKey(), perCategory);
    }
    return scores;
  }

  private static final class TrainingCorpus {

    private TrainingCorpus() {
    }

    static Map<String, List<String>> exemplars() {
      Map<String, List<String>> corpus = new LinkedHashMap<>();
      corpus.put("OTP", Arrays.asList(
          "use 123456 to login to your account do not share this otp with anyone",
          "your one time password is 456789 valid for 10 minutes only",
          "987654 is your verification code for secure account access"));
      corpus.put("Bank", Arrays.asList(
          "rs 2500 debited from account xx1234 on 12 05 available balance rs 15000",
          "your account xx9876 credited with rs 5000 on 15 06 ref no 223344",
          "rs 12000 spent on debit card ending 4321 at amazon on 03 07"));
      corpus.put("Promo", Arrays.asList(
          "get flat 50 percent off on all orders today limited time offer shop now",
          "mega sale up to 70 percent discount on shoes and fashion grab the deal now",
          "exclusive offer buy one get one free only for premium members this weekend"));
      corpus.put("Personal", Arrays.asList(
          "hey are you coming to the party tonight let me know soon",
          "thanks for the help yesterday really appreciate it call me when free",
          "lunch tomorrow at the usual place see you there at 1 pm"));
      return corpus;
    }

    static Map<String, String> unseenExpectations() {
      Map<String, String> expectations = new LinkedHashMap<>();
      expectations.put("your otp for netbanking login is 554433 do not share it", "OTP");
      expectations.put("otp 778899 valid till 15 minutes for transaction approval", "OTP");
      expectations.put("rs 750 debited from your account xx7712 balance is rs 9000 now", "Bank");
      expectations.put("credited rs 3000 to account xx4411 ref no 556677 thank you", "Bank");
      expectations.put("flat 40 percent off for you today hurry limited period offer", "Promo");
      expectations.put("sale alert extra discount on all orders grab deal now", "Promo");
      expectations.put("movie tonight instead of party call me after dinner", "Personal");
      expectations.put("meeting tomorrow at usual cafe see you there friend", "Personal");
      return expectations;
    }
  }
}
