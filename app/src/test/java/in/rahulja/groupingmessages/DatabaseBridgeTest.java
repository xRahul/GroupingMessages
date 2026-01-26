package in.rahulja.groupingmessages;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DatabaseBridgeTest {

  @Test
  public void measureInsertIntoSmsFailurePerformance() throws Exception {
    // Create a real in-memory database
    SQLiteDatabase db = SQLiteDatabase.create(null);
    // Close it to force IllegalStateException on insert
    db.close();

    Map<String, String> smsMap = new HashMap<>();
    smsMap.put(DatabaseContract.Sms.KEY_DATE, "123456789");
    smsMap.put(DatabaseContract.Sms.KEY_PERSON, "0");
    smsMap.put(DatabaseContract.Sms.KEY_READ, "0");
    smsMap.put(DatabaseContract.Sms.KEY_SEEN, "0");
    smsMap.put(DatabaseContract.Sms.KEY_SUBJECT, "Test Subject");
    smsMap.put(DatabaseContract.Sms.KEY_BODY, "Test Body");
    smsMap.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "Test Cleaned");
    smsMap.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
    smsMap.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "0");
    smsMap.put(DatabaseContract.Sms.KEY_ADDRESS, "123456");
    smsMap.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
    smsMap.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
    smsMap.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "1");

    Method method = DatabaseBridge.class.getDeclaredMethod("insertIntoSms", SQLiteDatabase.class, Map.class);
    method.setAccessible(true);

    long startTime = System.currentTimeMillis();
    long result = (long) method.invoke(null, db, smsMap);
    long endTime = System.currentTimeMillis();

    long duration = endTime - startTime;
    System.out.println("Execution time optimized: " + duration + "ms");

    // Expecting minimal time (< 80ms) now that retry is removed.
    // Previous baseline was >= 80ms (likely 100ms+).
    // Allowing some overhead for Robolectric/environment, but it must be faster than the retry loop.
    assertTrue("Execution time should be optimized (< 80ms), actual: " + duration, duration < 80);
    assertEquals("Result should be -1 on error", -1, result);
  }
}
