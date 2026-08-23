package in.rahulja.groupingmessages.vm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class AppExecutorsTest {

  @Test
  public void diskRunsOffMainThread() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean onMainThread = new AtomicBoolean(true);
    AppExecutors.disk(() -> {
      onMainThread.set(Looper.getMainLooper().isCurrentThread());
      latch.countDown();
    });
    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertFalse(onMainThread.get());
  }

  @Test
  public void mainPostsToMainLooper() {
    AtomicBoolean ran = new AtomicBoolean(false);
    AppExecutors.main(() -> {
      ran.set(true);
      assertTrue(Looper.getMainLooper().isCurrentThread());
    });
    assertFalse(ran.get());
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    assertTrue(ran.get());
  }
}
