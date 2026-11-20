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
import java.io.InputStream;
import java.io.OutputStream;
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
        // Re-verify inside the lock: the backup file can change between the
        // pre-flight check and the copy above. Installing a non-SQLite or
        // foreign-schema file triggers silent data loss on next open.
        validateBackup(tmpOf(currentDB));
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
   * Writes a consistent copy of the live database to {@code out}. The caller
   * owns the stream. The destination is usually a Storage Access Framework
   * document chosen by the user.
   */
  public static void exportTo(Context context, OutputStream out) throws IOException {
    synchronized (AppDatabase.class) {
      SQLiteDatabase db = AppDatabase.get(context);
      checkpoint(db);
      AppDatabase.close(context);

      File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);
      try (FileInputStream in = new FileInputStream(currentDB)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
        out.flush();
      } catch (IOException e) {
        Log.e(TAG, "export failed: " + e);
        throw e;
      }
    }
  }

  /**
   * Installs a database from {@code in} after validating it on a scratch copy.
   * The live database is not touched until validation passes.
   */
  public static void importFrom(Context context, InputStream in) throws IOException {
    File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);
    File tmp = tmpOf(currentDB);
    try {
      try (FileOutputStream out = new FileOutputStream(tmp)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
        out.flush();
      }
      validateBackup(tmp);

      synchronized (AppDatabase.class) {
        AppDatabase.close(context);
        deleteIfExists(sidecar(currentDB, WAL_SUFFIX));
        deleteIfExists(sidecar(currentDB, SHM_SUFFIX));
        rename(tmp, currentDB);
      }
    } catch (IOException e) {
      deleteIfExists(tmp);
      Log.e(TAG, "import failed: " + e);
      throw e;
    }
  }

  private static void checkpoint(SQLiteDatabase db) {
    try (Cursor cursor = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) {
      while (cursor.moveToNext()) {
        // Fully drain the checkpoint result row(s).
      }
    }
  }

  /**
   * Rejects a well-formed but foreign or future-schema SQLite file before it
   * replaces the live database.
   */
  private static void validateBackup(File file) throws IOException {
    verifySqliteHeader(file);
    try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
        file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
      if (db.getVersion() > DatabaseContract.DATABASE_VERSION) {
        throw new IOException("Backup schema is newer than this app");
      }
      try (Cursor cursor = db.rawQuery("PRAGMA integrity_check", null)) {
        if (!cursor.moveToFirst() || !"ok".equalsIgnoreCase(cursor.getString(0))) {
          throw new IOException("Backup failed SQLite integrity check");
        }
      }
      for (String table : new String[] {
          DatabaseContract.Sms.TABLE_NAME,
          DatabaseContract.Category.TABLE_NAME,
          DatabaseContract.Config.TABLE_NAME
      }) {
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) {
          if (!cursor.moveToFirst()) {
            throw new IOException("Backup is missing table: " + table);
          }
        }
      }
    } catch (android.database.sqlite.SQLiteException e) {
      throw new IOException("Backup is not a valid SQLite database", e);
    }
  }

  /**
   * Copies the live database to the fixed app-specific backup file after
   * flushing the WAL. Kept for legacy imports; new callers use SAF streams.
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
   * Cheap pre-flight check used to reject obvious non-SQLite files before any
   * copy. Full integrity and schema validation lives in {@link #validateBackup}.
   */
  private static void verifySqliteHeader(File backupDB) throws IOException {
    try (FileInputStream in = new FileInputStream(backupDB)) {
      byte[] magic = new byte[SQLITE_HEADER.length()];
      int offset = 0;
      while (offset < magic.length) {
        int read = in.read(magic, offset, magic.length - offset);
        if (read < 0) {
          throw new IOException("Not a valid SQLite database backup: " + backupDB.getAbsolutePath());
        }
        offset += read;
      }
      if (!SQLITE_HEADER.equals(new String(magic, StandardCharsets.UTF_8))) {
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
        long size = src.size();
        long position = 0;
        while (position < size) {
          position += dst.transferFrom(src, position, size - position);
        }
        dst.force(true);
      }
    }
  }
}
