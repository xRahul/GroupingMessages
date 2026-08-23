package in.rahulja.groupingmessages.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import in.rahulja.groupingmessages.DatabaseContract;
import in.rahulja.groupingmessages.model.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class CategoryDao {

  private static final String TAG = "GM/CategoryDao";
  private static final String EQUALS_QUESTION = " = ? ";
  private static final long UNKNOWN_CATEGORY_ID = 1L;

  private CategoryDao() {
  }

  public static List<Map<String, String>> getAllVisibleCategories(Context context) {

    String selection = DatabaseContract.Category.KEY_VISIBILITY + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(1) };

    Log.i(TAG, "Getting visible categories");
    return queryMaps(context, selection, selectionArgs);
  }

  public static List<Category> getAllOrderedByPipelineOrder(Context context) {

    List<Category> categories = new ArrayList<>();

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Category.TABLE_NAME,
        DatabaseContract.Category.KEY_ARRAY,
        null,
        null,
        null,
        null,
        DatabaseContract.Category.DEFAULT_SORT_ORDER
    )) {
      while (cursor != null && cursor.moveToNext()) {
        categories.add(Category.fromCursor(cursor));
      }
    }
    return categories;
  }

  public static Category getById(Context context, long categoryId) {

    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Category.TABLE_NAME,
        DatabaseContract.Category.KEY_ARRAY,
        selection,
        selectionArgs,
        null,
        null,
        null
    )) {
      if (cursor != null && cursor.moveToFirst()) {
        return Category.fromCursor(cursor);
      }
    }
    return null;
  }

  public static Boolean addCategory(Context context, Map<String, String> category) {
    return insertMap(context, category) != -1;
  }

  public static Boolean updateCategory(Context context, Map<String, String> category) {
    return updateMap(context, category) > 0;
  }

  public static void deleteCategories(Context context) {

    SmsDao.deleteModel(context);

    String selection = DatabaseContract.Category._ID + " != ?";
    String[] selectionArgs = { String.valueOf(UNKNOWN_CATEGORY_ID) };

    SQLiteDatabase db = AppDatabase.get(context);
    db.delete(
        DatabaseContract.Category.TABLE_NAME,
        selection,
        selectionArgs
    );
  }

  public static void deleteCategory(Context context, long categoryId) {

    SmsDao.deleteModelForCategory(context, categoryId);

    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category.KEY_VISIBILITY, 0);

    SQLiteDatabase db = AppDatabase.get(context);
    db.update(
        DatabaseContract.Category.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );
  }

  public static int getCountPerCategory(Context context, long categoryId) {

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        new String[] { "COUNT(*)" },
        DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION,
        new String[] { String.valueOf(categoryId) },
        null,
        null,
        null
    )) {
      if (cursor != null && cursor.moveToFirst()) {
        return cursor.getInt(0);
      }
    }
    return 0;
  }

  public static void ensureDefaultsExist(Context context) {

    if (getById(context, UNKNOWN_CATEGORY_ID) != null) {
      return;
    }

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category._ID, UNKNOWN_CATEGORY_ID);
    values.put(DatabaseContract.Category.KEY_NAME, "Unknown");
    values.put(DatabaseContract.Category.KEY_COLOR, Color.WHITE);
    values.put(DatabaseContract.Category.KEY_VISIBILITY, 1);

    SQLiteDatabase db = AppDatabase.get(context);
    long rowId = db.insert(DatabaseContract.Category.TABLE_NAME, null, values);

    if (rowId == -1) {
      Log.e(TAG, "Error while adding Unknown category");
    }
  }

  public static long insert(Context context, String name) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category.KEY_NAME, name);
    values.put(DatabaseContract.Category.KEY_COLOR, Color.WHITE);
    values.put(DatabaseContract.Category.KEY_VISIBILITY, 1);

    SQLiteDatabase db = AppDatabase.get(context);
    return db.insert(DatabaseContract.Category.TABLE_NAME, null, values);
  }

  public static int rename(Context context, long categoryId, String newName) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category.KEY_NAME, newName);

    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    SQLiteDatabase db = AppDatabase.get(context);
    return db.update(
        DatabaseContract.Category.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );
  }

  public static int deleteById(Context context, long categoryId) {

    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    SQLiteDatabase db = AppDatabase.get(context);
    return db.delete(
        DatabaseContract.Category.TABLE_NAME,
        selection,
        selectionArgs
    );
  }

  private static List<Map<String, String>> queryMaps(Context context, String selection,
      String[] selectionArgs) {

    List<Map<String, String>> categories = new ArrayList<>();

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Category.TABLE_NAME,
        DatabaseContract.Category.KEY_ARRAY,
        selection,
        selectionArgs,
        null,
        null,
        DatabaseContract.Category.DEFAULT_SORT_ORDER
    )) {
      while (cursor != null && cursor.moveToNext()) {
        Map<String, String> categoryTemp = new HashMap<>();
        for (String key : DatabaseContract.Category.KEY_ARRAY) {
          if (DatabaseContract.Category._ID.equals(key)) {
            categoryTemp.put(key, String.valueOf(cursor.getLong(
                cursor.getColumnIndexOrThrow(key))));
          } else if (DatabaseContract.Category.KEY_VISIBILITY.equals(key)) {
            categoryTemp.put(key, String.valueOf(cursor.getInt(
                cursor.getColumnIndexOrThrow(key))));
          } else {
            categoryTemp.put(key, cursor.getString(cursor.getColumnIndexOrThrow(key)));
          }
        }
        categories.add(categoryTemp);
      }
    }
    Log.i(TAG, String.valueOf(categories.size()));
    return categories;
  }

  private static long insertMap(Context context, Map<String, String> category) {

    ContentValues values = contentValuesFromMap(category);

    SQLiteDatabase db = AppDatabase.get(context);
    return db.insert(DatabaseContract.Category.TABLE_NAME, null, values);
  }

  private static long updateMap(Context context, Map<String, String> category) {

    ContentValues values = contentValuesFromMap(category);

    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(category.get(DatabaseContract.Category._ID)) };

    SQLiteDatabase db = AppDatabase.get(context);
    return db.update(
        DatabaseContract.Category.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );
  }

  private static ContentValues contentValuesFromMap(Map<String, String> category) {
    ContentValues values = new ContentValues();
    values.put(
        DatabaseContract.Category.KEY_NAME,
        category.get(DatabaseContract.Category.KEY_NAME)
    );
    values.put(
        DatabaseContract.Category.KEY_VISIBILITY,
        Integer.parseInt(category.get(DatabaseContract.Category.KEY_VISIBILITY))
    );
    values.put(
        DatabaseContract.Category.KEY_COLOR,
        Integer.parseInt(category.get(DatabaseContract.Category.KEY_COLOR))
    );

    return values;
  }
}
