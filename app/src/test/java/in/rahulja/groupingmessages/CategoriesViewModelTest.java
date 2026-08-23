package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Looper;
import in.rahulja.groupingmessages.db.AppDatabase;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Category;
import in.rahulja.groupingmessages.vm.AppExecutors;
import in.rahulja.groupingmessages.vm.CategoriesViewModel;
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
public class CategoriesViewModelTest {

  private Context ctx;
  private CategoriesViewModel viewModel;

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
    viewModel = new CategoriesViewModel(application);
  }

  private void runPendingWorkToCompletion() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AppExecutors.disk(latch::countDown);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    shadowOf(Looper.getMainLooper()).idle();
  }

  private static Map<String, String> smsMap(long date, String body, long categoryId) {
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
    sms.put(DatabaseContract.Sms.KEY_SIMILAR_TO, "0");
    sms.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.0");
    sms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
    return sms;
  }

  @Test
  public void refreshEmitsVisibleCategoriesInLegacyOrderWithZeroedCounts()
      throws InterruptedException {
    long bananaId = CategoryDao.insert(ctx, "banana");
    long appleId = CategoryDao.insert(ctx, "apple");

    viewModel.refresh();
    runPendingWorkToCompletion();

    List<Category> emitted = viewModel.getCategories().getValue();
    assertNotNull(emitted);
    assertEquals(3, emitted.size());
    assertEquals("Unknown", emitted.get(0).getName());
    assertEquals(appleId, emitted.get(1).getId());
    assertEquals(bananaId, emitted.get(2).getId());

    assertEquals("0", viewModel.getUnreadCounts().getValue().get(appleId));
    assertEquals("0", viewModel.getReadCounts().getValue().get(bananaId));
  }

  @Test
  public void refreshJoinsSmsCountsPerCategory() throws InterruptedException {
    long newsId = CategoryDao.insert(ctx, "News");
    SmsDao.storeTrainedInboxSms(ctx, Collections.singletonList(
        smsMap(1000L, "unread one", newsId)));
    SmsDao.storeTrainedInboxSms(ctx, Collections.singletonList(
        smsMap(2000L, "read one", newsId)));
    SmsDao.setAllCategorySmsAsRead(ctx, String.valueOf(newsId));

    viewModel.refresh();
    runPendingWorkToCompletion();

    List<Category> emitted = viewModel.getCategories().getValue();
    assertNotNull(emitted);
    assertEquals(2, emitted.size());
    assertEquals("2", viewModel.getReadCounts().getValue().get(newsId));
    assertEquals("0", viewModel.getUnreadCounts().getValue().get(newsId));
  }

  @Test
  public void refreshExcludesHiddenCategoriesLikeLegacyVisibleList()
      throws InterruptedException {
    long tempId = CategoryDao.insert(ctx, "Temporary");
    CategoryDao.deleteCategory(ctx, tempId);

    viewModel.refresh();
    runPendingWorkToCompletion();

    List<Category> emitted = viewModel.getCategories().getValue();
    assertNotNull(emitted);
    assertEquals(1, emitted.size());
    assertEquals("Unknown", emitted.get(0).getName());
    assertFalse(viewModel.getUnreadCounts().getValue().containsKey(tempId));
  }

  @Test
  public void addCategoryPersistsNameAndColorAndRefreshesList() throws InterruptedException {
    viewModel.addCategory("News", Color.RED);
    runPendingWorkToCompletion();
    runPendingWorkToCompletion();

    List<Category> emitted = viewModel.getCategories().getValue();
    assertNotNull(emitted);
    assertEquals(2, emitted.size());
    assertEquals("News", emitted.get(0).getName());
    assertEquals(Color.RED, emitted.get(0).getColor());
    assertEquals("Unknown", emitted.get(1).getName());

    Category stored = CategoryDao.getById(ctx, emitted.get(0).getId());
    assertNotNull(stored);
    assertEquals("News", stored.getName());
    assertEquals(Color.RED, stored.getColor());

    assertEquals("News", viewModel.getAddedCategoryName().getValue());
  }

  @Test
  public void deleteCategoryRemovesFromListAndCascadesSmsToUnknown()
      throws InterruptedException {
    long newsId = CategoryDao.insert(ctx, "News");
    SmsDao.storeTrainedInboxSms(ctx,
        Collections.singletonList(smsMap(123456L, "hello", newsId)));
    assertEquals(1, CategoryDao.getCountPerCategory(ctx, newsId));

    viewModel.deleteCategory(newsId);
    runPendingWorkToCompletion();
    runPendingWorkToCompletion();

    List<Category> emitted = viewModel.getCategories().getValue();
    assertNotNull(emitted);
    assertEquals(1, emitted.size());
    assertEquals("Unknown", emitted.get(0).getName());

    for (Map<String, String> sms : SmsDao.getAll(ctx)) {
      assertEquals("1", sms.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
    }
    assertEquals(0, CategoryDao.getCountPerCategory(ctx, newsId));
  }

  @Test
  public void deleteUnknownCategoryIsIgnored() throws InterruptedException {
    viewModel.deleteCategory(1L);
    viewModel.refresh();
    runPendingWorkToCompletion();
    runPendingWorkToCompletion();

    List<Category> emitted = viewModel.getCategories().getValue();
    assertNotNull(emitted);
    assertEquals(1, emitted.size());
    assertEquals("Unknown", emitted.get(0).getName());
    assertNotNull(CategoryDao.getById(ctx, 1L));
  }
}
