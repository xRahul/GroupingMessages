package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import in.rahulja.groupingmessages.classify.SmsCategorizer;
import in.rahulja.groupingmessages.db.SmsDao;

@SuppressWarnings("WeakerAccess") public class TrainSms {

  private static final String CLEAN_SMS = DatabaseContract.Sms.KEY_CLEANED_SMS;
  private static String[] stopWordsofwordnet = {
      "without", "see", "unless", "due", "also", "must", "might", "like", "will", "may", "can",
      "much", "every", "the", "in", "other", "this", "the", "many", "any", "an", "or", "for", "in",
      "an", "is", "a", "about", "above", "after", "again", "against", "all", "am", "an",
      "and", "any", "are", "arent", "as", "at", "be", "because", "been", "before", "being", "below",
      "between", "both", "but", "by", "cant", "cannot", "could", "couldnt", "did", "didnt", "do",
      "does", "doesnt", "doing", "dont", "down", "during", "each", "few", "for", "from", "further",
      "had", "hadnt", "has", "hasnt", "have", "havent", "having", "he", "hed", "hell", "hes", "her",
      "here", "heres", "hers", "herself", "him", "himself", "his", "how", "hows", "id",
      "ill", "im", "ive", "if", "in", "into", "is", "isnt", "it", "its", "its", "itself", "lets",
      "me", "more", "most", "mustnt", "my", "myself", "no", "nor", "not", "of", "off", "on", "once",
      "only", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shant", "she",
      "shed", "shell", "shes", "should", "shouldnt", "so", "some", "such", "than", "that", "thats",
      "their", "theirs", "them", "themselves", "then", "there", "theres", "these", "they", "theyd",
      "theyll", "theyre", "theyve", "this", "those", "through", "to", "too", "under", "until", "up",
      "very", "was", "wasnt", "we", "wed", "well", "were", "weve", "were", "werent", "what",
      "whats", "when", "whens", "where", "wheres", "which", "while", "who", "whos", "whom", "why",
      "whys", "with", "wont", "would", "wouldnt", "you", "youd", "youll", "youre", "youve", "your",
      "yours", "yourself", "yourselves"
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
    String mode = SmsCategorizer.resolveMode(prefs.getString("key_similarity_algorithm", null));
    double limitSimScore = prefs.getInt("key_similarity_score",
        SmsCategorizer.defaultThresholdPercent(mode)) / (double) 100;

    List<String> exemplarTexts = cleanedTextsOf(cleanedSmsListToTrainAgainst);
    SmsCategorizer.Batch batch =
        SmsCategorizer.Batch.build(exemplarTexts, new ArrayList<>(stopWordsSet), mode);

    for (Map<String, String> toTrainSmsMap : cleanedSmsListToTrain) {

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

      // tie-break: first exemplar in pipeline order wins equal scores
      int bestExemplarIndex = -1;
      double highestSimScore = -1.0;
      double[] scores = batch.scores(toTrainSmsMap.get(CLEAN_SMS));
      for (int i = 0; i < scores.length; i++) {
        double tempSimScore = scores[i];
        if (tempSimScore >= limitSimScore && tempSimScore > highestSimScore) {
          Log.d("GM/SimNewSms", String.format(
              "%s %s %s smsDate=%s exemplarId=%s",
              String.valueOf(tempSimScore),
              String.valueOf(limitSimScore),
              String.valueOf(highestSimScore),
              toTrainSmsMap.get(DatabaseContract.Sms.KEY_DATE),
              cleanedSmsListToTrainAgainst.get(i).get(DatabaseContract.Sms._ID)
          ));
          bestExemplarIndex = i;
          highestSimScore = tempSimScore;
        }
      }

      if (bestExemplarIndex != -1) {
        Map<String, String> exemplarMap = cleanedSmsListToTrainAgainst.get(bestExemplarIndex);
        toTrainSmsMap.put(
            DatabaseContract.Sms.KEY_SIM_SCORE,
            String.valueOf(highestSimScore)
        );
        toTrainSmsMap.put(
            DatabaseContract.Sms.KEY_CATEGORY_ID,
            exemplarMap.get(DatabaseContract.Sms.KEY_CATEGORY_ID)
        );
        toTrainSmsMap.put(
            DatabaseContract.Sms.KEY_SIMILAR_TO,
            exemplarMap.get(DatabaseContract.Sms._ID)
        );
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

  /**
   * Cleans the SMS string by removing punctuation, converting digits to '1',
   * and filtering stop words.
   * Optimized to use single-pass character iteration to avoid Regex overhead
   * and reduce garbage collection.
   */
  private static String cleanString(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder(s.length());
    StringBuilder currentWord = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      if (Character.isDigit(c)) {
        currentWord.append('1');
      } else if (isPunctuation(c) || Character.isWhitespace(c)) {
        if (currentWord.length() > 0) {
          String word = currentWord.toString();
          if (!stopWordsSet.contains(word)) {
            sb.append(" ").append(word);
          }
          currentWord.setLength(0);
        }
      } else {
        currentWord.append(Character.toLowerCase(c));
      }
    }

    if (currentWord.length() > 0) {
      String word = currentWord.toString();
      if (!stopWordsSet.contains(word)) {
        sb.append(" ").append(word);
      }
    }

    return sb.toString();
  }

  private static boolean isPunctuation(char c) {
    int type = Character.getType(c);
    return type == Character.START_PUNCTUATION ||
        type == Character.END_PUNCTUATION ||
        type == Character.OTHER_PUNCTUATION ||
        type == Character.CONNECTOR_PUNCTUATION ||
        type == Character.DASH_PUNCTUATION ||
        type == Character.INITIAL_QUOTE_PUNCTUATION ||
        type == Character.FINAL_QUOTE_PUNCTUATION;
  }

  public static List<Map<String, String>> retrainExistingSms(Context context,
      Map<String, String> trainedSms) {

    List<Map<String, String>> allSms = SmsDao.getAll(context);
    List<Map<String, String>> reTrainedSmsList = new ArrayList<>();
    List<Map<String, String>> cleanedAllSms = cleanListOfSms(allSms);
    Map<String, String> cleanedTrainedSms = cleanSmsMap(trainedSms);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String mode = SmsCategorizer.resolveMode(prefs.getString("key_similarity_algorithm", null));
    double limitSimScore = prefs.getInt("key_similarity_score",
        SmsCategorizer.defaultThresholdPercent(mode)) / (double) 100;

    List<String> candidateTexts = cleanedTextsOf(cleanedAllSms);
    SmsCategorizer.Batch batch =
        SmsCategorizer.Batch.build(candidateTexts, new ArrayList<>(stopWordsSet), mode);
    double[] scores = batch.scores(cleanedTrainedSms.get(CLEAN_SMS));

    for (int i = 0; i < cleanedAllSms.size(); i++) {

      Map<String, String> toTrainSmsMap = cleanedAllSms.get(i);

      double highestSimScore =
          Double.parseDouble(toTrainSmsMap.get(DatabaseContract.Sms.KEY_SIM_SCORE));

      double tempSimScore = scores[i];

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

  private static List<String> cleanedTextsOf(List<Map<String, String>> smsList) {
    List<String> texts = new ArrayList<>(smsList.size());
    for (Map<String, String> smsMap : smsList) {
      texts.add(smsMap.get(CLEAN_SMS));
    }
    return texts;
  }
}
