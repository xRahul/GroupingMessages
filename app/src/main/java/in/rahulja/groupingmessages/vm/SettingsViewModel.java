package in.rahulja.groupingmessages.vm;

import android.app.Application;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import in.rahulja.groupingmessages.BuildConfig;
import in.rahulja.groupingmessages.R;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class SettingsViewModel extends AndroidViewModel {

  public enum VersionState { UP_TO_DATE, OUTDATED, ERROR, DISABLED }

  public static final String PREF_CHECK_UPDATES = "key_check_updates";
  public static final int CONNECT_TIMEOUT_MS = 5000;
  public static final int READ_TIMEOUT_MS = 5000;
  private static final String RELEASE_HOST = "github.com";

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
    boolean enabled = PreferenceManager.getDefaultSharedPreferences(getApplication())
        .getBoolean(PREF_CHECK_UPDATES, false);
    return checkLatestVersion(enabled);
  }

  public LiveData<VersionState> checkLatestVersion(boolean enabled) {
    if (!enabled) {
      latestVersionUrl = null;
      versionState.setValue(VersionState.DISABLED);
      return versionState;
    }
    AppExecutors.network(() -> {
      VersionState result = fetchLatestVersion();
      AppExecutors.main(() -> versionState.setValue(result));
    });
    return versionState;
  }

  public VersionState fetchLatestVersion() {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(getApplication().getString(R.string.latest_release_url));
      connection = openConnection(url);
      String secondUrl = resolveRedirect(connection);
      Log.d("GM/updateUrl", secondUrl);
      if (!isTrustedReleaseUrl(secondUrl)) {
        latestVersionUrl = null;
        return VersionState.ERROR;
      }
      String tag = lastPathSegment(secondUrl);
      if (tag != null && tag.startsWith("v")) {
        tag = tag.substring(1);
      }
      if (BuildConfig.VERSION_NAME.equals(tag)) {
        latestVersionUrl = null;
        return VersionState.UP_TO_DATE;
      }
      latestVersionUrl = secondUrl;
      return VersionState.OUTDATED;
    } catch (Throwable t) {
      Log.e("GM/SettingsViewModel", "version check failed: " + t);
      latestVersionUrl = null;
      return VersionState.ERROR;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  public HttpURLConnection openConnection(URL url) throws IOException {
    return (HttpURLConnection) url.openConnection();
  }

  private static String resolveRedirect(HttpURLConnection connection) throws IOException {
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("HEAD");
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);
    String location = connection.getHeaderField("Location");
    if (location == null || location.isEmpty()) {
      throw new IOException("No redirect location");
    }
    return String.valueOf(new URL(location));
  }

  /**
   * Only GitHub release pages for this repository are accepted, so a compromised
   * or misconfigured redirect cannot send the user to an arbitrary app.
   */
  public static boolean isTrustedReleaseUrl(String url) {
    try {
      URI uri = new URI(url);
      return "https".equalsIgnoreCase(uri.getScheme())
          && RELEASE_HOST.equalsIgnoreCase(uri.getHost())
          && uri.getPath() != null
          && uri.getPath().startsWith("/xRahul/GroupingMessages/releases/tag/");
    } catch (Exception e) {
      return false;
    }
  }

  /** Version label from a release URL, matching legacy summary text. */
  public static String lastPathSegment(String url) {
    if (url == null) {
      return null;
    }
    return Uri.parse(url).getLastPathSegment();
  }
}
