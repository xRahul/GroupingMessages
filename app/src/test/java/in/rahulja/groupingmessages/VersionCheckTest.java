package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import in.rahulja.groupingmessages.vm.SettingsViewModel;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class VersionCheckTest {

  // mockito-core 4.11 bundles a Byte Buddy that predates the running JVM;
  // opt in to forward-version instrumentation so HttpURLConnection mocks work
  static {
    System.setProperty("net.bytebuddy.experimental", "true");
  }

  private Application application;
  private HttpURLConnection connection;
  private SettingsViewModel viewModel;

  @Before
  public void setUp() throws Exception {
    application = RuntimeEnvironment.getApplication();
    connection = org.mockito.Mockito.mock(HttpURLConnection.class);
    viewModel = org.mockito.Mockito.spy(new SettingsViewModel(application));
    doReturn(connection).when(viewModel).openConnection(any(URL.class));
  }

  private String currentReleaseUrl() {
    return application.getString(R.string.current_release_url_prefix)
        + BuildConfig.VERSION_NAME;
  }

  @Test
  public void redirectTagMatchingCurrentVersionIsUpToDate() {
    when(connection.getHeaderField("Location")).thenReturn(currentReleaseUrl());

    assertEquals(SettingsViewModel.VersionState.UP_TO_DATE,
        viewModel.fetchLatestVersion());
    assertNull(viewModel.getLatestVersionUrl());
    assertTimeoutsSet();
  }

  @Test
  public void redirectTagNewerThanCurrentIsOutdated() {
    String newerUrl =
        application.getString(R.string.current_release_url_prefix) + "v9.9.9";
    when(connection.getHeaderField("Location")).thenReturn(newerUrl);

    assertEquals(SettingsViewModel.VersionState.OUTDATED,
        viewModel.fetchLatestVersion());
    assertEquals(newerUrl, viewModel.getLatestVersionUrl());
    assertTimeoutsSet();
  }

  @Test
  public void missingRedirectLocationIsError() {
    when(connection.getHeaderField("Location")).thenReturn(null);

    assertEquals(SettingsViewModel.VersionState.ERROR,
        viewModel.fetchLatestVersion());
    assertNull(viewModel.getLatestVersionUrl());
    assertTimeoutsSet();
  }

  @Test
  public void thrownConnectionFailureIsError() {
    // getHeaderField(String) declares no checked exceptions; real read
    // timeouts surface as SocketTimeoutException wrapped at runtime
    doThrow(new java.io.UncheckedIOException(
        new java.net.SocketTimeoutException("connect timed out")))
        .when(connection).getHeaderField("Location");

    assertEquals(SettingsViewModel.VersionState.ERROR,
        viewModel.fetchLatestVersion());
    assertNull(viewModel.getLatestVersionUrl());
    assertTimeoutsSet();
  }

  private void assertTimeoutsSet() {
    verify(connection).setConnectTimeout(SettingsViewModel.CONNECT_TIMEOUT_MS);
    verify(connection).setReadTimeout(SettingsViewModel.READ_TIMEOUT_MS);
  }
}
