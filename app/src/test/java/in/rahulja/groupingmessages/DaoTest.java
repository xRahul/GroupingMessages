package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import in.rahulja.groupingmessages.db.AppDatabase;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.ConfigDao;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Category;
import in.rahulja.groupingmessages.model.Sms;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class DaoTest {

  private Context ctx;

  @Before
  public void setUp() throws Exception {
    ctx = RuntimeEnvironment.getApplication();
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
    AppDatabase.get(ctx);
  }

  private static Map<String, String> smsMap(long date, String body, long categoryId,
      long similarTo, double simScore) {
    Map<String, String> m = new HashMap<>();
    m.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(date));
    m.put(DatabaseContract.Sms.KEY_PERSON, "0");
    m.put(DatabaseContract.Sms.KEY_READ, "0");
    m.put(DatabaseContract.Sms.KEY_SEEN, "0");
    m.put(DatabaseContract.Sms.KEY_SUBJECT, "subject");
    m.put(DatabaseContract.Sms.KEY_BODY, body);
    m.put(DatabaseContract.Sms.KEY_CLEANED_SMS, "");
    m.put(DatabaseContract.Sms.KEY_VISIBILITY, "1");
    m.put(DatabaseContract.Sms.KEY_SENDER_TYPE, "-1");
    m.put(DatabaseContract.Sms.KEY_ADDRESS, "+15551234567");
    m.put(DatabaseContract.Sms.KEY_SIMILAR_TO, String.valueOf(similarTo));
    m.put(DatabaseContract.Sms.KEY_SIM_SCORE, String.valueOf(simScore));
    m.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
    return m;
  }

  private List<Map<String, String>> storedSmsMaps() {
    return SmsDao.getAll(ctx);
  }

  private static List<Long> idsOf(List<Map<String, String>> smsList) {
    List<Long> ids = new ArrayList<>();
    for (Map<String, String> sms : smsList) {
      ids.add(Long.parseLong(sms.get(DatabaseContract.Sms._ID)));
    }
    return ids;
  }

  @Test
  public void configRoundTripAndReplace() {
    assertEquals("0", ConfigDao.getValue(ctx, "lastSmsTime"));
    assertNull(ConfigDao.getValue(ctx, "missingKey"));

    ConfigDao.put(ctx, "lastSmsTime", "123456");
    assertEquals("123456", ConfigDao.getValue(ctx, "lastSmsTime"));

    ConfigDao.put(ctx, "customKey", "customValue");
    assertEquals("customValue", ConfigDao.getValue(ctx, "customKey"));
  }

  @Test
  public void ensureDefaultsExistIsIdempotent() {
    Category unknown = CategoryDao.getById(ctx, 1L);
    assertNotNull(unknown);
    assertEquals("Unknown", unknown.getName());

    CategoryDao.ensureDefaultsExist(ctx);
    CategoryDao.ensureDefaultsExist(ctx);

    assertEquals(1, CategoryDao.getAllOrderedByPipelineOrder(ctx).size());
    assertEquals(Color.WHITE, CategoryDao.getById(ctx, 1L).getColor());
  }

  @Test
  public void categoryCrudTypedMethods() {
    long newsId = CategoryDao.insert(ctx, "News");
    assertTrue(newsId > 0);

    Category fetched = CategoryDao.getById(ctx, newsId);
    assertNotNull(fetched);
    assertEquals("News", fetched.getName());
    assertEquals(Color.WHITE, fetched.getColor());

    assertEquals(1, CategoryDao.rename(ctx, newsId, "NewsFeed"));
    assertEquals("NewsFeed", CategoryDao.getById(ctx, newsId).getName());

    assertEquals(1, CategoryDao.deleteById(ctx, newsId));
    assertNull(CategoryDao.getById(ctx, newsId));
  }

  @Test
  public void categoriesOrderMatchesLegacyNameAsc() {
    CategoryDao.insert(ctx, "Zebra");
    CategoryDao.insert(ctx, "Alpha");

    List<Category> ordered = CategoryDao.getAllOrderedByPipelineOrder(ctx);
    List<String> names = new ArrayList<>();
    for (Category c : ordered) {
      names.add(c.getName());
    }
    assertEquals(Arrays.asList("Alpha", "Unknown", "Zebra"), names);
  }

  @Test
  public void categoryMapPathAddUpdateColorRoundTrip() {
    Map<String, String> category = new HashMap<>();
    int purple = Color.rgb(128, 0, 128);
    category.put(DatabaseContract.Category.KEY_NAME, "Promos");
    category.put(DatabaseContract.Category.KEY_VISIBILITY, "1");
    category.put(DatabaseContract.Category.KEY_COLOR, String.valueOf(purple));

    assertTrue(CategoryDao.addCategory(ctx, category));

    Map<String, String> promos = null;
    for (Map<String, String> c : CategoryDao.getAllVisibleCategories(ctx)) {
      if ("Promos".equals(c.get(DatabaseContract.Category.KEY_NAME))) {
        promos = c;
      }
    }
    assertNotNull(promos);
    assertEquals(purple,
        Integer.parseInt(promos.get(DatabaseContract.Category.KEY_COLOR)));

    promos.put(DatabaseContract.Category.KEY_NAME, "Offers");
    assertTrue(CategoryDao.updateCategory(ctx, promos));
    assertEquals("Offers", CategoryDao.getById(
        ctx, Long.parseLong(promos.get(DatabaseContract.Category._ID))).getName());
  }

  @Test
  public void storeTrainedInboxSmsTransactionalInsertAndUpdateWatermark() {
    List<Map<String, String>> batch = Arrays.asList(
        smsMap(1000L, "first", 1L, 0L, 0.5d),
        smsMap(2000L, "second", 1L, 0L, 0.75d),
        smsMap(3000L, "third", 1L, 0L, 0.85d)
    );

    assertEquals(3, SmsDao.storeTrainedInboxSms(ctx, batch));
    assertEquals("3000", ConfigDao.getValue(ctx, "lastSmsTime"));
    assertEquals(3, SmsDao.countAll(ctx));
  }

  @Test
  public void smsOrderingMatchesLegacyDateDesc() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "old", 1L, 0L, 0.0d),
        smsMap(3000L, "newest", 1L, 0L, 0.0d),
        smsMap(2000L, "middle", 1L, 0L, 0.0d)
    ));

    List<Map<String, String>> all = storedSmsMaps();
    assertEquals(Arrays.asList("newest", "middle", "old"), bodiesOf(all));
  }

  private static List<String> bodiesOf(List<Map<String, String>> smsList) {
    List<String> bodies = new ArrayList<>();
    for (Map<String, String> sms : smsList) {
      bodies.add(sms.get(DatabaseContract.Sms.KEY_BODY));
    }
    return bodies;
  }

  @Test
  public void floatPrecisionRoundTripWithinFloatTolerance() {
    SmsDao.storeTrainedInboxSms(ctx,
        Collections.singletonList(smsMap(1000L, "scored", 1L, 0L, 0.85d)));

    Map<String, String> stored = storedSmsMaps().get(0);
    double roundTripped = Double.parseDouble(stored.get(DatabaseContract.Sms.KEY_SIM_SCORE));
    assertEquals(0.85f, (float) roundTripped, 1e-6f);
  }

  @Test
  public void getSelfTrainedReturnsOnlySelfSimilarFullScoreRows() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "untrained", 1L, 0L, 0.4d),
        smsMap(2000L, "trained", 2L, 2L, 1.0d)
    ));

    List<Map<String, String>> selfTrained = SmsDao.getSelfTrained(ctx);
    assertEquals(1, selfTrained.size());
    assertEquals("trained",
        selfTrained.get(0).get(DatabaseContract.Sms.KEY_BODY));
  }

  @Test
  public void visibleByCategoryFiltersAndOrdersBothShapes() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "catOneVisible", 1L, 0L, 0.0d),
        smsMap(2000L, "catTwoHidden", 2L, 0L, 0.0d)
    ));
    SmsDao.setVisibility(ctx, idsOf(SmsDao.getAll(ctx)), 0);

    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1500L, "catTwoShown", 2L, 0L, 0.0d)
    ));

    List<Sms> typed = SmsDao.getVisibleByCategory(ctx, 2L);
    assertEquals(1, typed.size());
    assertEquals("catTwoShown", typed.get(0).getBody());

    List<Map<String, String>> maps =
        SmsDao.getVisibleMapsByCategory(ctx, 2L);
    assertEquals(Arrays.asList("catTwoShown"), bodiesOf(maps));
  }

  @Test
  public void batchMutatorsUpdateAndDeleteInBulk() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "one", 1L, 0L, 0.0d),
        smsMap(2000L, "two", 1L, 0L, 0.0d),
        smsMap(3000L, "three", 1L, 0L, 0.0d)
    ));

    List<Long> allIds = idsOf(SmsDao.getAll(ctx));
    List<Long> firstTwo = allIds.subList(0, 2);

    assertEquals(2, SmsDao.markRead(ctx, firstTwo));
    assertEquals(1, SmsDao.countUnreadInCategory(ctx, 1L));

    assertEquals(2, SmsDao.changeCategory(ctx, firstTwo, 1L));
    assertEquals(3, CategoryDao.getCountPerCategory(ctx, 1L));

    assertEquals(1, SmsDao.deleteByIds(ctx,
        Collections.singletonList(allIds.get(2))));
    assertEquals(2, SmsDao.countAll(ctx));

    assertEquals(0, SmsDao.markRead(ctx, Collections.<Long>emptyList()));
    assertEquals(0, SmsDao.deleteByIds(ctx, null));
  }

  @Test
  public void storeReTrainedSmsUpdatesAllRowsInTransaction() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "alpha", 1L, 0L, 0.0d),
        smsMap(2000L, "beta", 1L, 0L, 0.0d)
    ));

    List<Map<String, String>> retrained = new ArrayList<>();
    for (Map<String, String> sms : SmsDao.getAll(ctx)) {
      sms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, "1");
      sms.put(DatabaseContract.Sms.KEY_SIMILAR_TO,
          sms.get(DatabaseContract.Sms._ID));
      sms.put(DatabaseContract.Sms.KEY_SIM_SCORE, "0.95");
      retrained.add(sms);
    }

    assertEquals(retrained.size(), SmsDao.storeReTrainedSms(ctx, retrained));

    for (Map<String, String> sms : SmsDao.getAll(ctx)) {
      assertEquals(0.95f, Float.parseFloat(
          sms.get(DatabaseContract.Sms.KEY_SIM_SCORE)), 1e-6f);
    }
  }

  @Test
  public void deleteSmsByMapHidesSelfTrainedAndDeletesUntrained() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "plain", 1L, 0L, 0.0d),
        smsMap(2000L, "selftrained", 1L, 0L, 0.0d)
    ));

    List<Map<String, String>> all = SmsDao.getAll(ctx);
    Map<String, String> plain = null;
    Map<String, String> selfTrained = null;
    for (Map<String, String> sms : all) {
      if ("plain".equals(sms.get(DatabaseContract.Sms.KEY_BODY))) {
        plain = sms;
      } else {
        selfTrained = sms;
      }
    }

    selfTrained.put(DatabaseContract.Sms.KEY_SIMILAR_TO,
        selfTrained.get(DatabaseContract.Sms._ID));
    SmsDao.updateSmsData(ctx, selfTrained);

    SmsDao.deleteSmsByMap(ctx, selfTrained);
    // self-trained row is hidden, not removed; only "plain" stays visible
    List<Map<String, String>> visibleAfterHide =
        SmsDao.getVisibleMapsByCategory(ctx, 1L);
    assertEquals(1, visibleAfterHide.size());
    assertEquals("plain",
        visibleAfterHide.get(0).get(DatabaseContract.Sms.KEY_BODY));
    assertEquals(2, SmsDao.countAll(ctx));

    SmsDao.deleteSmsByMap(ctx, plain);
    assertEquals(1, SmsDao.countAll(ctx));
  }

  @Test
  public void deleteAllSmsOfCategoryByIdDeletesUntrainedHidesTrained() {
    long targetCat = CategoryDao.insert(ctx, "Target");
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "untrainedA", targetCat, 0L, 0.0d),
        smsMap(2000L, "untrainedB", targetCat, 0L, 0.0d)
    ));

    // make newest row trained (self-similar)
    List<Map<String, String>> all = SmsDao.getAll(ctx);
    all.get(0).put(DatabaseContract.Sms.KEY_SIMILAR_TO,
        all.get(0).get(DatabaseContract.Sms._ID));
    SmsDao.updateSmsData(ctx, all.get(0));

    SmsDao.deleteAllSmsOfCategoryById(ctx, targetCat);

    assertEquals(1, SmsDao.countAll(ctx));
    List<Map<String, String>> remaining = SmsDao.getAll(ctx);
    assertEquals("0", remaining.get(0).get(DatabaseContract.Sms.KEY_VISIBILITY));
  }

  @Test
  public void deleteCategoriesResetsModelAndKeepsUnknownOnly() {
    long tempCat = CategoryDao.insert(ctx, "Temporary");
    SmsDao.storeTrainedInboxSms(ctx,
        Collections.singletonList(smsMap(1000L, "somewhere", tempCat, 0L, 0.9d)));

    CategoryDao.deleteCategories(ctx);

    assertEquals(1, CategoryDao.getAllOrderedByPipelineOrder(ctx).size());
    for (Map<String, String> sms : SmsDao.getAll(ctx)) {
      assertEquals("1", sms.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
      assertEquals("0", sms.get(DatabaseContract.Sms.KEY_SIMILAR_TO));
      assertEquals(0.0f, Float.parseFloat(
          sms.get(DatabaseContract.Sms.KEY_SIM_SCORE)), 0.0f);
    }
  }

  @Test
  public void setSmsAsReadAndSetAllCategorySmsAsRead() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "a", 1L, 0L, 0.0d),
        smsMap(2000L, "b", 1L, 0L, 0.0d)
    ));

    SmsDao.setSmsAsRead(ctx, storedSmsMaps().get(0).get(DatabaseContract.Sms._ID));
    assertEquals(1, SmsDao.countUnreadInCategory(ctx, 1L));

    SmsDao.setAllCategorySmsAsRead(ctx, "1");
    assertEquals(0, SmsDao.countUnreadInCategory(ctx, 1L));
  }

  @Test
  public void repeatedQueriesDoNotLeakCursors() {
    SmsDao.storeTrainedInboxSms(ctx, Arrays.asList(
        smsMap(1000L, "x", 1L, 0L, 0.0d),
        smsMap(2000L, "y", 1L, 0L, 0.0d)
    ));
    for (int i = 0; i < 50; i++) {
      SmsDao.getAll(ctx);
      CategoryDao.getAllVisibleCategories(ctx);
      ConfigDao.getValue(ctx, "lastSmsTime");
      SmsDao.getCategoryIdsWithSmsCount(ctx);
    }
    assertEquals(2, SmsDao.countAll(ctx));
  }
}
