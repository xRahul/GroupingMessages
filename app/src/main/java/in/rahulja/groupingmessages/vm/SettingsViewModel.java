package in.rahulja.groupingmessages.vm;

import android.app.Application;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import in.rahulja.groupingmessages.BuildConfig;
import in.rahulja.groupingmessages.R;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsViewModel extends AndroidViewModel {

  public enum VersionState { UP_TO_DATE, OUTDATED, ERROR }

  public static final int CONNECT_TIMEOUT_MS = 5000;
  public static final int READ_TIMEOUT_MS = 5000;

  private final MutableLiveData<VersionState> versionState = new MutableLiveData<>();
  private String latestVersionUrl;

  public SettingsViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<VersionState> getVersionState() {
    return versionState;
  }

  /** Latest release page URL when the check ended OUTDATED, else null. */
  public String getLatestVersionUrl() {
    return latestVersionUrl;
  }

  public LiveData<VersionState> checkLatestVersion() {
    AppExecutors.disk(() -> {
      VersionState result = fetchLatestVersion();
      AppExecutors.main(() -> versionState.setValue(result));
    });
    return versionState;
  }

  public VersionState fetchLatestVersion() {
    try {
      URL url = new URL(getApplication().getString(R.string.latest_release_url));
      HttpURLConnection ucon = openConnection(url);
      String secondUrl = resolveRedirect(ucon);
      Log.d("GM/updateUrl", secondUrl);
      String checkUrl =
          getApplication().getString(R.string.current_release_url_prefix)
              + BuildConfig.VERSION_NAME;
      if (secondUrl.equals(checkUrl)) {
        latestVersionUrl = null;
        return VersionState.UP_TO_DATE;
      }
      latestVersionUrl = secondUrl;
      return VersionState.OUTDATED;
    } catch (Throwable t) {
      Log.e("GM/SettingsViewModel", "version check failed: " + t);
      latestVersionUrl = null;
      return VersionState.ERROR;
    }
  }

  public HttpURLConnection openConnection(URL url) throws IOException {
    return (HttpURLConnection) url.openConnection();
  }

  private static String resolveRedirect(HttpURLConnection ucon) throws IOException {
    ucon.setInstanceFollowRedirects(false);
    ucon.setConnectTimeout(CONNECT_TIMEOUT_MS);
    ucon.setReadTimeout(READ_TIMEOUT_MS);
    URL secondURL = new URL(ucon.getHeaderField("Location"));
    return String.valueOf(secondURL);
  }

  /** Version label from a release URL, matching legacy summary text. */
  public static String lastPathSegment(String url) {
    return Uri.parse(url).getLastPathSegment();
  }
}
