package in.rahulja.groupingmessages.classify;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TextVectorizer {

  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

  private final Set<String> stopwords;

  public TextVectorizer(List<String> stopwords) {
    this.stopwords = stopwords == null
        ? new HashSet<String>()
        : new HashSet<>(stopwords);
  }

  public static String normalize(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    return NON_ALPHANUMERIC.matcher(raw.toLowerCase()).replaceAll(" ").trim();
  }

  public Map<String, Double> tfIdfVector(String text, Map<String, Integer> documentFrequency,
      long corpusSize) {
    String normalized = normalize(text);
    Map<String, Integer> termCounts = new LinkedHashMap<>();
    int tokenCount = 0;
    if (!normalized.isEmpty()) {
      for (String token : normalized.split(" ")) {
        if (token.isEmpty() || stopwords.contains(token)) {
          continue;
        }
        Integer count = termCounts.get(token);
        termCounts.put(token, count == null ? 1 : count + 1);
        tokenCount++;
      }
    }
    Map<String, Double> vector = new LinkedHashMap<>();
    if (tokenCount == 0) {
      return vector;
    }
    for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
      double tf = entry.getValue() / (double) tokenCount;
      int df = documentFrequency != null && documentFrequency.containsKey(entry.getKey())
          ? documentFrequency.get(entry.getKey())
          : 0;
      double idf = Math.log((corpusSize + 1) / (double) (df + 1));
      vector.put(entry.getKey(), tf * idf);
    }
    return vector;
  }

  public static double cosine(Map<String, Double> a, Map<String, Double> b) {
    if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
      return 0.0;
    }
    double dotProduct = 0.0;
    double normA = 0.0;
    for (Map.Entry<String, Double> entry : a.entrySet()) {
      normA += entry.getValue() * entry.getValue();
      Double valueB = b.get(entry.getKey());
      if (valueB != null) {
        dotProduct += entry.getValue() * valueB;
      }
    }
    double normB = 0.0;
    for (Double value : b.values()) {
      normB += value * value;
    }
    if (normA == 0.0 || normB == 0.0) {
      return 0.0;
    }
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
