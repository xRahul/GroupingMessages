package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

class TrainSms {

  private static final String CLEAN_SMS = DatabaseContract.Sms.KEY_CLEANED_SMS;
  private static final int LIMIT_SIM_SCORE = 80;
  private static String[] stopWordsofwordnet = {
      "without", "see", "unless", "due", "also", "must", "might", "like", "will", "may", "can",
      "much", "every", "the", "in", "other", "this", "the", "many", "any", "an", "or", "for", "in",
      "an", "an ", "is", "a", "about", "above", "after", "again", "against", "all", "am", "an",
      "and", "any", "are", "arent", "as", "at", "be", "because", "been", "before", "being", "below",
      "between", "both", "but", "by", "cant", "cannot", "could", "couldnt", "did", "didnt", "do",
      "does", "doesnt", "doing", "dont", "down", "during", "each", "few", "for", "from", "further",
      "had", "hadnt", "has", "hasnt", "have", "havent", "having", "he", "hed", "hell", "hes", "her",
      "here", "heres", "hers", "herself", "him", "himself", "his", "how", "hows", "i ", " i", "id",
      "ill", "im", "ive", "if", "in", "into", "is", "isnt", "it", "its", "its", "itself", "lets",
      "me", "more", "most", "mustnt", "my", "myself", "no", "nor", "not", "of", "off", "on", "once",
      "only", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shant", "she",
      "shed", "shell", "shes", "should", "shouldnt", "so", "some", "such", "than", "that", "thats",
      "their", "theirs", "them", "themselves", "then", "there", "theres", "these", "they", "theyd",
      "theyll", "theyre", "theyve", "this", "those", "through", "to", "too", "under", "until", "up",
      "very", "was", "wasnt", "we", "wed", "well", "were", "weve", "were", "werent", "what",
      "whats", "when", "whens", "where", "wheres", "which", "while", "who", "whos", "whom", "why",
      "whys", "with", "wont", "would", "wouldnt", "you", "youd", "youll", "youre", "youve", "your",
      "yours", "yourself", "yourselves", "without", "see", "unless", "due", "also", "must", "might",
      "like", "will", "may", "can", "much", "every", "the", "in", "other", "this", "the", "many",
      "any", "an", "or", "for", "in", "an", "an ", "is", "a", "about", "above", "after", "again",
      "against", "all", "am", "an", "and", "any", "are", "arent", "as", "at", "be", "because",
      "been", "before", "being", "below", "between", "both", "but", "by", "cant", "cannot", "could",
      "couldnt", "did", "didnt", "do", "does", "doesnt", "doing", "dont", "down", "during", "each",
      "few", "for", "from", "further", "had", "hadnt", "has", "hasnt", "have", "havent", "having",
      "he", "hed", "hell", "hes", "her", "here", "heres", "hers", "herself", "him", "himself",
      "his", "how", "hows", "i ", " i", "id", "ill", "im", "ive", "if", "in", "into", "is", "isnt",
      "it", "its", "its", "itself", "lets", "me", "more", "most", "mustnt", "my", "myself", "no",
      "nor", "not", "of", "off", "on", "once", "only", "ought", "our", "ours", "ourselves", "out",
      "over", "own", "same", "shant", "she", "shed", "shell", "shes", "should", "shouldnt", "so",
      "some", "such", "than", "that", "thats", "their", "theirs", "them", "themselves", "then",
      "there", "theres", "these", "they", "theyd", "theyll", "theyre", "theyve", "this", "those",
      "through", "to", "too", "under", "until", "up", "very", "was", "wasnt", "we", "wed", "well",
      "were", "weve", "were", "werent", "what", "whats", "when", "whens", "where", "wheres",
      "which", "while", "who", "whos", "whom", "why", "whys", "with", "wont", "would", "wouldnt",
      "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves"
  };
  private static final Set<String> stopWordsSet = new HashSet<>(Arrays.asList(stopWordsofwordnet));

  private TrainSms() {
    // empty constructor
  }

  public static List<Map<String, String>> getTrainedListOfSms(Context context,
      List<Map<String, String>> smsListToTrain, List<Map<String, String>> smsListToTrainAgainst) {

    List<Map<String, String>> trainedLatestSmsList = new ArrayList<>();

    List<Map<String, String>> cleanedSmsListToTrain = cleanListOfSms(smsListToTrain);
    List<Map<String, String>> cleanedSmsListToTrainAgainst = cleanListOfSms(smsListToTrainAgainst);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String simAlgo = prefs.getString("key_similarity_algorithm", "levenshtein");
    double limitSimScore = prefs.getInt("key_similarity_score", LIMIT_SIM_SCORE) / (double) 100;

    for (Map<String, String> toTrainSmsMap : cleanedSmsListToTrain) {

      double highestSimScore = 0.0;
      toTrainSmsMap.put(
          DatabaseContract.Sms.KEY_SIM_SCORE,
          String.valueOf(0.0)
      );
      toTrainSmsMap.put(
          DatabaseContract.Sms.KEY_CATEGORY_ID,
          String.valueOf(1)
      );
      toTrainSmsMap.put(
          DatabaseContract.Sms.KEY_VISIBILITY,
          String.valueOf(1)
      );
      toTrainSmsMap.put(
          DatabaseContract.Sms.KEY_SIMILAR_TO,
          String.valueOf(0)
      );
      toTrainSmsMap.put(
          DatabaseContract.Sms.KEY_SENDER_TYPE,
          String.valueOf(-1)
      );

      for (Map<String, String> toTrainAgainstSmsMap : cleanedSmsListToTrainAgainst) {

        double tempSimScore = getSmsSimilarityScore(
            simAlgo,
            toTrainSmsMap.get(CLEAN_SMS),
            toTrainAgainstSmsMap.get(CLEAN_SMS)
        );

        if (tempSimScore >= limitSimScore && tempSimScore >= highestSimScore) {
          Log.d("GM/SimNewSms", String.format(
              "%s %s %s %s",
              String.valueOf(tempSimScore),
              String.valueOf(limitSimScore),
              String.valueOf(highestSimScore),
              toTrainSmsMap.toString()
          ));
          toTrainSmsMap.put(
              DatabaseContract.Sms.KEY_SIM_SCORE,
              String.valueOf(tempSimScore)
          );
          toTrainSmsMap.put(
              DatabaseContract.Sms.KEY_CATEGORY_ID,
              toTrainAgainstSmsMap.get(DatabaseContract.Sms.KEY_CATEGORY_ID)
          );
          toTrainSmsMap.put(
              DatabaseContract.Sms.KEY_SIMILAR_TO,
              toTrainAgainstSmsMap.get(DatabaseContract.Sms._ID)
          );
          highestSimScore = tempSimScore;
        }
      }

      trainedLatestSmsList.add(toTrainSmsMap);
    }

    Log.i("GM/getTrainedListOfSms", "Trained Latest SMS count: " + trainedLatestSmsList.size());
    return trainedLatestSmsList;
  }

  private static List<Map<String, String>> cleanListOfSms(List<Map<String, String>> smsList) {

    List<Map<String, String>> cleanedSmsList = new ArrayList<>();

    for (Map<String, String> smsMap : smsList) {
      cleanedSmsList.add(cleanSmsMap(smsMap));
    }

    return cleanedSmsList;
  }

  private static Map<String, String> cleanSmsMap(Map<String, String> sms) {

    if (sms.get(CLEAN_SMS) == null ||
        (sms.get(CLEAN_SMS) != null && sms.get(CLEAN_SMS).isEmpty())
        ) {
      sms.put(
          CLEAN_SMS,
          cleanString(sms.get(DatabaseContract.Sms.KEY_ADDRESS)
              + " "
              + sms.get(DatabaseContract.Sms.KEY_BODY))
      );
    }

    return sms;
  }

  private static String cleanString(String s) {
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

  public static List<Map<String, String>> retrainExistingSms(Context context,
      Map<String, String> trainedSms) {

    List<Map<String, String>> allSms = DatabaseBridge.getAllSms(context);
    List<Map<String, String>> reTrainedSmsList = new ArrayList<>();
    List<Map<String, String>> cleanedAllSms = cleanListOfSms(allSms);
    Map<String, String> cleanedTrainedSms = cleanSmsMap(trainedSms);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String simAlgo = prefs.getString("key_similarity_algorithm", "levenshtein");
    double limitSimScore = prefs.getInt("key_similarity_score", LIMIT_SIM_SCORE) / (double) 100;

    for (Map<String, String> toTrainSmsMap : cleanedAllSms) {

      double highestSimScore =
          Double.parseDouble(toTrainSmsMap.get(DatabaseContract.Sms.KEY_SIM_SCORE));

      double tempSimScore = getSmsSimilarityScore(
          simAlgo,
          toTrainSmsMap.get(CLEAN_SMS),
          cleanedTrainedSms.get(CLEAN_SMS)
      );

      if (tempSimScore >= limitSimScore && tempSimScore >= highestSimScore) {
        toTrainSmsMap.put(
            DatabaseContract.Sms.KEY_SIM_SCORE,
            String.valueOf(tempSimScore)
        );
        toTrainSmsMap.put(
            DatabaseContract.Sms.KEY_CATEGORY_ID,
            cleanedTrainedSms.get(DatabaseContract.Sms.KEY_CATEGORY_ID)
        );
        toTrainSmsMap.put(
            DatabaseContract.Sms.KEY_SIMILAR_TO,
            cleanedTrainedSms.get(DatabaseContract.Sms._ID)
        );
        reTrainedSmsList.add(toTrainSmsMap);
      }
    }

    return reTrainedSmsList;
  }

  private static double getSmsSimilarityScore(String algo, String sms1, String sms2) {
    Method method;
    try {
      method = StringMetrics.class.getMethod(algo);
      StringMetric m = (StringMetric) method.invoke(null);
      return m.compare(sms1, sms2);
    } catch (IllegalAccessException | InvocationTargetException | SecurityException | NoSuchMethodException e) {
      Log.e("GM/simError", e.toString());
      return 0.0;
    }
  }
}
