package in.rahulja.groupingmessages.vm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import in.rahulja.groupingmessages.DatabaseContract;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Sms;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsListViewModel extends AndroidViewModel {

  public static final long ALL_CATEGORIES = -1L;

  private final MutableLiveData<List<Sms>> smsList = new MutableLiveData<>();

  public SmsListViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<Sms>> getSms(long categoryId) {
    return smsList;
  }

  public void refresh(long categoryId) {

    Application application = getApplication();
    AppExecutors.disk(() -> refreshList(application, categoryId));
  }

  public interface DeleteCallback {
    void onDeleted(List<Map<String, String>> snapshot);
  }

  public void deleteSms(List<Long> ids, long categoryId) {
    deleteSms(ids, categoryId, null);
  }

  public void deleteSms(List<Long> ids, long categoryId, DeleteCallback callback) {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      List<Map<String, String>> snapshot = SmsDao.getByIds(application, ids);
      SmsDao.deleteSmsByIds(application, ids);
      refreshList(application, categoryId);
      if (callback != null) {
        AppExecutors.main(() -> callback.onDeleted(snapshot));
      }
    });
  }

  public void restoreSms(List<Map<String, String>> snapshot, long categoryId) {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      SmsDao.restoreSms(application, snapshot);
      refreshList(application, categoryId);
    });
  }

  private void refreshList(Application application, long categoryId) {
    List<Sms> loadedSms = categoryId == ALL_CATEGORIES
        ? SmsDao.getAllVisible(application)
        : SmsDao.getVisibleByCategory(application, categoryId);
    AppExecutors.main(() -> smsList.setValue(loadedSms));
  }

  public void swipeDelete(Sms sms, long categoryId) {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      Map<String, String> data = new HashMap<>();
      data.put(DatabaseContract.Sms._ID, String.valueOf(sms.getId()));
      data.put(DatabaseContract.Sms.KEY_SIMILAR_TO, String.valueOf(sms.getSimilarTo()));

      // legacy decision lives in SmsDao.deleteSmsByMap: self-trained sms
      // (_ID == similar_to) is hidden (visibility=0), untrained is deleted
      SmsDao.deleteSmsByMap(application, data);
      refreshList(application, categoryId);
    });
  }

  public void markRead(List<Long> ids) {

    Application application = getApplication();
    AppExecutors.disk(() -> SmsDao.markRead(application, ids));
  }

  public void moveToCategory(List<Long> ids, long targetId) {

    Application application = getApplication();
    AppExecutors.disk(() -> SmsDao.changeCategory(application, ids, targetId));
  }
}
