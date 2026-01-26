package in.rahulja.groupingmessages;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") class DatabaseBridge {

  private static final String LAST_SMS_TIME_CONFIG = "lastSmsTime";
  private static final String SMS_COUNT = "sms_count";
  private static final String EQUALS_QUESTION = " = ? ";
  private static final String GM_CURSOR = "GM/cursor";
  private static final String CURSOR_IS_NULL = "Cursor is null: ";
  private static final String GM_STORE_TRAINED_INBOX_SMS = "GM/storeTrainedInboxSms";
  private static final String BACKUP_DB_PATH =
      "GroupMessagingBackupV" + DatabaseContract.DATABASE_VERSION;
  private static SQLiteDatabase db;
  private static DatabaseHelper dbHelper;

  private DatabaseBridge() {
    //empty constructor
  }

  private static void initializeDb(Context context) {
    if (dbHelper == null) {
      dbHelper = DatabaseHelper.getInstance(context);
    }
    if (db == null || !db.isOpen()) {
      db = dbHelper.getWritableDatabase();
      Log.d("GM/getDb", "Initialized");
    }
  }

  private static void unInitializeDb() {
    if (db != null) {
      db = null;
      Log.d("GM/setDbNull", "Uninitialized");
    }
  }

  private static List<Map<String, String>> getFromConfigs(Context context, String selection,
      String[] selectArgs) {

    List<Map<String, String>> configs = new ArrayList<>();

    initializeDb(context);
    Cursor cursor = db.query(
        DatabaseContract.Config.TABLE_NAME,           // The table to query
        DatabaseContract.Config.KEY_ARRAY,            // The columns to return
        selection,                                      // The columns for the WHERE clause
        selectArgs,                                     // The values for the WHERE clause
        null,                                           // don't group the rows
        null,                                           // don't filter by row groups
        DatabaseContract.Config.DEFAULT_SORT_ORDER    // The sort order
    );

    if (cursor != null && cursor.moveToFirst()) {
      int indexId = cursor.getColumnIndexOrThrow(DatabaseContract.Config._ID);
      int indexName = cursor.getColumnIndexOrThrow(DatabaseContract.Config.KEY_NAME);
      int indexValue = cursor.getColumnIndexOrThrow(DatabaseContract.Config.KEY_VALUE);
      Map<String, String> configTemp;

      while (!cursor.isAfterLast()) {
        configTemp = new HashMap<>();

        final long configId = cursor.getLong(indexId);
        final String configName = cursor.getString(indexName);
        final String configValue = cursor.getString(indexValue);

        configTemp.put(DatabaseContract.Config._ID, String.valueOf(configId));
        configTemp.put(DatabaseContract.Config.KEY_NAME, configName);
        configTemp.put(DatabaseContract.Config.KEY_VALUE, configValue);
        configs.add(configTemp);
        cursor.moveToNext();
      }
    } else {
      Log.e(GM_CURSOR, CURSOR_IS_NULL + "getFromConfigs");
    }

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }

    unInitializeDb();

    return configs;
  }

  private static List<Map<String, String>> getFromCategories(Context context, String selection,
      String[] selectArgs) {

    List<Map<String, String>> categories = new ArrayList<>();

    initializeDb(context);
    Cursor cursor = db.query(
        DatabaseContract.Category.TABLE_NAME,           // The table to query
        DatabaseContract.Category.KEY_ARRAY,            // The columns to return
        selection,                                      // The columns for the WHERE clause
        selectArgs,                                     // The values for the WHERE clause
        null,                                           // don't group the rows
        null,                                           // don't filter by row groups
        DatabaseContract.Category.DEFAULT_SORT_ORDER    // The sort order
    );

    if (cursor != null && cursor.moveToFirst()) {
      int indexId = cursor.getColumnIndexOrThrow(DatabaseContract.Category._ID);
      int indexName = cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_NAME);
      int indexColor = cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_COLOR);
      int indexVisibility = cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_VISIBILITY);

      Map<String, String> categoryTemp;

      while (!cursor.isAfterLast()) {
        final long categoryId = cursor.getLong(indexId);
        final String categoryName = cursor.getString(indexName);
        final String categoryColor = cursor.getString(indexColor);
        final int categoryVisibility = cursor.getInt(indexVisibility);

        categoryTemp = new HashMap<>();

        categoryTemp.put(DatabaseContract.Category._ID, String.valueOf(categoryId));
        categoryTemp.put(DatabaseContract.Category.KEY_NAME, categoryName);
        categoryTemp.put(DatabaseContract.Category.KEY_COLOR, categoryColor);
        categoryTemp.put(DatabaseContract.Category.KEY_VISIBILITY,
            String.valueOf(categoryVisibility));
        categories.add(categoryTemp);
        cursor.moveToNext();
      }
    } else {
      Log.e(GM_CURSOR, CURSOR_IS_NULL + "getFromCategories");
    }

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }

    unInitializeDb();

    Log.i("GM/getFromCategories", String.valueOf(categories.size()));
    return categories;
  }

  private static List<Map<String, String>> getFromSms(Context context, String selection,
      String[] selectArgs) {

    List<Map<String, String>> smsList = new ArrayList<>();

    initializeDb(context);
    Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,            // The table to query
        DatabaseContract.Sms.KEY_ARRAY,             // The columns to return
        selection,                                  // The columns for the WHERE clause
        selectArgs,                                 // The values for the WHERE clause
        null,                                       // don't group the rows
        null,                                       // don't filter by row groups
        DatabaseContract.Sms.DEFAULT_SORT_ORDER     // The sort order
    );

    if (cursor != null && cursor.moveToFirst()) {
      int indexId = cursor.getColumnIndexOrThrow(DatabaseContract.Sms._ID);
      int indexDate = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_DATE);
      int indexPerson = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_PERSON);
      int indexRead = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ);
      int indexSeen = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SEEN);
      int indexSubject = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SUBJECT);
      int indexAddress = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_ADDRESS);
      int indexBody = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_BODY);
      int indexCleanedSms = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CLEANED_SMS);
      int indexVisibility = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_VISIBILITY);
      int indexSenderType = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SENDER_TYPE);
      int indexSimilarTo = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIMILAR_TO);
      int indexSimScore = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIM_SCORE);
      int indexCategoryId = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID);

      Map<String, String> tempSms;

      while (!cursor.isAfterLast()) {
        final long smsId = cursor.getLong(indexId);
        final long smsDate = cursor.getLong(indexDate);
        final long smsPerson = cursor.getLong(indexPerson);
        final long smsRead = cursor.getLong(indexRead);
        final long smsSeen = cursor.getLong(indexSeen);
        final String smsSubject = cursor.getString(indexSubject);
        final String smsAddress = cursor.getString(indexAddress);
        final String smsBody = cursor.getString(indexBody);
        final String smsCleanedSms = cursor.getString(indexCleanedSms);
        final long smsVisibility = cursor.getLong(indexVisibility);
        final long smsSenderType = cursor.getLong(indexSenderType);
        final long smsSimilarTo = cursor.getLong(indexSimilarTo);
        final float smsSimScore = cursor.getFloat(indexSimScore);
        final long smsCategoryId = cursor.getLong(indexCategoryId);

        tempSms = new HashMap<>();

        tempSms.put(DatabaseContract.Sms._ID, String.valueOf(smsId));
        tempSms.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(smsDate));
        tempSms.put(DatabaseContract.Sms.KEY_PERSON, String.valueOf(smsPerson));
        tempSms.put(DatabaseContract.Sms.KEY_READ, String.valueOf(smsRead));
        tempSms.put(DatabaseContract.Sms.KEY_SEEN, String.valueOf(smsSeen));
        tempSms.put(DatabaseContract.Sms.KEY_SUBJECT, smsSubject);
        tempSms.put(DatabaseContract.Sms.KEY_ADDRESS, smsAddress);
        tempSms.put(DatabaseContract.Sms.KEY_BODY, smsBody);
        tempSms.put(DatabaseContract.Sms.KEY_CLEANED_SMS, smsCleanedSms);
        tempSms.put(DatabaseContract.Sms.KEY_VISIBILITY, String.valueOf(smsVisibility));
        tempSms.put(DatabaseContract.Sms.KEY_SENDER_TYPE, String.valueOf(smsSenderType));
        tempSms.put(DatabaseContract.Sms.KEY_SIMILAR_TO, String.valueOf(smsSimilarTo));
        tempSms.put(DatabaseContract.Sms.KEY_SIM_SCORE, String.valueOf(smsSimScore));
        tempSms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(smsCategoryId));

        smsList.add(tempSms);
        cursor.moveToNext();
      }
    } else {
      Log.e(GM_CURSOR, CURSOR_IS_NULL + "getFromSms: " + Arrays.toString(
          Thread.currentThread().getStackTrace()));
    }

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }

    unInitializeDb();

    return smsList;
  }

  private static ContentValues getContentValuesFromSmsMap(Map<String, String> sms) {

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

    return values;
  }

  private static ContentValues getContentValuesFromCategoryMap(Map<String, String> category) {
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

  private static long insertIntoSms(SQLiteDatabase db, Map<String, String> sms) {

    ContentValues values = getContentValuesFromSmsMap(sms);
    long insertResult = -1;
    try {
      insertResult = db.insert(DatabaseContract.Sms.TABLE_NAME, null, values);
    } catch (Exception e) {
      Log.e("GM/insertIntoSms", "Error occurred while inserting sms", e);
    }
    return insertResult;
  }

  private static long insertIntoCategory(Context context, Map<String, String> category) {

    ContentValues values = getContentValuesFromCategoryMap(category);

    initializeDb(context);
    long insertResult = db.insert(DatabaseContract.Category.TABLE_NAME, null, values);

    unInitializeDb();

    return insertResult;
  }

  private static long updateInSms(Context context, Map<String, String> sms) {

    ContentValues values = getContentValuesFromSmsMap(sms);

    String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(Long.parseLong(sms.get(DatabaseContract.Sms._ID))) };

    initializeDb(context);
    long updateResult = db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );

    unInitializeDb();

    return updateResult;
  }

  private static void updateInSmsBySmsIdAndValues(Context context, long smsId,
      ContentValues values) {

    // Which row to update, based on the id
    String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(smsId) };

    initializeDb(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );

    unInitializeDb();
  }

  private static void updateInSmsByCategoryIdAndValues(Context context, long categoryId,
      ContentValues values) {

    // Which row to update, based on the id
    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    initializeDb(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );

    unInitializeDb();
  }

  private static long updateInCategory(Context context, Map<String, String> category) {

    ContentValues values = getContentValuesFromCategoryMap(category);

    // Which row to update, based on the title
    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(category.get(DatabaseContract.Category._ID)) };

    initializeDb(context);
    long updateResult = db.update(
        DatabaseContract.Category.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );

    unInitializeDb();

    return updateResult;
  }

  @Nullable
  public static String getConfig(Context context, String configName) {

    String selection = DatabaseContract.Config.KEY_NAME + EQUALS_QUESTION;
    String[] selectionArgs = { configName };

    List<Map<String, String>> configs = getFromConfigs(context, selection, selectionArgs);

    if (!configs.isEmpty()) {
      return configs.get(0).get(DatabaseContract.Config.KEY_VALUE);
    }

    return null;
  }

  public static List<Map<String, String>> getAllSms(Context context) {
    return getFromSms(context, null, null);
  }

  public static List<Map<String, String>> getSelfTrainedSms(Context context) {

    String selection = DatabaseContract.Sms._ID +
        " = " +
        DatabaseContract.Sms.KEY_SIMILAR_TO +
        " AND " +
        DatabaseContract.Sms.KEY_SIM_SCORE +
        EQUALS_QUESTION;

    String[] selectionArgs = { String.valueOf(1.0) };

    Log.i("GM/DatabaseBridge", "getSelfTrainedSms, selection= " + selection);
    return getFromSms(context, selection, selectionArgs);
  }

  public static List<Map<String, String>> getVisibleSmsFromCategory(Context context,
      long categoryId) {

    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION
        + " AND " + DatabaseContract.Sms.KEY_VISIBILITY + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId), String.valueOf(1) };

    return getFromSms(context, selection, selectionArgs);
  }

  public static long storeTrainedInboxSms(Context context,
      List<Map<String, String>> trainedInboxSms) {

    long lastSmsTime = Long.parseLong(DatabaseBridge.getConfig(context, LAST_SMS_TIME_CONFIG));
    Log.i(GM_STORE_TRAINED_INBOX_SMS, "before lastSmsTime: " + lastSmsTime);
    initializeDb(context);

    long numSmsStored = 0;
    long numSmsError = 0;
    long tempLastSmsTime = lastSmsTime;

    SQLiteDatabase localDb = db;
    localDb.beginTransaction();
    try {
      for (Map<String, String> trainedSmsMap : trainedInboxSms) {

        long longDate = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_DATE));
        long insertResult = insertIntoSms(localDb, trainedSmsMap);

        if (insertResult != -1) {
          numSmsStored += 1;
          tempLastSmsTime = longDate;
        } else {
          numSmsError += 1;
          Log.e(
              "GM/insertSms",
              "Some error occured while inserting sms- " + trainedSmsMap.toString()
          );
        }
      }
      localDb.setTransactionSuccessful();
    } finally {
      localDb.endTransaction();
      unInitializeDb();
    }

    if (tempLastSmsTime != lastSmsTime) {
      DatabaseBridge.setConfig(context, LAST_SMS_TIME_CONFIG, String.valueOf(tempLastSmsTime));
    }

    Log.i(GM_STORE_TRAINED_INBOX_SMS, "after lastSmsTime: " + tempLastSmsTime);
    Log.i(GM_STORE_TRAINED_INBOX_SMS, "Number of sms inserted successfully: " + numSmsStored);

    if (numSmsError > 0) {
      Log.e(GM_STORE_TRAINED_INBOX_SMS, "Number of sms failed to insert: " + numSmsError);
    }

    return numSmsStored;
  }

  private static void setConfig(Context context, @SuppressWarnings("SameParameterValue") String key,
      String value) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Config.KEY_NAME, key);
    values.put(DatabaseContract.Config.KEY_VALUE, value);
    Log.i("GM/setConfig", "Values: " + values.toString());

    initializeDb(context);
    long resultId = db.insertWithOnConflict(
        DatabaseContract.Config.TABLE_NAME,
        DatabaseContract.Config._ID,
        values,
        SQLiteDatabase.CONFLICT_REPLACE
    );

    unInitializeDb();

    Log.i("GM/setConfig", "Result Id: " + resultId);
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

    initializeDb(context);
    Cursor cursor = db.query(
        DatabaseContract.Sms.TABLE_NAME,                // The table to query
        projection,                                     // The columns to return
        selection,                                      // The columns for the WHERE clause
        selectionArgs,                                  // The values for the WHERE clause
        DatabaseContract.Sms.KEY_CATEGORY_ID + ", "
            + DatabaseContract.Sms.KEY_READ,        // don't group the rows
        null,                                           // don't filter by row groups
        DatabaseContract.Sms.DEFAULT_SORT_ORDER         // The sort order
    );

    if (cursor != null && cursor.moveToFirst()) {
      int indexCategoryId = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID);
      int indexRead = cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ);
      int indexSmsCount = cursor.getColumnIndexOrThrow(SMS_COUNT);

      Map<String, String> categorySmsCount;

      while (!cursor.isAfterLast()) {
        final long categoryId = cursor.getLong(indexCategoryId);
        final long readKey = cursor.getLong(indexRead);
        final long smsCount = cursor.getLong(indexSmsCount);

        categorySmsCount = new HashMap<>();

        categorySmsCount.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
        categorySmsCount.put(DatabaseContract.Sms.KEY_READ, String.valueOf(readKey));
        categorySmsCount.put(SMS_COUNT, String.valueOf(smsCount));

        categoryIdsWithSmsCount.add(categorySmsCount);
        cursor.moveToNext();
      }
    } else {
      Log.e(GM_CURSOR, CURSOR_IS_NULL + "getCategoryIdsWithSmsCount");
    }

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }

    unInitializeDb();

    Log.i("GM/getCatsSmsCount", categoryIdsWithSmsCount.toString());
    return categoryIdsWithSmsCount;
  }

  @NonNull
  public static Boolean addCategory(Context context, Map<String, String> category) {
    return insertIntoCategory(context, category) != -1;
  }

  public static void updateSmsData(Context context, Map<String, String> sms) {
    updateInSms(context, sms);
  }

  public static long storeReTrainedSms(Context context,
      List<Map<String, String>> retrainedSmsList) {

    long numSmsUpdated = 0;
    for (Map<String, String> reTrainedSmsMap : retrainedSmsList) {

      long updateSmsRowId = updateInSms(context, reTrainedSmsMap);

      if (updateSmsRowId > 0) {
        numSmsUpdated += 1;
      }
    }
    return numSmsUpdated;
  }

  public static void deleteModel(Context context) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, 1);
    values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, 0);
    values.put(DatabaseContract.Sms.KEY_SIM_SCORE, 0.0);

    initializeDb(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        null,
        null);

    unInitializeDb();
  }

  public static void deleteCategories(Context context) {
    deleteModel(context);

    // Which row to update, based on the title
    String selection = DatabaseContract.Category._ID + " != ?";
    String[] selectionArgs = { String.valueOf(1) };

    initializeDb(context);
    db.delete(
        DatabaseContract.Category.TABLE_NAME,
        selection,
        selectionArgs
    );

    unInitializeDb();
  }

  public static void deleteCategory(Context context, long categoryId) {

    deleteModelForCategory(context, categoryId);

    // Which row to update, based on the title
    String selection = DatabaseContract.Category._ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category.KEY_VISIBILITY, 0);

    initializeDb(context);
    db.update(
        DatabaseContract.Category.TABLE_NAME,
        values,
        selection,
        selectionArgs
    );

    unInitializeDb();
  }

  private static void deleteModelForCategory(Context context, long categoryId) {

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, 1);
    values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, 0);
    values.put(DatabaseContract.Sms.KEY_SIM_SCORE, 0.0);

    // Which row to update, based on the title
    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(categoryId) };

    initializeDb(context);
    db.update(
        DatabaseContract.Sms.TABLE_NAME,
        values,
        selection,
        selectionArgs);

    unInitializeDb();
  }

  public static Boolean updateCategory(Context context, Map<String, String> category) {
    return updateInCategory(context, category) > 0;
  }

  public static void importDB(Context context) {

    unInitializeDb();

    try {
      File sd = context.getExternalFilesDir(null);
      if (sd != null && sd.canWrite()) {
        File backupDB = new File(sd, BACKUP_DB_PATH);
        File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);

        try (FileInputStream fis = new FileInputStream(backupDB)) {
          try (FileOutputStream fos = new FileOutputStream(currentDB)) {
            FileChannel src = fis.getChannel();
            FileChannel dst = fos.getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
          }
        }
        initializeDb(context);
        return;
      }
    } catch (Exception e) {
      Log.e("GM/importDb", e.toString());
    }

    initializeDb(context);
  }

  public static void exportDB(Context context) {

    unInitializeDb();

    try {
      File sd = context.getExternalFilesDir(null);

      if (sd != null && sd.canWrite()) {
        File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);
        File backupDB = new File(sd, BACKUP_DB_PATH);

        try (FileInputStream fis = new FileInputStream(currentDB)) {
          try (FileOutputStream fos = new FileOutputStream(backupDB)) {
            FileChannel src = fis.getChannel();
            FileChannel dst = fos.getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
          }
        }
        initializeDb(context);
        return;
      }
    } catch (Exception e) {
      Log.e("GM/exportDb", e.toString());
    }
    initializeDb(context);
  }

  public static void setSmsAsRead(Context context, String smsId) {

    initializeDb(context);

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_READ, 1);

    updateInSmsBySmsIdAndValues(context, Long.parseLong(smsId), values);
  }

  public static void setAllCategorySmsAsRead(Context context, String categoryId) {
    initializeDb(context);

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_READ, 1);

    updateInSmsByCategoryIdAndValues(context, Long.parseLong(categoryId), values);
  }

  public static void deleteSmsByMap(Context context, Map<String, String> data) {

    if (data.get(DatabaseContract.Sms._ID).equals(data.get(DatabaseContract.Sms.KEY_SIMILAR_TO))) {
      hideSms(context, data);
    } else {
      deleteSms(context, data);
    }
  }

  public static void deleteAllSmsOfCategoryById(Context context, long categoryId) {

    // delete untrained sms first
    deleteSmsByCategoryId(context, categoryId);

    // hide remaining trained sms
    hideSmsByCategoryId(context, categoryId);
  }

  private static void deleteSmsByCategoryId(Context context, long categoryId) {

    // Which row to update, based on the title
    String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + EQUALS_QUESTION
        + " and "
        + DatabaseContract.Sms._ID + " <> " + DatabaseContract.Sms.KEY_SIMILAR_TO;
    String[] selectionArgs = { String.valueOf(categoryId) };

    initializeDb(context);
    db.delete(
        DatabaseContract.Sms.TABLE_NAME,
        selection,
        selectionArgs
    );

    unInitializeDb();
  }

  private static void hideSmsByCategoryId(Context context, long categoryId) {
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_VISIBILITY, 0);

    updateInSmsByCategoryIdAndValues(context, categoryId, values);
  }

  private static void deleteSms(Context context, Map<String, String> data) {

    // Which row to update, based on the title
    String selection = DatabaseContract.Sms._ID + EQUALS_QUESTION;
    String[] selectionArgs = { data.get(DatabaseContract.Sms._ID) };

    initializeDb(context);
    db.delete(
        DatabaseContract.Sms.TABLE_NAME,
        selection,
        selectionArgs
    );

    unInitializeDb();
  }

  private static void hideSms(Context context, Map<String, String> data) {
    initializeDb(context);

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_VISIBILITY, 0);
    long smsId = Long.parseLong(data.get(DatabaseContract.Sms._ID));

    updateInSmsBySmsIdAndValues(context, smsId, values);
  }

  public static List<Map<String, String>> getAllVisibleCategories(Context context) {

    String selection = DatabaseContract.Category.KEY_VISIBILITY + EQUALS_QUESTION;
    String[] selectionArgs = { String.valueOf(1) };

    Log.i("GM/getVisibleCategories", "Getting visible categories");
    return getFromCategories(context, selection, selectionArgs);
  }
}
