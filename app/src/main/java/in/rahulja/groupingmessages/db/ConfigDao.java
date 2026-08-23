package in.rahulja.groupingmessages.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import in.rahulja.groupingmessages.DatabaseContract;
import in.rahulja.groupingmessages.model.ConfigEntry;
import java.util.ArrayList;
import java.util.List;

public final class ConfigDao {

  private static final String TAG = "GM/ConfigDao";
  private static final String EQUALS_QUESTION = " = ? ";

  private ConfigDao() {
  }

  public static String getValue(Context context, String key) {

    String selection = DatabaseContract.Config.KEY_NAME + EQUALS_QUESTION;
    String[] selectionArgs = { key };

    List<ConfigEntry> entries = query(context, selection, selectionArgs);

    if (!entries.isEmpty()) {
      return entries.get(0).getValue();
    }

    return null;
  }

  public static void put(Context context, String key, String value) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Config.KEY_NAME, key);
    values.put(DatabaseContract.Config.KEY_VALUE, value);
    Log.i(TAG, "Values: " + values);

    SQLiteDatabase db = AppDatabase.get(context);
    long resultId = db.insertWithOnConflict(
        DatabaseContract.Config.TABLE_NAME,
        DatabaseContract.Config._ID,
        values,
        SQLiteDatabase.CONFLICT_REPLACE
    );

    Log.i(TAG, "Result Id: " + resultId);
  }

  private static List<ConfigEntry> query(Context context, String selection,
      String[] selectionArgs) {

    List<ConfigEntry> entries = new ArrayList<>();

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Config.TABLE_NAME,
        DatabaseContract.Config.KEY_ARRAY,
        selection,
        selectionArgs,
        null,
        null,
        DatabaseContract.Config.DEFAULT_SORT_ORDER
    )) {
      while (cursor != null && cursor.moveToNext()) {
        entries.add(ConfigEntry.fromCursor(cursor));
      }
    }
    return entries;
  }
}
