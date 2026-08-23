package in.rahulja.groupingmessages.db;

import android.content.Context;
import android.util.Log;
import in.rahulja.groupingmessages.DatabaseContract;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public final class DatabaseBackup {

  private static final String TAG = "GM/DatabaseBackup";
  private static final String BACKUP_DB_PATH =
      "GroupMessagingBackupV" + DatabaseContract.DATABASE_VERSION;

  private DatabaseBackup() {
  }

  public static void importDb(Context context) {

    AppDatabase.close(context);

    try {
      File sd = context.getExternalFilesDir(null);
      if (sd != null && sd.canWrite()) {
        File backupDB = new File(sd, BACKUP_DB_PATH);
        File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);

        copyFile(backupDB, currentDB);
        return;
      }
    } catch (Exception e) {
      Log.e(TAG, e.toString());
    }
  }

  public static void exportDb(Context context) {

    AppDatabase.close(context);

    try {
      File sd = context.getExternalFilesDir(null);

      if (sd != null && sd.canWrite()) {
        File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);
        File backupDB = new File(sd, BACKUP_DB_PATH);

        copyFile(currentDB, backupDB);
        return;
      }
    } catch (Exception e) {
      Log.e(TAG, e.toString());
    }
  }

  private static void copyFile(File source, File target) throws IOException {
    try (FileInputStream fis = new FileInputStream(source)) {
      try (FileOutputStream fos = new FileOutputStream(target)) {
        FileChannel src = fis.getChannel();
        FileChannel dst = fos.getChannel();
        dst.transferFrom(src, 0, src.size());
      }
    }
  }
}
