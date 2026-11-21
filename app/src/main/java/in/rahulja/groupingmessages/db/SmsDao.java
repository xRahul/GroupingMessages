package in.rahulja.groupingmessages.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import in.rahulja.groupingmessages.DatabaseContract;
import in.rahulja.groupingmessages.model.Sms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SmsDao {

  private static final String TAG = "GM/SmsDao";
  private static final String GM_CURSOR = "GM/cursor";
  private static final String CURSOR_IS_NULL = "Cursor is null: ";
  private static final String GM_STORE_TRAINED_INBOX_SMS = "GM/storeTrainedInboxSms";
  private static final String LAST_SMS_TIME_CONFIG = "lastSmsTime";
  private static final String SMS_COUNT = "sms_count";
  private static final String EQUALS_QUESTION = " = ? ";
  private static final long UNKNOWN_CATEGORY_ID = 1L;
  private static final long INSERT_DUPLICATE = -2L;
  private static final int MAX_SQL_VARIABLES = 900;

  private SmsDao() {
  }

  public static List<Map<String, String>> getAll(Context context) {
    return queryMaps(context, null, null);
  }

  public static List<Map<String, String>> getSelfTrained(Context context) {

    String selection = DatabaseContract.Sms._ID +
        " = " +
        DatabaseContract.Sms.KEY_SIMILAR_TO;

    Log.i(TAG, "getSelfTrained, selection= " + selection);
    return queryMaps(context, selection, null);
  }

  public static List<Map<String, String>> getByIds(Context context, List<Long> ids) {
    List<Map<String, String>> rows = new ArrayList<>();
    if (ids == null || ids.isEmpty()) {
      return rows;
    }
    for (int start = 0; start < ids.size(); start += MAX_SQL_VARIABLES) {
      int end = Math.min(start + MAX_SQL_VARIABLES, ids.size());
      List<Long> chunk = ids.subList(start, end);
      StringBuilder placeholders = new StringBuilder();
      String[] selectionArgs = new String[chunk.size()];
      for (int i = 0; i < chunk.size(); i++) {
        placeholders.append("?");
        selectionArgs[i] = String.valueOf(chunk.get(i));
        if (i < chunk.size() - 1) {
          placeholders.append(",");
        }
      }
      rows.addAll(queryMaps(context,
          DatabaseContract.Sms._ID + " IN (" + placeholders + ")", selectionArgs));
    }
    return rows;
  }

  /**
   * Restores a snapshot taken before {@link #deleteSmsByIds}. Hidden
   * self-trained rows are made visible again; removed untrained rows are
   * reinserted.
   */
  public static int restoreSms(Context context, List<Map<String, String>> rows) {
    if (rows == null || rows.isEmpty()) {
      return 0;
    }
    SQLiteDatabase db = AppDatabase.get(context);
    int restored = 0;
    db.beginTransaction();
    try {
      for (Map<String, String> row : rows) {
        String id = row.get(DatabaseContract.Sms._ID);
        String similarTo = row.get(DatabaseContract.Sms.KEY_SIMILAR_TO);
        if (id != null && id.equals(similarTo)) {
          ContentValues values = new ContentValues();
          values.put(DatabaseContract.Sms.KEY_VISIBILITY,
              Long.parseLong(row.get(DatabaseContract.Sms.KEY_VISIBILITY)));
          restored += db.update(
              DatabaseContract.Sms.TABLE_NAME,
              values,
              DatabaseContract.Sms._ID + EQUALS_QUESTION,
              new String[] { id });
        } else if (insertInTransaction(db, row) >= 0) {
          restored++;
        }
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return restored;
  }

  public static Map<String, String> getById(Context context, long smsId) {

    Map<String, String> smsMap = null;

    String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(smsId) };

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        DatabaseContract.Sms.KEY_ARRAY,
        selection,
        selectionArgs,
        null,
        null,
        null
    )) {
      if (cursor != null && cursor.moveToFirst()) {
        smsMap = smsMapFromCursor(cursor);
      }
    }
    return smsMap;
  }

  public static List<Map<String, String>> getVisibleMapsByCategory(Context context,
      long categoryId) {

    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION
        + " AND " + DatabaseContract.Sms.KEY_VISIBILITY + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId), String.valueOf(1) };

    return queryMaps(context, selection, selectionArgs);
  }

  public static List<Sms> getVisibleByCategory(Context context, long categoryId) {

    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION
        + " AND " + DatabaseContract.Sms.KEY_VISIBILITY + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId), String.valueOf(1) };

    return querySms(context, selection, selectionArgs);
  }

  public static List<Sms> getAllVisible(Context context) {
    return querySms(context,
        DatabaseContract.Sms.KEY_VISIBILITY + EQUALS_QUESTION,
        new String[] { String.valueOf(1) });
  }

  private static List<Sms> querySms(Context context, String selection, String[] selectionArgs) {

    List<Sms> smsList = new ArrayList<>();
    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        DatabaseContract.Sms.KEY_ARRAY,
        selection,
        selectionArgs,
        null,
        null,
        DatabaseContract.Sms.DEFAULT_SORT_ORDER
    )) {
      while (cursor != null && cursor.moveToNext()) {
        smsList.add(Sms.fromCursor(cursor));
      }
    }
    return smsList;
  }

  public static long storeTrainedInboxSms(Context context,
      List<Map<String, String>> trainedInboxSms) {

    long lastSmsTime = ConfigDao.getLong(context, LAST_SMS_TIME_CONFIG, 0L);
    Log.i(GM_STORE_TRAINED_INBOX_SMS, "before lastSmsTime: " + lastSmsTime);

    long numSmsStored = 0;
    long numSmsDuplicate = 0;
    long numSmsError = 0;
    long tempLastSmsTime = lastSmsTime;

    SQLiteDatabase localDb = AppDatabase.get(context);
    localDb.beginTransaction();
    try {
      for (Map<String, String> trainedSmsMap : trainedInboxSms) {

        long longDate = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_DATE));
        long insertResult = insertInTransaction(localDb, trainedSmsMap);

        if (insertResult >= 0) {
          numSmsStored += 1;
          tempLastSmsTime = Math.max(tempLastSmsTime, longDate);
        } else if (insertResult == INSERT_DUPLICATE) {
          numSmsDuplicate += 1;
          tempLastSmsTime = Math.max(tempLastSmsTime, longDate);
        } else {
          numSmsError += 1;
          // Never log the row: it contains the sender address and message body.
          Log.e("GM/insertSms", "Some error occured while inserting sms");
        }
      }
      localDb.setTransactionSuccessful();
    } finally {
      localDb.endTransaction();
    }

    if (tempLastSmsTime != lastSmsTime) {
      ConfigDao.put(context, LAST_SMS_TIME_CONFIG, String.valueOf(tempLastSmsTime));
    }

    Log.i(GM_STORE_TRAINED_INBOX_SMS, "after lastSmsTime: " + tempLastSmsTime);
    Log.i(GM_STORE_TRAINED_INBOX_SMS, "Number of sms inserted successfully: " + numSmsStored);
    Log.i(GM_STORE_TRAINED_INBOX_SMS, "Number of duplicate sms skipped: " + numSmsDuplicate);

    if (numSmsError > 0) {
      Log.e(GM_STORE_TRAINED_INBOX_SMS, "Number of sms failed to insert: " + numSmsError);
    }

    return numSmsStored;
  }

  public static long storeReTrainedSms(Context context,
      List<Map<String, String>> retrainedSmsList) {

    long numSmsUpdated = 0;

    SQLiteDatabase localDb = AppDatabase.get(context);
    localDb.beginTransaction();
    try {
      for (Map<String, String> reTrainedSmsMap : retrainedSmsList) {

        long updateSmsRowId = updateMapInTransaction(localDb, reTrainedSmsMap);

        if (updateSmsRowId > 0) {
          numSmsUpdated += 1;
        }
      }
      localDb.setTransactionSuccessful();
    } finally {
      localDb.endTransaction();
    }

    return numSmsUpdated;
  }

  public static void updateSmsData(Context context, Map<String, String> sms) {
    updateMap(context, sms);
  }

  public static void setSmsAsRead(Context context, String smsId) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_READ, 1);

    updateById(context, Long.parseLong(smsId), values);
  }

  public static void setAllCategorySmsAsRead(Context context, String categoryId) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_READ, 1);

    updateByCategoryId(context, Long.parseLong(categoryId), values);
  }

  public static void deleteSmsByMap(Context context, Map<String, String> data) {

    SQLiteDatabase db = AppDatabase.get(context);

    if (data.get(DatabaseContract.Sms._ID).equals(data.get(DatabaseContract.Sms.KEY_SIMILAR_TO))) {
      // hide self-trained sms
      ContentValues values = new ContentValues();
      values.put(DatabaseContract.Sms.KEY_VISIBILITY, 0);
      long smsId = Long.parseLong(data.get(DatabaseContract.Sms._ID));
      updateById(context, smsId, values);
    } else {
      String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
      String[] selectionArgs = { data.get(DatabaseContract.Sms._ID) };
      db.delete(
          DatabaseContract.Sms.TABLE_NAME,
          selection,
          selectionArgs
      );
    }
  }

  public static void deleteAllSmsOfCategoryById(Context context, long categoryId) {

    SQLiteDatabase db = AppDatabase.get(context);

    // delete untrained sms first
    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION
        + " and "
        + DatabaseContract.Sms._ID + " <> " + DatabaseContract.Sms.KEY_SIMILAR_TO;
    String[] selectionArgs = { String.valueOf(categoryId) };
    db.delete(
        DatabaseContract.Sms.TABLE_NAME,
        selection,
        selectionArgs
    );

    // hide remaining trained sms
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_VISIBILITY, 0);
    updateByCategoryId(context, categoryId, values);
  }

  public static void deleteModel(Context context) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, UNKNOWN_CATEGORY_ID);
    values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, 0);
    values.put(DatabaseContract.Sms.KEY_SIM_SCORE, 0.0);

    SQLiteDatabase db = AppDatabase.get(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        null,
        null);
  }

  static void deleteModelForCategory(Context context, long categoryId) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, UNKNOWN_CATEGORY_ID);
    values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, 0);
    values.put(DatabaseContract.Sms.KEY_SIM_SCORE, 0.0);

    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    SQLiteDatabase db = AppDatabase.get(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs);
  }

  public static List<Map<String, String>> getCategoryIdsWithSmsCount(Context context) {

    List<Map<String, String>> categoryIdsWithSmsCount = new ArrayList<>();

    String[] projection = {
        DatabaseContract.Sms.KEY_CATEGORY_ID,
        DatabaseContract.Sms.KEY_READ,
        "COUNT(" + DatabaseContract.Sms.KEY_CATEGORY_ID + ") as " + SMS_COUNT
    };

    String selection = DatabaseContract.Sms.KEY_VISIBILITY + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(1) };

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        projection,
        selection,
        selectionArgs,
        DatabaseContract.Sms.KEY_CATEGORY_ID + ", "
            + DatabaseContract.Sms.KEY_READ,
        null,
        DatabaseContract.Sms.DEFAULT_SORT_ORDER
    )) {
      while (cursor != null && cursor.moveToNext()) {
        int indexCategoryId =
            cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID);
        int indexRead = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ);
        int indexSmsCount = cursor.getColumnIndexOrThrow(SMS_COUNT);

        Map<String, String> categorySmsCount = new HashMap<>();
        categorySmsCount.put(DatabaseContract.Sms.KEY_CATEGORY_ID,
            String.valueOf(cursor.getLong(indexCategoryId)));
        categorySmsCount.put(DatabaseContract.Sms.KEY_READ,
            String.valueOf(cursor.getLong(indexRead)));
        categorySmsCount.put(SMS_COUNT, String.valueOf(cursor.getLong(indexSmsCount)));

        categoryIdsWithSmsCount.add(categorySmsCount);
      }
    }

    Log.i("GM/getCatsSmsCount", categoryIdsWithSmsCount.toString());
    return categoryIdsWithSmsCount;
  }

  public static int markRead(Context context, List<Long> ids) {
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_READ, 1);
    return updateIds(context, ids, values);
  }

  public static int setVisibility(Context context, List<Long> ids, int visibility) {
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_VISIBILITY, visibility);
    return updateIds(context, ids, values);
  }

  public static int changeCategory(Context context, List<Long> ids, long newCategoryId) {
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, newCategoryId);
    return updateIds(context, ids, values);
  }

  public static int deleteByIds(Context context, List<Long> ids) {

    if (ids == null || ids.isEmpty()) {
      return 0;
    }

    StringBuilder placeholders = new StringBuilder();
    String[] selectionArgs = new String[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      placeholders.append("?");
      selectionArgs[i] = String.valueOf(ids.get(i));
      if (i < ids.size() - 1) {
        placeholders.append(",");
      }
    }

    SQLiteDatabase db = AppDatabase.get(context);
    return db.delete(
        DatabaseContract.Sms.TABLE_NAME,
        DatabaseContract.Sms._ID + " IN (" + placeholders + ")",
        selectionArgs
    );
  }

  /**
   * Batch delete matching the single-row contract: hide self-trained rows so
   * their exemplar survives, delete untrained rows. Chunked for large
   * selections and atomic across the whole batch.
   */
  public static int deleteSmsByIds(Context context, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }

    SQLiteDatabase db = AppDatabase.get(context);
    db.beginTransaction();
    try {
      int affected = 0;
      for (int start = 0; start < ids.size(); start += MAX_SQL_VARIABLES) {
        int end = Math.min(start + MAX_SQL_VARIABLES, ids.size());
        List<Long> chunk = ids.subList(start, end);
        StringBuilder placeholders = new StringBuilder();
        String[] selectionArgs = new String[chunk.size()];
        for (int i = 0; i < chunk.size(); i++) {
          placeholders.append("?");
          selectionArgs[i] = String.valueOf(chunk.get(i));
          if (i < chunk.size() - 1) {
            placeholders.append(",");
          }
        }

        ContentValues hidden = new ContentValues();
        hidden.put(DatabaseContract.Sms.KEY_VISIBILITY, 0);
        affected += db.update(
            DatabaseContract.Sms.TABLE_NAME,
            hidden,
            DatabaseContract.Sms._ID + " IN (" + placeholders + ") AND "
                + DatabaseContract.Sms._ID + " = " + DatabaseContract.Sms.KEY_SIMILAR_TO,
            selectionArgs);
        affected += db.delete(
            DatabaseContract.Sms.TABLE_NAME,
            DatabaseContract.Sms._ID + " IN (" + placeholders + ") AND "
                + DatabaseContract.Sms._ID + " != " + DatabaseContract.Sms.KEY_SIMILAR_TO,
            selectionArgs);
      }
      db.setTransactionSuccessful();
      return affected;
    } finally {
      db.endTransaction();
    }
  }

  public static int countAll(Context context) {

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        new String[] { "COUNT(*)" },
        null,
        null,
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

  public static int countUnreadInCategory(Context context, long categoryId) {

    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION
        + " AND " + DatabaseContract.Sms.KEY_READ + " = 0";
    String[] selectionArgs = { String.valueOf(categoryId) };

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        new String[] { "COUNT(*)" },
        selection,
        selectionArgs,
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

  private static int updateIds(Context context, List<Long> ids, ContentValues values) {

    if (ids == null || ids.isEmpty()) {
      return 0;
    }

    SQLiteDatabase db = AppDatabase.get(context);
    int updated = 0;
    // Stay below SQLite's bind-variable limit for large selections.
    for (int start = 0; start < ids.size(); start += MAX_SQL_VARIABLES) {
      int end = Math.min(start + MAX_SQL_VARIABLES, ids.size());
      List<Long> chunk = ids.subList(start, end);
      StringBuilder placeholders = new StringBuilder();
      String[] selectionArgs = new String[chunk.size()];
      for (int i = 0; i < chunk.size(); i++) {
        placeholders.append("?");
        selectionArgs[i] = String.valueOf(chunk.get(i));
        if (i < chunk.size() - 1) {
          placeholders.append(",");
        }
      }
      updated += db.update(
          DatabaseContract.Sms.TABLE_NAME,
          values,
          DatabaseContract.Sms._ID + " IN (" + placeholders + ")",
          selectionArgs
      );
    }
    return updated;
  }

  private static List<Map<String, String>> queryMaps(Context context, String selection,
      String[] selectArgs) {

    List<Map<String, String>> smsList = new ArrayList<>();

    SQLiteDatabase db = AppDatabase.get(context);
    try (Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,
        DatabaseContract.Sms.KEY_ARRAY,
        selection,
        selectArgs,
        null,
        null,
        DatabaseContract.Sms.DEFAULT_SORT_ORDER
    )) {
      while (cursor != null && cursor.moveToNext()) {
        smsList.add(smsMapFromCursor(cursor));
      }
    }

    return smsList;
  }

  private static Map<String, String> smsMapFromCursor(Cursor cursor) {

    Map<String, String> tempSms = new HashMap<>();

    for (String key : DatabaseContract.Sms.KEY_ARRAY) {
      int index = cursor.getColumnIndexOrThrow(key);
      switch (key) {
        case DatabaseContract.Sms._ID:
        case DatabaseContract.Sms.KEY_DATE:
        case DatabaseContract.Sms.KEY_PERSON:
        case DatabaseContract.Sms.KEY_READ:
        case DatabaseContract.Sms.KEY_SEEN:
        case DatabaseContract.Sms.KEY_VISIBILITY:
        case DatabaseContract.Sms.KEY_SENDER_TYPE:
        case DatabaseContract.Sms.KEY_CATEGORY_ID:
        case DatabaseContract.Sms.KEY_SIMILAR_TO:
          tempSms.put(key, String.valueOf(cursor.getLong(index)));
          break;
        case DatabaseContract.Sms.KEY_PROVIDER_ID:
          tempSms.put(key, cursor.isNull(index) ? null : String.valueOf(cursor.getLong(index)));
          break;
        case DatabaseContract.Sms.KEY_CREATED_AT:
        case DatabaseContract.Sms.KEY_UPDATED_AT:
          // legacy maps excluded timestamp columns
          continue;
        case DatabaseContract.Sms.KEY_SIM_SCORE:
          tempSms.put(key, String.valueOf(cursor.getFloat(index)));
          break;
        default:
          tempSms.put(key, cursor.getString(index));
          break;
      }
    }

    return tempSms;
  }

  private static long insertInTransaction(SQLiteDatabase db, Map<String, String> sms) {

    String providerIdValue = sms.get(DatabaseContract.Sms.KEY_PROVIDER_ID);
    if (providerIdValue != null && !providerIdValue.isEmpty()) {
      try (Cursor cursor = db.query(
          DatabaseContract.Sms.TABLE_NAME,
          new String[] { DatabaseContract.Sms._ID },
          DatabaseContract.Sms.KEY_PROVIDER_ID + EQUALS_QUESTION,
          new String[] { providerIdValue },
          null,
          null,
          null
      )) {
        if (cursor != null && cursor.moveToFirst()) {
          return INSERT_DUPLICATE;
        }
      }
    }

    ContentValues values = contentValuesFromMap(sms);
    long insertResult;
    try {
      insertResult = db.insertWithOnConflict(
          DatabaseContract.Sms.TABLE_NAME,
          null,
          values,
          SQLiteDatabase.CONFLICT_IGNORE
      );
    } catch (Exception e) {
      Log.e("GM/insertIntoSms", "Error occurred while inserting sms", e);
      insertResult = -1;
    }
    return insertResult;
  }

  private static long updateMap(Context context, Map<String, String> sms) {

    SQLiteDatabase db = AppDatabase.get(context);
    return updateMapInTransaction(db, sms);
  }

  private static long updateMapInTransaction(SQLiteDatabase db, Map<String, String> sms) {

    ContentValues values = contentValuesFromMap(sms);

    String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
    String[] selectionArgs =
        { String.valueOf(Long.parseLong(sms.get(DatabaseContract.Sms._ID))) };

    return db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );
  }

  private static void updateById(Context context, long smsId, ContentValues values) {

    String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(smsId) };

    SQLiteDatabase db = AppDatabase.get(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );
  }

  private static void updateByCategoryId(Context context, long categoryId,
      ContentValues values) {

    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    SQLiteDatabase db = AppDatabase.get(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );
  }

  private static ContentValues contentValuesFromMap(Map<String, String> sms) {

    ContentValues values = new ContentValues();
    values.put(
        DatabaseContract.Sms.KEY_DATE,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_DATE))
    );
    values.put(
        DatabaseContract.Sms.KEY_PERSON,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_PERSON))
    );
    values.put(
        DatabaseContract.Sms.KEY_READ,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_READ))
    );
    values.put(
        DatabaseContract.Sms.KEY_SEEN,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_SEEN))
    );
    values.put(
        DatabaseContract.Sms.KEY_SUBJECT,
        sms.get(DatabaseContract.Sms.KEY_SUBJECT)
    );
    values.put(
        DatabaseContract.Sms.KEY_BODY,
        sms.get(DatabaseContract.Sms.KEY_BODY)
    );
    values.put(
        DatabaseContract.Sms.KEY_CLEANED_SMS,
        sms.get(DatabaseContract.Sms.KEY_CLEANED_SMS)
    );
    values.put(
        DatabaseContract.Sms.KEY_VISIBILITY,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_VISIBILITY))
    );
    values.put(
        DatabaseContract.Sms.KEY_SENDER_TYPE,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_SENDER_TYPE))
    );
    values.put(
        DatabaseContract.Sms.KEY_ADDRESS,
        sms.get(DatabaseContract.Sms.KEY_ADDRESS)
    );
    values.put(
        DatabaseContract.Sms.KEY_SIMILAR_TO,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_SIMILAR_TO))
    );
    values.put(
        DatabaseContract.Sms.KEY_SIM_SCORE,
        Double.parseDouble(sms.get(DatabaseContract.Sms.KEY_SIM_SCORE))
    );
    values.put(
        DatabaseContract.Sms.KEY_CATEGORY_ID,
        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_CATEGORY_ID))
    );
    String providerId = sms.get(DatabaseContract.Sms.KEY_PROVIDER_ID);
    if (providerId != null && !providerId.isEmpty()) {
      values.put(DatabaseContract.Sms.KEY_PROVIDER_ID, Long.parseLong(providerId));
    }

    return values;
  }
}
