package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import in.rahulja.groupingmessages.db.AppDatabase;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Sms;
import in.rahulja.groupingmessages.vm.AppExecutors;
import in.rahulja.groupingmessages.vm.SmsListViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class SmsListViewModelTest {

  private Context ctx;
  private SmsListViewModel viewModel;

  @Before
  public void setUp() throws Exception {
    Application application = RuntimeEnvironment.getApplication();
    // Robolectric keeps static state across tests of a class while giving each
    // test a fresh data dir; drop the helper singleton so the next open binds
    // to this test's own database file instead of a previous test's
    java.lang.reflect.Field f =
        DatabaseHelper.class.getDeclaredField("sInstance");
    f.setAccessible(true);
    DatabaseHelper inst = (DatabaseHelper) f.get(null);
    if (inst != null) {
      inst.close();
      f.set(null, null);
    }
    ctx = application;
    AppDatabase.get(ctx);
    viewModel = new SmsListViewModel(application);
  }

  private void runPendingWorkToCompletion() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AppExecutors.disk(latch::countDown);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    shadowOf(Looper.getMainLooper()).idle();
  }

  private static Map<String, String> smsMap(long date, String body, long categoryId,
      long similarTo) {
    Map<String, String> sms = new HashMap<>();
    sms.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(date));
    sms.put(DatabaseContract.Sms.KEY_PERSON, "0");
    sms.put(DatabaseContract.Sms.KEY_READ, "0");
    sms.put(DatabaseContract.Sms.KEY_SEEN, "0");
    sms.put(DatabaseContract.Sms.KEY_SUBJECT, "subject");
    sms.put(DatabaseContract.Sms.KEY_BODY, body);
    sms.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
    sms.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
    sms.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
    sms.put(DatabaseContract.Sms.KEY_ADDRESS, "+15551234567");
    sms.put(DatabaseContract.Sms.KEY_SIMILAR_TO, String.valueOf(similarTo));
    sms.put(DatabaseContract.Sms.KEY_SIM_SCORE, similarTo == 0 ? "0.0" : "1.0");
    sms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
    return sms;
  }

  private static Sms smsFromMap(Map<String, String> data) {
    return new Sms(
        Long.parseLong(data.get(DatabaseContract.Sms._ID)),
        Long.parseLong(data.get(DatabaseContract.Sms.KEY_CATEGORY_ID)),
        Long.parseLong(data.get(DatabaseContract.Sms.KEY_DATE)),
        Integer.parseInt(data.get(DatabaseContract.Sms.KEY_VISIBILITY)),
        Integer.parseInt(data.get(DatabaseContract.Sms.KEY_READ)),
        data.get(DatabaseContract.Sms.KEY_ADDRESS),
        data.get(DatabaseContract.Sms.KEY_BODY),
        Long.parseLong(data.get(DatabaseContract.Sms.KEY_SIMILAR_TO)));
  }

  /** Inserts an untrained sms and returns its stored row (with real _ID). */
  private Map<String, String> insertSms(long date, String body, long categoryId) {
    SmsDao.storeTrainedInboxSms(ctx, Collections.singletonList(
        smsMap(date, body, categoryId, 0)));
    for (Map<String, String> row : SmsDao.getAll(ctx)) {
      if (body.equals(row.get(DatabaseContract.Sms.KEY_BODY))) {
        return row;
      }
    }
    throw new IllegalStateException("sms not stored: " + body);
  }

  /** Marks a stored sms as self-trained, like legacy change-category flow does. */
  private void trainSelf(Map<String, String> row) {
    row.put(DatabaseContract.Sms.KEY_SIMILAR_TO, row.get(DatabaseContract.Sms._ID));
    row.put(DatabaseContract.Sms.KEY_SIM_SCORE, "1.0");
    SmsDao.updateSmsData(ctx, row);
  }

  @Test
  public void swipeDeleteOnTrainedSmsHidesRowInsteadOfDeleting()
      throws InterruptedException {
    long categoryId = CategoryDao.insert(ctx, "News");
    Map<String, String> row = insertSms(1000L, "trained one", categoryId);
    trainSelf(row);

    Sms sms = smsFromMap(SmsDao.getAll(ctx).get(0));
    assertEquals(sms.getId(), sms.getSimilarTo());

    viewModel.swipeDelete(sms, categoryId);
    runPendingWorkToCompletion();

    List<Map<String, String>> allRows = SmsDao.getAll(ctx);
    assertEquals(1, allRows.size());
    assertEquals("0", allRows.get(0).get(DatabaseContract.Sms.KEY_VISIBILITY));

    List<Sms> emitted = viewModel.getSms(categoryId).getValue();
    assertNotNull(emitted);
    assertEquals(0, emitted.size());
  }

  @Test
  public void swipeDeleteOnUntrainedSmsDeletesRow() throws InterruptedException {
    long categoryId = CategoryDao.insert(ctx, "News");
    insertSms(1000L, "untrained one", categoryId);
    assertEquals(1, SmsDao.getAll(ctx).size());

    Sms sms = smsFromMap(SmsDao.getAll(ctx).get(0));
    assertEquals(0L, sms.getSimilarTo());

    viewModel.swipeDelete(sms, categoryId);
    runPendingWorkToCompletion();

    assertEquals(0, SmsDao.getAll(ctx).size());
    List<Sms> emitted = viewModel.getSms(categoryId).getValue();
    assertNotNull(emitted);
    assertEquals(0, emitted.size());
  }

  @Test
  public void markReadUpdatesRows() throws InterruptedException {
    long categoryId = CategoryDao.insert(ctx, "News");
    Map<String, String> first = insertSms(1000L, "one", categoryId);
    Map<String, String> second = insertSms(2000L, "two", categoryId);

    viewModel.markRead(Collections.singletonList(
        Long.parseLong(first.get(DatabaseContract.Sms._ID))));
    runPendingWorkToCompletion();

    int unreadAfterFirst = 0;
    for (Map<String, String> row : SmsDao.getAll(ctx)) {
      if ("0".equals(row.get(DatabaseContract.Sms.KEY_READ))) {
        unreadAfterFirst++;
      }
    }
    assertEquals(1, unreadAfterFirst);

    viewModel.markRead(Collections.singletonList(
        Long.parseLong(second.get(DatabaseContract.Sms._ID))));
    runPendingWorkToCompletion();

    for (Map<String, String> row : SmsDao.getAll(ctx)) {
      assertEquals("1", row.get(DatabaseContract.Sms.KEY_READ));
    }
  }

  @Test
  public void moveToCategoryMovesRowsToTargetCategory() throws InterruptedException {
    long sourceId = CategoryDao.insert(ctx, "Source");
    long targetId = CategoryDao.insert(ctx, "Target");
    insertSms(1000L, "movable", sourceId);
    insertSms(2000L, "movable too", sourceId);

    List<Long> ids = new ArrayList<>();
    for (Map<String, String> row : SmsDao.getAll(ctx)) {
      ids.add(Long.parseLong(row.get(DatabaseContract.Sms._ID)));
    }

    viewModel.moveToCategory(ids, targetId);
    runPendingWorkToCompletion();

    for (Map<String, String> row : SmsDao.getAll(ctx)) {
      assertEquals(String.valueOf(targetId),
          row.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
    }
  }

  @Test
  public void refreshEmitsSameOrderAsLegacyCategoryQuery()
      throws InterruptedException {
    long categoryId = CategoryDao.insert(ctx, "News");
    insertSms(3000L, "newest", categoryId);
    insertSms(1000L, "oldest", categoryId);
    insertSms(2000L, "middle", categoryId);

    viewModel.refresh(categoryId);
    runPendingWorkToCompletion();

    List<Sms> emitted = viewModel.getSms(categoryId).getValue();
    assertNotNull(emitted);

    List<Long> emittedIds = new ArrayList<>();
    for (Sms sms : emitted) {
      emittedIds.add(sms.getId());
    }

    List<Long> legacyIds = new ArrayList<>();
    for (Map<String, String> legacyRow : SmsDao.getVisibleMapsByCategory(ctx, categoryId)) {
      legacyIds.add(Long.parseLong(legacyRow.get(DatabaseContract.Sms._ID)));
    }

    assertEquals(3, legacyIds.size());
    assertEquals(legacyIds, emittedIds);
  }
}
