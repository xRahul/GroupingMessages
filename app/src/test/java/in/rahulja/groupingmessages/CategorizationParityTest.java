package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import in.rahulja.groupingmessages.db.AppDatabase;
import in.rahulja.groupingmessages.db.SmsDao;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Behavioral contract harness for the categorization pipeline behind
 * TrainSms.getTrainedListOfSms. Fixed corpus of 6 exemplars across 2 categories,
 * fixed probe set. Golden assignments are updated ONLY in the dedicated
 * parity-evaluation step with an old-vs-new justification per probe.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class CategorizationParityTest {

  private static final long CATEGORY_BANK_ID = 2L;
  private static final long CATEGORY_PROMO_ID = 3L;

  private static final String[] BANK_EXEMPLARS = {
      "rs 2500 debited from account xx1234 on 12 05 available balance rs 15000",
      "your account xx9876 credited with rs 5000 on 15 06 ref no 223344",
      "rs 12000 spent on debit card ending 4321 at amazon on 03 07"
  };

  private static final String[] PROMO_EXEMPLARS = {
      "get flat 50 percent off on all orders today limited time offer shop now",
      "mega sale up to 70 percent discount on shoes and fashion grab the deal now",
      "exclusive offer buy one get one free only for premium members this weekend"
  };

  private static final String[] PROBES = {
      "rs 750 debited from your account xx7712 balance is rs 9000 now",
      "credited rs 3000 to account xx4411 ref no 556677 thank you",
      "flat 40 percent off for you today hurry limited period offer",
      "sale alert extra discount on all orders grab deal now",
      "quick brown fox jumps over lazy dog near riverside",
      "lorem ipsum dolor sit amet consectetur adipiscing elit sed"
  };

  private Context ctx;

  @Before
  public void setUp() throws Exception {
    ctx = RuntimeEnvironment.getApplication();
    Field f = DatabaseHelper.class.getDeclaredField("sInstance");
    f.setAccessible(true);
    DatabaseHelper inst = (DatabaseHelper) f.get(null);
    if (inst != null) {
      inst.close();
      f.set(null, null);
    }
    AppDatabase.get(ctx);
    seedExemplars();
  }

  private void seedExemplars() {
    seedCategory(CATEGORY_BANK_ID, BANK_EXEMPLARS);
    seedCategory(CATEGORY_PROMO_ID, PROMO_EXEMPLARS);
  }

  private void seedCategory(long categoryId, String[] bodies) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (int i = 0; i < bodies.length; i++) {
      Map<String, String> m = new LinkedHashMap<>();
      m.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(1000L + categoryId * 100 + i));
      m.put(DatabaseContract.Sms.KEY_PERSON, "0");
      m.put(DatabaseContract.Sms.KEY_READ, "1");
      m.put(DatabaseContract.Sms.KEY_SEEN, "1");
      m.put(DatabaseContract.Sms.KEY_SUBJECT, "");
      m.put(DatabaseContract.Sms.KEY_BODY, bodies[i]);
      m.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
      m.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
      m.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
      m.put(DatabaseContract.Sms.KEY_ADDRESS, "+1555000000" + categoryId);
      m.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
      m.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
      m.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
      rows.add(m);
    }
    assertEquals(rows.size(), SmsDao.storeTrainedInboxSms(ctx, rows));
    List<Map<String, String>> stored = SmsDao.getAll(ctx);
    for (Map<String, String> storedRow : stored) {
      if (String.valueOf(categoryId).equals(storedRow.get(DatabaseContract.Sms.KEY_CATEGORY_ID))
          && !"1.0".equals(storedRow.get(DatabaseContract.Sms.KEY_SIM_SCORE))) {
        storedRow.put(DatabaseContract.Sms.KEY_SIMILAR_TO,
            storedRow.get(DatabaseContract.Sms._ID));
        storedRow.put(DatabaseContract.Sms.KEY_SIM_SCORE, "1.0");
        SmsDao.updateSmsData(ctx, storedRow);
      }
    }
  }

  private void setAlgorithmPref(String value) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    if (value == null) {
      prefs.edit().remove("key_similarity_algorithm").apply();
    } else {
      prefs.edit().putString("key_similarity_algorithm", value).apply();
    }
  }

  private void setThresholdPref(Integer percent) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    if (percent == null) {
      prefs.edit().remove("key_similarity_score").apply();
    } else {
      prefs.edit().putInt("key_similarity_score", percent).apply();
    }
  }

  /** Runs probes through the production training pipeline; returns probe -> assigned category id. */
  private Map<String, String> trainProbes() {
    List<Map<String, String>> probes = new ArrayList<>();
    for (int i = 0; i < PROBES.length; i++) {
      Map<String, String> m = new LinkedHashMap<>();
      m.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(9000L + i));
      m.put(DatabaseContract.Sms.KEY_PERSON, "0");
      m.put(DatabaseContract.Sms.KEY_READ, "0");
      m.put(DatabaseContract.Sms.KEY_SEEN, "0");
      m.put(DatabaseContract.Sms.KEY_SUBJECT, "");
      m.put(DatabaseContract.Sms.KEY_BODY, PROBES[i]);
      m.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
      m.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
      m.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
      m.put(DatabaseContract.Sms.KEY_ADDRESS, "+1555999999");
      m.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
      m.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
      m.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "1");
      probes.add(m);
    }
    List<Map<String, String>> trained = TrainSms.getTrainedListOfSms(
        ctx, probes, SmsDao.getSelfTrained(ctx));

    Map<String, String> assigned = new LinkedHashMap<>();
    for (Map<String, String> trainedRow : trained) {
      assigned.put(
          trainedRow.get(DatabaseContract.Sms.KEY_BODY),
          trainedRow.get(DatabaseContract.Sms.KEY_CATEGORY_ID)
              + "/" + trainedRow.get(DatabaseContract.Sms.KEY_SIM_SCORE)
      );
    }
    return assigned;
  }

  @Test
  public void goldenMaster_currentPipelineAssignments() {
    setAlgorithmPref(null);
    setThresholdPref(30);
    Map<String, String> actual = trainProbes();
    StringBuilder table = new StringBuilder("=== Parity run ===\n");
    for (int i = 0; i < PROBES.length; i++) {
      table.append(String.format("%-70s -> %s%n", "\"" + PROBES[i] + "\"", actual.get(PROBES[i])));
    }
    System.out.print(table);

    Map<String, String> expected = goldenMaster();
    for (int i = 0; i < PROBES.length; i++) {
      assertEquals("probe: " + PROBES[i], expected.get(PROBES[i]), actual.get(PROBES[i]));
    }
  }

  @Test
  public void legacyStoredAlgorithmValuesKeepLegacyBehavior() {
    String[] legacyValues = {"levenshtein", "jaroWinkler"};
    for (String legacyValue : legacyValues) {
      setAlgorithmPref(legacyValue);
      setThresholdPref(30);
      Map<String, String> actual = trainProbes();
      // exact old-engine outcomes: legacy users' semantics fully preserved
      assertEquals(legacyValue, "2/0.7142857313156128", actual.get(PROBES[0]));
      assertEquals(legacyValue, "2/0.4677419066429138", actual.get(PROBES[1]));
      assertEquals(legacyValue, "3/0.6216216087341309", actual.get(PROBES[2]));
      assertEquals(legacyValue, "3/0.6338028311729431", actual.get(PROBES[3]));
      assertEquals(legacyValue, "3/0.35135138034820557", actual.get(PROBES[4]));
      assertEquals(legacyValue, "3/0.3243243098258972", actual.get(PROBES[5]));
    }
  }

  @Test
  public void staleStoredAlgorithmValueFallsBackToBalanced() {
    setAlgorithmPref("dice");
    setThresholdPref(30);
    Map<String, String> actual = trainProbes();
    for (int i = 0; i < PROBES.length; i++) {
      assertEquals("probe: " + PROBES[i], goldenMaster().get(PROBES[i]), actual.get(PROBES[i]));
    }
  }

  @Test
  public void freshInstallDefaultsToBalancedWithRebaselinedThreshold() {
    setAlgorithmPref(null);
    setThresholdPref(null);
    Map<String, String> actual = trainProbes();
    // balanced default threshold 25%: content probes assigned, unrelated Unknown
    assertEquals("2", categoryOf(actual.get(PROBES[0])));
    assertEquals("2", categoryOf(actual.get(PROBES[1])));
    assertEquals("3", categoryOf(actual.get(PROBES[2])));
    assertEquals("3", categoryOf(actual.get(PROBES[3])));
    assertEquals("1", categoryOf(actual.get(PROBES[4])));
    assertEquals("1", categoryOf(actual.get(PROBES[5])));
  }

  @Test
  public void legacyMappedUserWithoutStoredScoreKeepsLegacyDefaultThreshold() {
    setAlgorithmPref("levenshtein");
    setThresholdPref(null);
    Map<String, String> actual = trainProbes();
    // threshold defaults to 80 (not 25) in legacyLevenshtein mode:
    // best legacy score 0.714 < 0.8, so nothing is auto-assigned
    assertEquals("1", categoryOf(actual.get(PROBES[0])));
    assertEquals("1", categoryOf(actual.get(PROBES[4])));
  }

  private static String categoryOf(String assignment) {
    return assignment.substring(0, assignment.indexOf('/'));
  }

  @Test
  public void eachHybridModeClassifiesContentAndRejectsUnrelatedProbes() {
    for (String mode : new String[]{"wordsOnly", "charactersOnly", "balanced"}) {
      setAlgorithmPref(mode);
      setThresholdPref(25);
      Map<String, String> actual = trainProbes();
      assertEquals(mode, "2", categoryOf(actual.get(PROBES[0])));
      assertEquals(mode, "2", categoryOf(actual.get(PROBES[1])));
      assertEquals(mode, "3", categoryOf(actual.get(PROBES[2])));
      assertEquals(mode, "3", categoryOf(actual.get(PROBES[3])));
      assertEquals(mode, "1", categoryOf(actual.get(PROBES[4])));
      assertEquals(mode, "1", categoryOf(actual.get(PROBES[5])));
    }
  }

  @Test
  public void tiedScoresResolveToFirstExemplarInPipelineOrder() {
    String sharedBody = "identical twin message body for tie break";
    // pipeline order for exemplars is Sms.DEFAULT_SORT_ORDER = date DESC:
    // the newer bank twin must therefore come first and win the tie
    long firstId = seedSelfTrained(sharedBody, CATEGORY_BANK_ID, 20000L);
    long secondId = seedSelfTrained(sharedBody, CATEGORY_PROMO_ID, 10000L);
    assertTrue(secondId > firstId);

    setAlgorithmPref(null);
    setThresholdPref(25);
    Map<String, String> actual = trainSingleProbe(sharedBody);
    assertEquals("2/1.0", actual.get(sharedBody));
  }

  @Test
  public void retrainPropagatesCategoryToSimilarStoredSms() {
    String similarBody =
        "rs 950 debited from your account xx4432 balance is rs 8000 now";
    long candidateId = seedUnknownCandidate(similarBody);

    setAlgorithmPref(null);
    setThresholdPref(null);

    // the candidate mirrors BANK_EXEMPLARS[0]; retrain against that exemplar
    Map<String, String> exemplar = selfTrainedWithBody(BANK_EXEMPLARS[0]);
    exemplar.put(DatabaseContract.Sms.KEY_CATEGORY_ID,
        String.valueOf(CATEGORY_BANK_ID));
    exemplar.put(DatabaseContract.Sms.KEY_SIMILAR_TO,
        exemplar.get(DatabaseContract.Sms._ID));
    exemplar.put(DatabaseContract.Sms.KEY_SIM_SCORE, "1.0");

    List<Map<String, String>> retrained =
        TrainSms.retrainExistingSms(ctx, exemplar);

    boolean candidateUpgraded = false;
    for (Map<String, String> row : retrained) {
      if (String.valueOf(candidateId).equals(row.get(DatabaseContract.Sms._ID))) {
        candidateUpgraded = true;
        assertEquals(String.valueOf(CATEGORY_BANK_ID),
            row.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
        assertEquals(exemplar.get(DatabaseContract.Sms._ID),
            row.get(DatabaseContract.Sms.KEY_SIMILAR_TO));
        double score = Double.parseDouble(row.get(DatabaseContract.Sms.KEY_SIM_SCORE));
        assertTrue("expected upgraded score above default threshold, got " + score,
            score >= 0.25);
      }
    }
    assertTrue("similar stored sms should be retrained", candidateUpgraded);
  }

  private long seedSelfTrained(String body, long categoryId, long date) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(date));
    m.put(DatabaseContract.Sms.KEY_PERSON, "0");
    m.put(DatabaseContract.Sms.KEY_READ, "1");
    m.put(DatabaseContract.Sms.KEY_SEEN, "1");
    m.put(DatabaseContract.Sms.KEY_SUBJECT, "");
    m.put(DatabaseContract.Sms.KEY_BODY, body);
    m.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
    m.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
    m.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
    m.put(DatabaseContract.Sms.KEY_ADDRESS, "+1555888111");
    m.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
    m.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
    m.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
    assertEquals(1, SmsDao.storeTrainedInboxSms(ctx, java.util.Collections.singletonList(m)));
    List<Map<String, String>> all = SmsDao.getAll(ctx);
    for (Map<String, String> row : all) {
      if (body.equals(row.get(DatabaseContract.Sms.KEY_BODY))
          && String.valueOf(categoryId)
              .equals(row.get(DatabaseContract.Sms.KEY_CATEGORY_ID))) {
        row.put(DatabaseContract.Sms.KEY_SIMILAR_TO, row.get(DatabaseContract.Sms._ID));
        row.put(DatabaseContract.Sms.KEY_SIM_SCORE, "1.0");
        SmsDao.updateSmsData(ctx, row);
        return Long.parseLong(row.get(DatabaseContract.Sms._ID));
      }
    }
    throw new IllegalStateException("seeded row not found");
  }

  private long seedUnknownCandidate(String body) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put(DatabaseContract.Sms.KEY_DATE, "30000");
    m.put(DatabaseContract.Sms.KEY_PERSON, "0");
    m.put(DatabaseContract.Sms.KEY_READ, "0");
    m.put(DatabaseContract.Sms.KEY_SEEN, "0");
    m.put(DatabaseContract.Sms.KEY_SUBJECT, "");
    m.put(DatabaseContract.Sms.KEY_BODY, body);
    m.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
    m.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
    m.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
    m.put(DatabaseContract.Sms.KEY_ADDRESS, "+1555777222");
    m.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
    m.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
    m.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "1");
    assertEquals(1, SmsDao.storeTrainedInboxSms(ctx, java.util.Collections.singletonList(m)));
    List<Map<String, String>> all = SmsDao.getAll(ctx);
    for (Map<String, String> row : all) {
      if (body.equals(row.get(DatabaseContract.Sms.KEY_BODY))) {
        return Long.parseLong(row.get(DatabaseContract.Sms._ID));
      }
    }
    throw new IllegalStateException("seeded row not found");
  }

  private Map<String, String> selfTrainedWithBody(String body) {
    List<Map<String, String>> selfTrained = SmsDao.getSelfTrained(ctx);
    for (Map<String, String> row : selfTrained) {
      if (body.equals(row.get(DatabaseContract.Sms.KEY_BODY))) {
        return row;
      }
    }
    throw new IllegalStateException("no self-trained exemplar with body: " + body);
  }

  private Map<String, String> trainSingleProbe(String body) {
    List<Map<String, String>> probes = new ArrayList<>();
    Map<String, String> m = new LinkedHashMap<>();
    m.put(DatabaseContract.Sms.KEY_DATE, "9500");
    m.put(DatabaseContract.Sms.KEY_PERSON, "0");
    m.put(DatabaseContract.Sms.KEY_READ, "0");
    m.put(DatabaseContract.Sms.KEY_SEEN, "0");
    m.put(DatabaseContract.Sms.KEY_SUBJECT, "");
    m.put(DatabaseContract.Sms.KEY_BODY, body);
    m.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
    m.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
    m.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
    m.put(DatabaseContract.Sms.KEY_ADDRESS, "+1555999999");
    m.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
    m.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
    m.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "1");
    probes.add(m);
    List<Map<String, String>> trained = TrainSms.getTrainedListOfSms(
        ctx, probes, SmsDao.getSelfTrained(ctx));
    Map<String, String> assigned = new LinkedHashMap<>();
    for (Map<String, String> trainedRow : trained) {
      assigned.put(
          trainedRow.get(DatabaseContract.Sms.KEY_BODY),
          trainedRow.get(DatabaseContract.Sms.KEY_CATEGORY_ID)
              + "/" + trainedRow.get(DatabaseContract.Sms.KEY_SIM_SCORE)
      );
    }
    return assigned;
  }

  /**
   * Golden master of the NEW-engine pipeline behavior for PROBES under
   * threshold 30% balanced mode. Updated during the dedicated parity-evaluation
   * step; old-engine values and per-probe justification live in task-20-report.
   */
  private Map<String, String> goldenMaster() {
    Map<String, String> golden = new LinkedHashMap<>();
    golden.put(PROBES[0], "2/0.658130497447025");
    golden.put(PROBES[1], "2/0.7656604729084928");
    golden.put(PROBES[2], "3/0.5084075442512366");
    golden.put(PROBES[3], "3/0.48876857542582663");
    golden.put(PROBES[4], "1/0.0");
    golden.put(PROBES[5], "1/0.0");
    return golden;
  }
}
