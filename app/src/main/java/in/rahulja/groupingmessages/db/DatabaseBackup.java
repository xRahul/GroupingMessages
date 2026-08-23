package in.rahulja.groupingmessages.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import in.rahulja.groupingmessages.DatabaseContract;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Exports and imports the application database file to/from external storage.
 *
 * <p>Both operations swap the live database file, so each one holds
 * {@code synchronized (AppDatabase.class)} for its entire duration: the DB is
 * closed for the whole swap and must have no in-flight readers (see the
 * contract documented on {@link AppDatabase#get(Context)}).
 */
public final class DatabaseBackup {

  private static final String TAG = "GM/DatabaseBackup";

  /** Backup file name inside {@link Context#getExternalFilesDir(String)}. */
  public static final String BACKUP_DB_PATH =
      "GroupMessagingBackupV" + DatabaseContract.DATABASE_VERSION;

  private static final String TMP_SUFFIX = ".tmp";
  private static final String WAL_SUFFIX = "-wal";
  private static final String SHM_SUFFIX = "-shm";
  private static final String SQLITE_HEADER = "SQLite format 3\u0000";

  private DatabaseBackup() {
  }

  /**
   * Overwrites the live database with the backup file.
   *
   * @throws IOException if external storage is unavailable or copying fails;
   *     the live database may then be closed but its previous content stays intact
   */
  public static void importDb(Context context) throws IOException {
    File sd = context.getExternalFilesDir(null);
    File backupDB = backupFile(sd);

    synchronized (AppDatabase.class) {
      AppDatabase.close(context);

      try {
        File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);
        deleteIfExists(sidecar(currentDB, WAL_SUFFIX));
        deleteIfExists(sidecar(currentDB, SHM_SUFFIX));

        copyFile(backupDB, tmpOf(currentDB));
        rename(tmpOf(currentDB), currentDB);
      } catch (IOException e) {
        deleteIfExists(tmpOf(context.getDatabasePath(DatabaseContract.DATABASE_NAME)));
        Log.e(TAG, "import failed: " + e);
        throw e;
      }
    }
    // Live database reopens lazily via AppDatabase.get().
  }

  /**
   * Copies the live database to the backup file after flushing the WAL.
   *
   * @throws IOException if external storage is unavailable or copying fails
   */
  public static void exportDb(Context context) throws IOException {
    File sd = context.getExternalFilesDir(null);
    if (sd == null || !sd.canWrite()) {
      throw new IOException("External storage not writable: " + sd);
    }
    File backupDB = new File(sd, BACKUP_DB_PATH);

    synchronized (AppDatabase.class) {
      SQLiteDatabase db = AppDatabase.get(context);
      Cursor cursor = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null);
      try {
        while (cursor.moveToNext()) {
          // Fully drain the checkpoint result row(s).
        }
      } finally {
        cursor.close();
      }
      AppDatabase.close(context);

      try {
        copyFile(context.getDatabasePath(DatabaseContract.DATABASE_NAME), tmpOf(backupDB));
        rename(tmpOf(backupDB), backupDB);
      } catch (IOException e) {
        deleteIfExists(tmpOf(backupDB));
        Log.e(TAG, "export failed: " + e);
        throw e;
      }
    }
  }

  /**
   * Rejects non-SQLite sources before anything touches the live database.
   * ponytail: header check only; deeper corruption inside a well-formed file
   * surfaces via the platform's own handling on next open - upgrade path is a
   * {@code PRAGMA integrity_check} against a scratch copy.
   */
  private static void verifySqliteHeader(File backupDB) throws IOException {
    try (FileInputStream in = new FileInputStream(backupDB)) {
      byte[] magic = new byte[SQLITE_HEADER.length()];
      int read = in.read(magic);
      String header = read == magic.length ? new String(magic, StandardCharsets.UTF_8) : "";
      if (!SQLITE_HEADER.equals(header)) {
        throw new IOException("Not a valid SQLite database backup: " + backupDB.getAbsolutePath());
      }
    }
  }

  private static File backupFile(File sd) throws IOException {
    if (sd == null || !sd.canWrite()) {
      throw new IOException("External storage not writable: " + sd);
    }
    File backupDB = new File(sd, BACKUP_DB_PATH);
    if (!backupDB.isFile()) {
      throw new IOException("Backup file missing: " + backupDB.getAbsolutePath());
    }
    verifySqliteHeader(backupDB);
    return backupDB;
  }

  private static File tmpOf(File target) {
    return new File(target.getParentFile(), target.getName() + TMP_SUFFIX);
  }

  private static File sidecar(File dbFile, String suffix) {
    return new File(dbFile.getParentFile(), dbFile.getName() + suffix);
  }

  private static void deleteIfExists(File file) throws IOException {
    if (file.exists() && !file.delete()) {
      throw new IOException("Could not delete " + file.getAbsolutePath());
    }
  }

  private static void rename(File source, File target) throws IOException {
    if (!source.renameTo(target)) {
      throw new IOException(
          "Rename failed: " + source.getAbsolutePath() + " -> " + target.getAbsolutePath());
    }
  }

  private static void copyFile(File source, File target) throws IOException {
    try (FileInputStream fis = new FileInputStream(source)) {
      try (FileOutputStream fos = new FileOutputStream(target)) {
        FileChannel src = fis.getChannel();
        FileChannel dst = fos.getChannel();
        dst.transferFrom(src, 0, src.size());
        dst.force(true);
      }
    }
  }
}
