package in.rahulja.groupingmessages;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DatabaseBridge {

    private static final String LAST_SMS_TIME_CONFIG = "lastSmsTime";
    private static final String SMS_COUNT = "sms_count";
    private static final String SMS_URI_INBOX = "content://sms/inbox";
    private static final String DOUBLE_EQUALS_QUESTION = " == ?";
    private static SQLiteDatabase db;


    private DatabaseBridge() {
        //empty constructor
    }

    private static void initializeDb(Context context) {
        if (db == null) {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            db = dbHelper.getWritableDatabase();
        }
    }

    private static void unInitializeDb() {
        if (db.isOpen()) {
            db.close();
            db = null;
        }
    }

    static List<Map<String, String>> getAllCategories(Context context) {

        initializeDb(context);

        List<Map<String, String>> categories = new ArrayList<>();

        Cursor cursor = db.query(
                DatabaseContract.Category.TABLE_NAME,           // The table to query
                DatabaseContract.Category.KEY_ARRAY,            // The columns to return
                null,                                           // The columns for the WHERE clause
                null,                                           // The values for the WHERE clause
                null,                                           // don't group the rows
                null,                                           // don't filter by row groups
                DatabaseContract.Category.DEFAULT_SORT_ORDER    // The sort order
        );

        while (cursor.moveToNext()) {
            Map<String, String> categoryTemp = new HashMap<>();
            final long categoryId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category._ID));
            final String categoryName = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_NAME));
            final String categoryColor = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_COLOR));
            final int categoryVisibility = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_VISIBILITY));

            categoryTemp.put(DatabaseContract.Category._ID, String.valueOf(categoryId));
            categoryTemp.put(DatabaseContract.Category.KEY_NAME, categoryName);
            categoryTemp.put(DatabaseContract.Category.KEY_COLOR, categoryColor);
            categoryTemp.put(DatabaseContract.Category.KEY_VISIBILITY, String.valueOf(categoryVisibility));
            categories.add(categoryTemp);
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }

        return categories;
    }


    static List<Map<String, String>> getLatestSmsFromInbox(Context context) {

        long lastSmsTime = Long.parseLong(DatabaseBridge.getConfig(context, LAST_SMS_TIME_CONFIG));

        List<Map<String, String>> latestSms = new ArrayList<>();

        Uri uri = Uri.parse(SMS_URI_INBOX);

        String[] projection = new String[]{
                "_id", "date", "person", "read", "seen", "subject", "body", "address"
        };

        StringBuilder searchString = new StringBuilder();
        searchString.append("date > ").append(String.valueOf(lastSmsTime));

        Log.d("GM/searchString", searchString.toString());
        Cursor cur = context.getContentResolver()
                .query(uri, projection, searchString.toString(), null, "date asc");

        if (cur != null && cur.moveToFirst()) {
            int indexDate = cur.getColumnIndex("date");
            int indexPerson = cur.getColumnIndex("person");
            int indexRead = cur.getColumnIndex("read");
            int indexSeen = cur.getColumnIndex("seen");
            int indexSubject = cur.getColumnIndex("subject");
            int indexBody = cur.getColumnIndex("body");
            int indexAddress = cur.getColumnIndex("address");

            do {
                long longDate = cur.getLong(indexDate);
                long longPerson = cur.getLong(indexPerson);
                long longRead = cur.getLong(indexRead);
                long longSeen = cur.getLong(indexSeen);
                String strSubject = cur.getString(indexSubject);
                String strBody = cur.getString(indexBody);
                String strAddress = cur.getString(indexAddress);

                Map<String, String> smsTemp = new HashMap<>();

                smsTemp.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(longDate));
                smsTemp.put(DatabaseContract.Sms.KEY_PERSON, String.valueOf(longPerson));
                smsTemp.put(DatabaseContract.Sms.KEY_READ, String.valueOf(longRead));
                smsTemp.put(DatabaseContract.Sms.KEY_SEEN, String.valueOf(longSeen));
                smsTemp.put(DatabaseContract.Sms.KEY_SUBJECT, strSubject);
                smsTemp.put(DatabaseContract.Sms.KEY_BODY, strBody);
                smsTemp.put(DatabaseContract.Sms.KEY_ADDRESS, strAddress);

                latestSms.add(smsTemp);

                Log.d("GM/temp", smsTemp.toString());

            } while (cur.moveToNext());

            if (!cur.isClosed()) {
                cur.close();
            }
        }
        return latestSms;
    }

    private static String getConfig(Context context, String configName) {

        initializeDb(context);

        String configValue = null;

        String selection = DatabaseContract.Config.KEY_NAME + " = ?";
        String[] selectionArgs = {configName};

        Cursor cursor = db.query(
                DatabaseContract.Config.TABLE_NAME,             // The table to query
                DatabaseContract.Config.KEY_ARRAY,              // The columns to return
                selection,                                      // The columns for the WHERE clause
                selectionArgs,                                  // The values for the WHERE clause
                null,                                           // don't group the rows
                null,                                           // don't filter by row groups
                DatabaseContract.Config.DEFAULT_SORT_ORDER      // The sort order
        );

        if (cursor.moveToNext()) {
            configValue = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Config.KEY_VALUE));
        }

        cursor.close();

        return configValue;
    }

    static List<Map<String, String>> getAllSms(Context context) {

        initializeDb(context);

        List<Map<String, String>> smsList = new ArrayList<>();

        Cursor cursor = db.query(
                DatabaseContract.Sms.TABLE_NAME,            // The table to query
                DatabaseContract.Sms.KEY_ARRAY,             // The columns to return
                null,                                       // The columns for the WHERE clause
                null,                                       // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                DatabaseContract.Sms.DEFAULT_SORT_ORDER     // The sort order
        );

        while (cursor.moveToNext()) {
            final long smsId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms._ID));
            final long smsDate = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_DATE));
            final long smsPerson = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_PERSON));
            final long smsRead = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ));
            final long smsSeen = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SEEN));
            final String smsSubject = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SUBJECT));
            final String smsAddress = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_ADDRESS));
            final String smsBody = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_BODY));
            final long smsSimilarTo = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIMILAR_TO));
            final float smsSimScore = cursor.getFloat(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIM_SCORE));
            final long smsCategoryId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID));

            Map<String, String> tempSms = new HashMap<>();

            tempSms.put(DatabaseContract.Sms._ID, String.valueOf(smsId));
            tempSms.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(smsDate));
            tempSms.put(DatabaseContract.Sms.KEY_PERSON, String.valueOf(smsPerson));
            tempSms.put(DatabaseContract.Sms.KEY_READ, String.valueOf(smsRead));
            tempSms.put(DatabaseContract.Sms.KEY_SEEN, String.valueOf(smsSeen));
            tempSms.put(DatabaseContract.Sms.KEY_SUBJECT, smsSubject);
            tempSms.put(DatabaseContract.Sms.KEY_ADDRESS, smsAddress);
            tempSms.put(DatabaseContract.Sms.KEY_BODY, smsBody);
            tempSms.put(DatabaseContract.Sms.KEY_SIMILAR_TO, String.valueOf(smsSimilarTo));
            tempSms.put(DatabaseContract.Sms.KEY_SIM_SCORE, String.valueOf(smsSimScore));
            tempSms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(smsCategoryId));

            smsList.add(tempSms);
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }

        return smsList;
    }

    static List<Map<String, String>> getFilteredSms(Context context, String selection, String[] selectionArgs) {

        initializeDb(context);

        List<Map<String, String>> smsList = new ArrayList<>();

        Cursor cursor = db.query(
                DatabaseContract.Sms.TABLE_NAME,            // The table to query
                DatabaseContract.Sms.KEY_ARRAY,             // The columns to return
                selection,                                  // The columns for the WHERE clause
                selectionArgs,                              // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                DatabaseContract.Sms.DEFAULT_SORT_ORDER     // The sort order
        );

        while (cursor.moveToNext()) {
            final long smsId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms._ID));
            final long smsDate = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_DATE));
            final long smsPerson = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_PERSON));
            final long smsRead = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ));
            final long smsSeen = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SEEN));
            final String smsSubject = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SUBJECT));
            final String smsAddress = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_ADDRESS));
            final String smsBody = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_BODY));
            final long smsSimilarTo = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIMILAR_TO));
            final float smsSimScore = cursor.getFloat(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIM_SCORE));
            final long smsCategoryId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID));

            Map<String, String> tempSms = new HashMap<>();

            tempSms.put(DatabaseContract.Sms._ID, String.valueOf(smsId));
            tempSms.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(smsDate));
            tempSms.put(DatabaseContract.Sms.KEY_PERSON, String.valueOf(smsPerson));
            tempSms.put(DatabaseContract.Sms.KEY_READ, String.valueOf(smsRead));
            tempSms.put(DatabaseContract.Sms.KEY_SEEN, String.valueOf(smsSeen));
            tempSms.put(DatabaseContract.Sms.KEY_SUBJECT, smsSubject);
            tempSms.put(DatabaseContract.Sms.KEY_ADDRESS, smsAddress);
            tempSms.put(DatabaseContract.Sms.KEY_BODY, smsBody);
            tempSms.put(DatabaseContract.Sms.KEY_SIMILAR_TO, String.valueOf(smsSimilarTo));
            tempSms.put(DatabaseContract.Sms.KEY_SIM_SCORE, String.valueOf(smsSimScore));
            tempSms.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(smsCategoryId));

            smsList.add(tempSms);
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }

        return smsList;
    }


    static long storeTrainedInboxSms(Context context, List<Map<String, String>> trainedInboxSms) {

        initializeDb(context);
        long lastSmsTime = Long.parseLong(DatabaseBridge.getConfig(context, LAST_SMS_TIME_CONFIG));
        long numSmsStored = 0;

        for (Map<String, String> trainedSmsMap : trainedInboxSms) {

            long longDate = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_DATE));
            long longPerson = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_PERSON));
            long longRead = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_READ));
            long longSeen = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_SEEN));
            String strSubject = trainedSmsMap.get(DatabaseContract.Sms.KEY_SUBJECT);
            String strAddress = trainedSmsMap.get(DatabaseContract.Sms.KEY_ADDRESS);
            String strBody = trainedSmsMap.get(DatabaseContract.Sms.KEY_BODY);
            long longCategoryId = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
            long longSimilarTo = Long.parseLong(trainedSmsMap.get(DatabaseContract.Sms.KEY_SIMILAR_TO));
            double longSimScore = Double.parseDouble(trainedSmsMap.get(DatabaseContract.Sms.KEY_SIM_SCORE));

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.Sms.KEY_DATE, longDate);
            values.put(DatabaseContract.Sms.KEY_PERSON, longPerson);
            values.put(DatabaseContract.Sms.KEY_READ, longRead);
            values.put(DatabaseContract.Sms.KEY_SEEN, longSeen);
            values.put(DatabaseContract.Sms.KEY_SUBJECT, strSubject);
            values.put(DatabaseContract.Sms.KEY_BODY, strBody);
            values.put(DatabaseContract.Sms.KEY_ADDRESS, strAddress);
            values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, longSimilarTo);
            values.put(DatabaseContract.Sms.KEY_SIM_SCORE, longSimScore);
            values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, longCategoryId);

            long addSmsRowId = db.insert(DatabaseContract.Sms.TABLE_NAME, null, values);

            if (addSmsRowId == -1) {
                Log.e("GM/insertInbox", values.toString());
            } else {
                numSmsStored += 1;
                lastSmsTime = longDate;
            }
        }

        DatabaseBridge.setConfig(context, LAST_SMS_TIME_CONFIG, String.valueOf(lastSmsTime));

        return numSmsStored;
    }

    private static Boolean setConfig(Context context, String key, String value) {

        String currentConfigValue = DatabaseBridge.getConfig(context, key);
        Boolean configAlreadyExist = true;

        if (currentConfigValue == null) {
            configAlreadyExist = false;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Config.KEY_VALUE, value);

        // Which row to update, based on the title
        String selection = DatabaseContract.Config.KEY_NAME + " LIKE ?";
        String[] selectionArgs = {key};
        long resultId;

        if (configAlreadyExist) {
            resultId = db.update(
                    DatabaseContract.Config.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);
        } else {
            resultId = db.insert(DatabaseContract.Config.TABLE_NAME, null, values);
        }

        return resultId > 0;
    }

    static List<Map<String, String>> getCategoryIdsWithSmsCount(Context context) {

        initializeDb(context);

        String[] projection = {
                DatabaseContract.Sms.KEY_CATEGORY_ID,
                DatabaseContract.Sms.KEY_READ,
                "COUNT(" + DatabaseContract.Sms.KEY_CATEGORY_ID + ") as " + SMS_COUNT
        };

        Cursor cursor = db.query(
                DatabaseContract.Sms.TABLE_NAME,                // The table to query
                projection,                                     // The columns to return
                null,                                           // The columns for the WHERE clause
                null,                                           // The values for the WHERE clause
                DatabaseContract.Sms.KEY_CATEGORY_ID
                        + ", "
                        + DatabaseContract.Sms.KEY_READ,        // don't group the rows
                null,                                           // don't filter by row groups
                DatabaseContract.Sms.DEFAULT_SORT_ORDER         // The sort order
        );

        List<Map<String, String>> categoryIdsWithSmsCount = new ArrayList<>();

        while (cursor.moveToNext()) {
            final long categoryId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID));
            final long readKey = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ));
            final long smsCount = cursor.getLong(
                    cursor.getColumnIndexOrThrow(SMS_COUNT));

            Map<String, String> categorySmsCount = new HashMap<>();

            categorySmsCount.put(DatabaseContract.Sms.KEY_CATEGORY_ID, String.valueOf(categoryId));
            categorySmsCount.put(DatabaseContract.Sms.KEY_READ, String.valueOf(readKey));
            categorySmsCount.put(SMS_COUNT, String.valueOf(smsCount));

            categoryIdsWithSmsCount.add(categorySmsCount);
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }
        return categoryIdsWithSmsCount;
    }

    static Boolean addCategory(Context context, Map<String, String> category) {

        initializeDb(context);

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

        long addCatRowId = db.insert(DatabaseContract.Category.TABLE_NAME, null, values);

        return addCatRowId != -1;
    }

    static Boolean updateSmsData(Context context, Map<String, String> sms) {

        initializeDb(context);

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Sms.KEY_DATE, sms.get(DatabaseContract.Sms.KEY_DATE));
        values.put(DatabaseContract.Sms.KEY_PERSON, sms.get(DatabaseContract.Sms.KEY_PERSON));
        values.put(DatabaseContract.Sms.KEY_READ, sms.get(DatabaseContract.Sms.KEY_READ));
        values.put(DatabaseContract.Sms.KEY_SEEN, sms.get(DatabaseContract.Sms.KEY_SEEN));
        values.put(DatabaseContract.Sms.KEY_SUBJECT, sms.get(DatabaseContract.Sms.KEY_SUBJECT));
        values.put(DatabaseContract.Sms.KEY_ADDRESS, sms.get(DatabaseContract.Sms.KEY_ADDRESS));
        values.put(DatabaseContract.Sms.KEY_BODY, sms.get(DatabaseContract.Sms.KEY_BODY));
        values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, sms.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
        values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, sms.get(DatabaseContract.Sms.KEY_SIMILAR_TO));
        values.put(DatabaseContract.Sms.KEY_SIM_SCORE, sms.get(DatabaseContract.Sms.KEY_SIM_SCORE));

        // Which row to update, based on the title
        String selection = DatabaseContract.Sms._ID + " = ?";
        String[] selectionArgs = {String.valueOf(sms.get(DatabaseContract.Sms._ID))};

        int count = db.update(
                DatabaseContract.Sms.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        return count > 0;
    }

    static long storeReTrainedSms(Context context, List<Map<String, String>> retrainedSmsList) {

        initializeDb(context);
        long numSmsUpdated = 0;

        for (Map<String, String> reTrainedSmsMap : retrainedSmsList) {

            long longId = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms._ID));
            long longDate = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_DATE));
            long longPerson = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_PERSON));
            long longRead = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_READ));
            long longSeen = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_SEEN));
            String strSubject = reTrainedSmsMap.get(DatabaseContract.Sms.KEY_SUBJECT);
            String strAddress = reTrainedSmsMap.get(DatabaseContract.Sms.KEY_ADDRESS);
            String strBody = reTrainedSmsMap.get(DatabaseContract.Sms.KEY_BODY);
            long longCategoryId = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_CATEGORY_ID));
            long longSimilarTo = Long.parseLong(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_SIMILAR_TO));
            double longSimScore = Double.parseDouble(reTrainedSmsMap.get(DatabaseContract.Sms.KEY_SIM_SCORE));

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.Sms.KEY_DATE, longDate);
            values.put(DatabaseContract.Sms.KEY_PERSON, longPerson);
            values.put(DatabaseContract.Sms.KEY_READ, longRead);
            values.put(DatabaseContract.Sms.KEY_SEEN, longSeen);
            values.put(DatabaseContract.Sms.KEY_SUBJECT, strSubject);
            values.put(DatabaseContract.Sms.KEY_BODY, strBody);
            values.put(DatabaseContract.Sms.KEY_ADDRESS, strAddress);
            values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, longSimilarTo);
            values.put(DatabaseContract.Sms.KEY_SIM_SCORE, longSimScore);
            values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, longCategoryId);

            String selection = DatabaseContract.Sms._ID + " = ?";
            String[] selectionArgs = {String.valueOf(longId)};

            long updateSmsRowId = db.update(
                    DatabaseContract.Sms.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
            );

            if (updateSmsRowId > 0) {
                numSmsUpdated += 1;
            }
        }
        return numSmsUpdated;
    }

    static Boolean deleteModel(Context context) {
        initializeDb(context);

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, 1);
        values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, 0);
        values.put(DatabaseContract.Sms.KEY_SIM_SCORE, 0.0);

        int count = db.update(
                DatabaseContract.Sms.TABLE_NAME,
                values,
                null,
                null);

        return count > 0;
    }

    static Boolean deleteCategories(Context context) {
        initializeDb(context);
        deleteModel(context);

        // Which row to update, based on the title
        String selection = DatabaseContract.Category._ID + " != ?";
        String[] selectionArgs = {String.valueOf(1)};

        int count = db.delete(
                DatabaseContract.Category.TABLE_NAME,
                selection,
                selectionArgs
        );
        return count > 0;
    }

    static Boolean deleteCategory(Context context, long categoryId) {

        initializeDb(context);
        deleteModelForCategory(context, categoryId);

        // Which row to update, based on the title
        String selection = DatabaseContract.Category._ID + DOUBLE_EQUALS_QUESTION;
        String[] selectionArgs = {String.valueOf(categoryId)};

        int count = db.delete(
                DatabaseContract.Category.TABLE_NAME,
                selection,
                selectionArgs
        );
        return count > 0;
    }

    private static Boolean deleteModelForCategory(Context context, long categoryId) {
        initializeDb(context);

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, 1);
        values.put(DatabaseContract.Sms.KEY_SIMILAR_TO, 0);
        values.put(DatabaseContract.Sms.KEY_SIM_SCORE, 0.0);

        // Which row to update, based on the title
        String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + DOUBLE_EQUALS_QUESTION;
        String[] selectionArgs = {String.valueOf(categoryId)};

        int count = db.update(
                DatabaseContract.Sms.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        return count > 0;
    }

    static Boolean updateCategory(Context context, Map<String, String> category) {

        initializeDb(context);

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

        // Which row to update, based on the title
        String selection = DatabaseContract.Category._ID + DOUBLE_EQUALS_QUESTION;
        String[] selectionArgs = {String.valueOf(category.get(DatabaseContract.Category._ID))};

        int count = db.update(
                DatabaseContract.Category.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );

        return count > 0;
    }

    static boolean importDB(Context context) {

        unInitializeDb();

        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                String backupDBPath = "GroupMessagingBackup"; // From SD directory.
                File backupDB = new File(sd, backupDBPath);
                File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);

                try (FileInputStream fis = new FileInputStream(backupDB)) {
                    try (FileOutputStream fos = new FileOutputStream(currentDB)) {
                        FileChannel src = fis.getChannel();
                        FileChannel dst = fos.getChannel();
                        dst.transferFrom(src, 0, src.size());
                        src.close();
                        dst.close();
                        fis.close();
                        fos.close();
                    }
                }
                initializeDb(context);
                return true;
            }
        } catch (Exception e) {
            Log.e("GM/importDb", e.toString());
        }

        initializeDb(context);
        return false;
    }

    static Boolean exportDB(Context context) {

        unInitializeDb();

        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                String backupDBPath = "GroupMessagingBackup";
                File currentDB = context.getDatabasePath(DatabaseContract.DATABASE_NAME);
                File backupDB = new File(sd, backupDBPath);

                try (FileInputStream fis = new FileInputStream(currentDB)) {
                    try (FileOutputStream fos = new FileOutputStream(backupDB)) {
                        FileChannel src = fis.getChannel();
                        FileChannel dst = fos.getChannel();
                        dst.transferFrom(src, 0, src.size());
                        src.close();
                        dst.close();
                        fis.close();
                        fos.close();
                    }
                }
                initializeDb(context);
                return true;
            }
        } catch (Exception e) {
            Log.e("GM/exportDb", e.toString());
        }
        initializeDb(context);
        return false;
    }

    static Boolean setSmsAsRead(Context context, String smsId) {

        initializeDb(context);

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Sms.KEY_READ, 1);


        // Which row to update, based on the title
        String selection = DatabaseContract.Sms._ID + " = ?";
        String[] selectionArgs = {smsId};

        int count = db.update(
                DatabaseContract.Sms.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        Log.d("GM/smsRead", "Count: " + String.valueOf(count));
        return count > 0;
    }

    static Boolean setAllCategorySmsAsRead(Context context, String categoryId) {
        initializeDb(context);

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Sms.KEY_READ, 1);


        // Which row to update, based on the title
        String selection = DatabaseContract.Sms.KEY_CATEGORY_ID + " = ?";
        String[] selectionArgs = {categoryId};

        int count = db.update(
                DatabaseContract.Sms.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        Log.d("GM/catSmsRead", "Count: " + String.valueOf(count));
        return count > 0;
    }

    static void deleteSmsOrChangeVisibility(Map<String, String> data) {

    }
}
