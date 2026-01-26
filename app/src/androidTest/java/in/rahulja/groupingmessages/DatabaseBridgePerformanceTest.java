package in.rahulja.groupingmessages;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class DatabaseBridgePerformanceTest {

    private Context context;
    private static final String TAG = "PerfTest";

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testStoreReTrainedSmsPerformance() {
        // Prepare data
        int smsCount = 100;
        List<Map<String, String>> initialSmsList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < smsCount; i++) {
            Map<String, String> sms = new HashMap<>();
            sms.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(currentTime + i));
            sms.put(DatabaseContract.Sms.KEY_PERSON, "0");
            sms.put(DatabaseContract.Sms.KEY_READ, "1");
            sms.put(DatabaseContract.Sms.KEY_SEEN, "1");
            sms.put(DatabaseContract.Sms.KEY_SUBJECT, "Test Subject " + i);
            sms.put(DatabaseContract.Sms.KEY_ADDRESS, "1234567890");
            sms.put(DatabaseContract.Sms.KEY_BODY, "PerfTest Body " + i);
            sms.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "PerfTest Body " + i);
            sms.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
            sms.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "1");
            sms.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
            sms.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
            sms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "1");
            initialSmsList.add(sms);
        }

        // Insert initial data
        DatabaseBridge.storeTrainedInboxSms(context, initialSmsList);

        // Fetch inserted SMS to get their IDs and verify they exist
        List<Map<String, String>> storedSms = DatabaseBridge.getAllSms(context);
        List<Map<String, String>> retrainedList = new ArrayList<>();

        for (Map<String, String> sms : storedSms) {
            String body = sms.get(DatabaseContract.Sms.KEY_BODY);
            if (body != null && body.startsWith("PerfTest Body")) {
                // Modify to simulate retraining (e.g. changing category)
                sms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "2");
                retrainedList.add(sms);
            }
        }

        // We might have run this test before, so retrainedList might be larger than smsCount if we didn't cleanup.
        // But for performance measuring, larger is fine.
        assertTrue("Should have at least " + smsCount + " records", retrainedList.size() >= smsCount);

        Log.i(TAG, "Starting performance test with " + retrainedList.size() + " records");

        // Measure storeReTrainedSms
        long startTime = System.currentTimeMillis();
        long updatedCount = DatabaseBridge.storeReTrainedSms(context, retrainedList);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        Log.i(TAG, "PERFORMANCE_RESULT: storeReTrainedSms took " + duration + "ms for " + retrainedList.size() + " records");

        assertEquals(retrainedList.size(), updatedCount);
    }
}
