package in.rahulja.groupingmessages.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import in.rahulja.groupingmessages.DatabaseHelper;

public final class AppDatabase {

  private AppDatabase() {
  }

  public static synchronized SQLiteDatabase get(Context ctx) {
    return DatabaseHelper.getInstance(ctx).getWritableDatabase();
  }

  public static synchronized void close(Context ctx) {
    DatabaseHelper.getInstance(ctx).close();
  }
}
