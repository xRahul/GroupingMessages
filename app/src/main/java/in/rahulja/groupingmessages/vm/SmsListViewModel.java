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

  private final MutableLiveData<List<Sms>> smsList = new MutableLiveData<>();

  public SmsListViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<Sms>> getSms(long categoryId) {
    return smsList;
  }

  public void refresh(long categoryId) {

    Application application = getApplication();
    AppExecutors.disk(() -> {
      List<Sms> loadedSms = SmsDao.getVisibleByCategory(application, categoryId);
      AppExecutors.main(() -> smsList.setValue(loadedSms));
    });
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

      List<Sms> loadedSms = SmsDao.getVisibleByCategory(application, categoryId);
      AppExecutors.main(() -> smsList.setValue(loadedSms));
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
