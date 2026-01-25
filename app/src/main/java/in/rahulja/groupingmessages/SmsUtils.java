package in.rahulja.groupingmessages;

import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

public class SmsUtils {

  private static final String[] stopWordsofwordnet = {
      "a", "about", "above", "after", "again", "against", "all", "also", "am", "an", "and", "any", "are", "arent", "as", "at",
      "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can", "cannot", "cant", "could", "couldnt",
      "did", "didnt", "do", "does", "doesnt", "doing", "dont", "down", "due", "during", "each", "every", "few", "for", "from", "further",
      "had", "hadnt", "has", "hasnt", "have", "havent", "having", "he", "hed", "hell", "her", "here", "heres", "hers", "herself", "hes",
      "him", "himself", "his", "how", "hows", "i", "id", "if", "ill", "im", "in", "into", "is", "isnt", "it", "its", "itself", "ive",
      "lets", "like", "many", "may", "me", "might", "more", "most", "much", "must", "mustnt", "my", "myself", "no", "nor", "not",
      "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "see",
      "shant", "she", "shed", "shell", "shes", "should", "shouldnt", "so", "some", "such", "than", "that", "thats", "the", "their",
      "theirs", "them", "themselves", "then", "there", "theres", "these", "they", "theyd", "theyll", "theyre", "theyve", "this",
      "those", "through", "to", "too", "under", "unless", "until", "up", "very", "was", "wasnt", "we", "wed", "well", "were", "werent",
      "weve", "what", "whats", "when", "whens", "where", "wheres", "which", "while", "who", "whom", "whos", "why", "whys", "will",
      "with", "without", "wont", "would", "wouldnt", "you", "youd", "youll", "your", "youre", "yours", "yourself", "yourselves", "youve"
  };
  private static final Set<String> stopWordsSet = new HashSet<>(Arrays.asList(stopWordsofwordnet));

  private SmsUtils() {
    // empty constructor
  }

  public static String cleanString(String s) {
    if (s == null) return "";
    String[] words = s.replaceAll("[\\p{P}]", " ")
        .replaceAll("\\s+", " ")
        .replaceAll("\\d", "1")
        .toLowerCase()
        .split(" ");
    StringBuilder wordsList = new StringBuilder();

    for (String word : words) {
      if (!stopWordsSet.contains(word)) {
        wordsList.append(" ").append(word);
      }
    }

    return wordsList.toString();
  }

  public static double getSmsSimilarityScore(String algo, String sms1, String sms2) {
    Method method;
    try {
      method = StringMetrics.class.getMethod(algo);
      StringMetric m = (StringMetric) method.invoke(null);
      return m.compare(sms1, sms2);
    } catch (IllegalAccessException | InvocationTargetException | SecurityException | NoSuchMethodException e) {
      // In unit tests, Log might not be mocked.
      try {
        Log.e("GM/simError", e.toString());
      } catch (RuntimeException re) {
        System.err.println("GM/simError: " + e.toString());
      }
      return 0.0;
    }
  }
}
