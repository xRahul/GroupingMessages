package in.rahulja.groupingmessages.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import in.rahulja.groupingmessages.DatabaseHelper;

public final class AppDatabase {

  private AppDatabase() {
  }

  /**
   * Returns the writable application database, opening it lazily if needed.
   *
   * <p>Contract: {@link DatabaseBackup} export/import callers must serialize
   * the whole file swap under {@code synchronized (AppDatabase.class)} with no
   * in-flight readers — close first, swap files, and only then let the next
   * call here reopen the database.
   */
  public static synchronized SQLiteDatabase get(Context ctx) {
    return DatabaseHelper.getInstance(ctx).getWritableDatabase();
  }

  /**
   * Closes the application database; the next {@link #get(Context)} reopens it.
   *
   * <p>See the contract on {@link #get(Context)} for how backup/import swaps
   * must be serialized.
   */
  public static synchronized void close(Context ctx) {
    DatabaseHelper.getInstance(ctx).close();
  }
}
