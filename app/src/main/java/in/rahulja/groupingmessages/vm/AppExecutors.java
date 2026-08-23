package in.rahulja.groupingmessages.vm;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {

  private static final ExecutorService DISK_IO = Executors.newSingleThreadExecutor();

  public static void disk(Runnable r) {
    DISK_IO.execute(r);
  }

  public static void main(Runnable r) {
    new Handler(Looper.getMainLooper()).post(r);
  }
}
