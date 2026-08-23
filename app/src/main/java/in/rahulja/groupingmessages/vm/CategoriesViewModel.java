package in.rahulja.groupingmessages.vm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import in.rahulja.groupingmessages.DatabaseContract;
import in.rahulja.groupingmessages.ExternalContentBridge;
import in.rahulja.groupingmessages.TrainSms;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Category;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesViewModel extends AndroidViewModel {

  private static final long UNKNOWN_CATEGORY_ID = 1L;
  private static final String SMS_COUNT = "sms_count";

  private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
  private final MutableLiveData<Map<Long, String>> unreadCountsByCategoryId =
      new MutableLiveData<>();
  private final MutableLiveData<Map<Long, String>> readCountsByCategoryId =
      new MutableLiveData<>();
  private final MutableLiveData<Long> newlyAddedSmsCount = new MutableLiveData<>();
  private final MutableLiveData<String> addedCategoryName = new MutableLiveData<>();

  public CategoriesViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<Category>> getCategories() {
    return categories;
  }

  public LiveData<Map<Long, String>> getUnreadCounts() {
    return unreadCountsByCategoryId;
  }

  public LiveData<Map<Long, String>> getReadCounts() {
    return readCountsByCategoryId;
  }

  public LiveData<Long> getNewlyAddedSmsCount() {
    return newlyAddedSmsCount;
  }

  public LiveData<String> getAddedCategoryName() {
    return addedCategoryName;
  }

  public void consumeNewlyAddedSmsCount() {
    newlyAddedSmsCount.setValue(null);
  }

  public void consumeAddedCategoryName() {
    addedCategoryName.setValue(null);
  }

  public void syncLatestSms() {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      List<Map<String, String>> trainedLatestSmsFromInbox = TrainSms.getTrainedListOfSms(
          application,
          ExternalContentBridge.getLatestSmsFromInbox(application),
          SmsDao.getSelfTrained(application)
      );

      addSenderTypeToListOfSms(trainedLatestSmsFromInbox);

      long numRowsAddedToSms = SmsDao.storeTrainedInboxSms(
          application,
          trainedLatestSmsFromInbox
      );

      AppExecutors.main(() -> {
        newlyAddedSmsCount.setValue(numRowsAddedToSms);
        if (numRowsAddedToSms > 0) {
          refresh();
        }
      });
    });
  }

  public static void addSenderTypeToListOfSms(List<Map<String, String>> listOfSms) {

    for (int i = 0; i < listOfSms.size(); i++) {
      Map<String, String> tempSms = listOfSms.get(i);
      String fromString = tempSms.get(DatabaseContract.Sms.KEY_ADDRESS);
      int senderType = DatabaseContract.Sms.SENDER_CONTACT;
      if ("0".equals(String.valueOf(tempSms.get(DatabaseContract.Sms.KEY_PERSON)))) {
        senderType = DatabaseContract.Sms.SENDER_COMPANY;
        if (fromString.matches(".*[0-9]{10}.*") && !fromString.matches(".*[a-zA-Z]+.*")) {
          senderType = DatabaseContract.Sms.SENDER_NUMBER;
        }
      }

      tempSms.put(
          DatabaseContract.Sms.KEY_SENDER_TYPE,
          String.valueOf(senderType)
      );

      listOfSms.set(i, tempSms);
    }
  }

  public void refresh() {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      List<Category> loadedCategories = loadVisibleCategories(application);
      Map<Long, String> unreadCounts = zeroedCountsFor(loadedCategories);
      Map<Long, String> readCounts = zeroedCountsFor(loadedCategories);
      joinSmsCountsInto(application, unreadCounts, readCounts);

      AppExecutors.main(() -> {
        unreadCountsByCategoryId.setValue(unreadCounts);
        readCountsByCategoryId.setValue(readCounts);
        categories.setValue(loadedCategories);
      });
    });
  }

  public void addCategory(String name, int color) {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      Map<String, String> newCategory = new HashMap<>();

      newCategory.put(DatabaseContract.Category.KEY_NAME, name);
      newCategory.put(DatabaseContract.Category.KEY_VISIBILITY, String.valueOf(1));
      newCategory.put(DatabaseContract.Category.KEY_COLOR, String.valueOf(color));

      Boolean categoryAdded = CategoryDao.addCategory(application, newCategory);

      AppExecutors.main(() -> {
        addedCategoryName.setValue(Boolean.TRUE.equals(categoryAdded) ? name : null);
        refresh();
      });
    });
  }

  public void deleteCategory(long categoryId) {

    if (categoryId == UNKNOWN_CATEGORY_ID) {
      return;
    }

    Application application = getApplication();
    AppExecutors.disk(() -> {
      CategoryDao.deleteCategory(application, categoryId);
      AppExecutors.main(this::refresh);
    });
  }

  private static List<Category> loadVisibleCategories(Application application) {

    List<Category> loadedCategories = new ArrayList<>();

    for (Map<String, String> categoryMap : CategoryDao.getAllVisibleCategories(application)) {
      loadedCategories.add(new Category(
          Long.parseLong(categoryMap.get(DatabaseContract.Category._ID)),
          categoryMap.get(DatabaseContract.Category.KEY_NAME),
          Integer.parseInt(categoryMap.get(DatabaseContract.Category.KEY_COLOR))
      ));
    }
    return loadedCategories;
  }

  private static Map<Long, String> zeroedCountsFor(List<Category> loadedCategories) {

    Map<Long, String> counts = new HashMap<>();
    for (Category category : loadedCategories) {
      counts.put(category.getId(), "0");
    }
    return counts;
  }

  private static void joinSmsCountsInto(Application application, Map<Long, String> unreadCounts,
      Map<Long, String> readCounts) {

    List<Map<String, String>> categoryIdsWithSmsCount =
        SmsDao.getCategoryIdsWithSmsCount(application);

    for (Map<String, String> countRow : categoryIdsWithSmsCount) {
      long categoryId = Long.parseLong(countRow.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
      String count = countRow.get(SMS_COUNT);

      if (Integer.parseInt(countRow.get(DatabaseContract.Sms.KEY_READ)) == 1) {
        readCounts.put(categoryId, count);
      } else {
        unreadCounts.put(categoryId, count);
      }
    }
  }
}
