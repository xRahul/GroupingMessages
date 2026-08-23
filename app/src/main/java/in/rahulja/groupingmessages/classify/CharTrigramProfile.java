package in.rahulja.groupingmessages.classify;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CharTrigramProfile {

  private static final char PADDING = '_';
  private static final int TRIGRAM_LENGTH = 3;

  private CharTrigramProfile() {
  }

  public static Map<String, Integer> of(String normalizedText) {
    Map<String, Integer> profile = new LinkedHashMap<>();
    if (normalizedText == null || normalizedText.isEmpty()) {
      return profile;
    }
    String padded = PADDING + normalizedText + PADDING;
    for (int i = 0; i + TRIGRAM_LENGTH <= padded.length(); i++) {
      String trigram = padded.substring(i, i + TRIGRAM_LENGTH);
      Integer count = profile.get(trigram);
      profile.put(trigram, count == null ? 1 : count + 1);
    }
    return profile;
  }

  public static double dice(Map<String, Integer> a, Map<String, Integer> b) {
    if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
      return 0.0;
    }
    long intersectionSum = 0L;
    long totalB = 0L;
    for (Map.Entry<String, Integer> entry : b.entrySet()) {
      totalB += entry.getValue();
      Integer countInA = a.get(entry.getKey());
      if (countInA != null) {
        intersectionSum += Math.min(countInA, entry.getValue());
      }
    }
    long totalA = 0L;
    for (Integer count : a.values()) {
      totalA += count;
    }
    if (totalA + totalB == 0L) {
      return 0.0;
    }
    return (2.0 * intersectionSum) / (totalA + totalB);
  }
}
