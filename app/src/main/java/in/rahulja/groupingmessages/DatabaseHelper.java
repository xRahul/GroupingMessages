package in.rahulja.groupingmessages;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;

@SuppressWarnings("WeakerAccess") public final class DatabaseHelper extends SQLiteOpenHelper {

  private static final String[] SMS_INDEXES_V3 = {
      "CREATE INDEX IF NOT EXISTS idx_sms_category ON "
          + DatabaseContract.Sms.TABLE_NAME + "(" + DatabaseContract.Sms.KEY_CATEGORY_ID + ")",
      "CREATE INDEX IF NOT EXISTS idx_sms_date ON "
          + DatabaseContract.Sms.TABLE_NAME + "(" + DatabaseContract.Sms.KEY_DATE + ")",
      "CREATE INDEX IF NOT EXISTS idx_sms_visibility ON "
          + DatabaseContract.Sms.TABLE_NAME + "(" + DatabaseContract.Sms.KEY_VISIBILITY + ")",
  };

  private static final String[] SMS_INDEXES_V4 = {
      "CREATE UNIQUE INDEX IF NOT EXISTS idx_sms_provider_id ON "
          + DatabaseContract.Sms.TABLE_NAME
          + "(" + DatabaseContract.Sms.KEY_PROVIDER_ID + ") WHERE "
          + DatabaseContract.Sms.KEY_PROVIDER_ID + " IS NOT NULL",
  };

  private static DatabaseHelper sInstance;

  private DatabaseHelper(Context context) {
    super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
  }

  public static synchronized DatabaseHelper getInstance(Context context) {

    // Use the application context, which will ensure that you
    // don't accidentally leak an Activity's context.
    // See this article for more information: http://bit.ly/6LRzfx
    if (sInstance == null) {
      sInstance = new DatabaseHelper(context.getApplicationContext());
    }
    return sInstance;
  }

  // Method is called during creation of the database
  @Override
  public void onCreate(SQLiteDatabase db) {
    createConfigTable(db);
    createCategoryTable(db);
    createSmsTable(db);
    createSmsIndexes(db);
    createTriggers(db);
  }

  private void createSmsIndexes(SQLiteDatabase db) {
    for (String createIndex : SMS_INDEXES_V3) {
      db.execSQL(createIndex);
    }
    for (String createIndex : SMS_INDEXES_V4) {
      db.execSQL(createIndex);
    }
  }

  private void createTriggers(SQLiteDatabase db) {
    db.execSQL(DatabaseContract.Config.UPDATE_AT_TRIGGER);
    db.execSQL(DatabaseContract.Category.UPDATE_AT_TRIGGER);
    db.execSQL(DatabaseContract.Sms.UPDATE_AT_TRIGGER);
  }

  private void createConfigTable(SQLiteDatabase db) {
    db.execSQL(DatabaseContract.Config.CREATE_TABLE);

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Config.KEY_NAME, "lastSmsTime");
    values.put(DatabaseContract.Config.KEY_VALUE, 0);

    long addCatRowId = db.insert(DatabaseContract.Config.TABLE_NAME, null, values);

    if (addCatRowId == -1) {
      Log.e("GM/createDb", "Error while adding lastSmsTime as zero");
    }
  }

  private void createSmsTable(SQLiteDatabase db) {
    db.execSQL(DatabaseContract.Sms.CREATE_TABLE);
  }

  private void createCategoryTable(SQLiteDatabase db) {
    db.execSQL(DatabaseContract.Category.CREATE_TABLE);

    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category.KEY_NAME, "Unknown");
    values.put(DatabaseContract.Category.KEY_COLOR, Color.WHITE);
    values.put(DatabaseContract.Category.KEY_VISIBILITY, 1);

    long addCatRowId = db.insert(DatabaseContract.Category.TABLE_NAME, null, values);

    if (addCatRowId == -1) {
      Log.e("GM/createDb", "Error while adding Unknown category");
    }
  }

  // Method is called during an upgrade of the database
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      for (String query : DatabaseContract.Sms.CHANGES_V2) {
        db.execSQL(query);
      }
    }
    if (oldVersion < 4) {
      db.execSQL("INSERT OR IGNORE INTO " + DatabaseContract.Category.TABLE_NAME
          + " (" + DatabaseContract.Category._ID + ", " + DatabaseContract.Category.KEY_NAME
          + ", " + DatabaseContract.Category.KEY_COLOR + ", "
          + DatabaseContract.Category.KEY_VISIBILITY + ") VALUES (1, 'Unknown', '-1', 1)");

      String legacyTable = DatabaseContract.Sms.TABLE_NAME + "_v4";
      String createNew = DatabaseContract.Sms.CREATE_TABLE.replace(
          "CREATE TABLE " + DatabaseContract.Sms.TABLE_NAME + " (",
          "CREATE TABLE " + legacyTable + " (");
      db.execSQL(createNew);
      db.execSQL("INSERT INTO " + legacyTable + " ("
          + DatabaseContract.Sms._ID + ", "
          + DatabaseContract.Sms.KEY_DATE + ", "
          + DatabaseContract.Sms.KEY_PERSON + ", "
          + DatabaseContract.Sms.KEY_READ + ", "
          + DatabaseContract.Sms.KEY_SEEN + ", "
          + DatabaseContract.Sms.KEY_SUBJECT + ", "
          + DatabaseContract.Sms.KEY_ADDRESS + ", "
          + DatabaseContract.Sms.KEY_BODY + ", "
          + DatabaseContract.Sms.KEY_CLEANED_SMS + ", "
          + DatabaseContract.Sms.KEY_VISIBILITY + ", "
          + DatabaseContract.Sms.KEY_SENDER_TYPE + ", "
          + DatabaseContract.Sms.KEY_CATEGORY_ID + ", "
          + DatabaseContract.Sms.KEY_SIMILAR_TO + ", "
          + DatabaseContract.Sms.KEY_SIM_SCORE + ", "
          + DatabaseContract.Sms.KEY_CREATED_AT + ", "
          + DatabaseContract.Sms.KEY_UPDATED_AT + ") SELECT "
          + DatabaseContract.Sms._ID + ", "
          + DatabaseContract.Sms.KEY_DATE + ", "
          + DatabaseContract.Sms.KEY_PERSON + ", "
          + DatabaseContract.Sms.KEY_READ + ", "
          + DatabaseContract.Sms.KEY_SEEN + ", "
          + DatabaseContract.Sms.KEY_SUBJECT + ", "
          + DatabaseContract.Sms.KEY_ADDRESS + ", "
          + DatabaseContract.Sms.KEY_BODY + ", "
          + DatabaseContract.Sms.KEY_CLEANED_SMS + ", "
          + DatabaseContract.Sms.KEY_VISIBILITY + ", "
          + DatabaseContract.Sms.KEY_SENDER_TYPE + ", "
          + DatabaseContract.Sms.KEY_CATEGORY_ID + ", "
          + DatabaseContract.Sms.KEY_SIMILAR_TO + ", "
          + DatabaseContract.Sms.KEY_SIM_SCORE + ", "
          + DatabaseContract.Sms.KEY_CREATED_AT + ", "
          + DatabaseContract.Sms.KEY_UPDATED_AT
          + " FROM " + DatabaseContract.Sms.TABLE_NAME);
      db.execSQL("DROP TABLE " + DatabaseContract.Sms.TABLE_NAME);
      db.execSQL("ALTER TABLE " + legacyTable + " RENAME TO "
          + DatabaseContract.Sms.TABLE_NAME);

      db.execSQL("UPDATE " + DatabaseContract.Sms.TABLE_NAME
          + " SET " + DatabaseContract.Sms.KEY_CATEGORY_ID + " = 1 WHERE "
          + DatabaseContract.Sms.KEY_CATEGORY_ID + " IS NULL OR "
          + DatabaseContract.Sms.KEY_CATEGORY_ID + " NOT IN (SELECT "
          + DatabaseContract.Category._ID + " FROM " + DatabaseContract.Category.TABLE_NAME + ")");
      db.execSQL("UPDATE " + DatabaseContract.Sms.TABLE_NAME
          + " SET " + DatabaseContract.Sms.KEY_SIMILAR_TO + " = 0 WHERE "
          + DatabaseContract.Sms.KEY_SIMILAR_TO + " IS NOT NULL AND "
          + DatabaseContract.Sms.KEY_SIMILAR_TO + " != 0 AND "
          + DatabaseContract.Sms.KEY_SIMILAR_TO + " NOT IN (SELECT "
          + DatabaseContract.Sms._ID + " FROM " + DatabaseContract.Sms.TABLE_NAME + ")");

      for (String createIndex : SMS_INDEXES_V3) {
        db.execSQL(createIndex);
      }
      for (String createIndex : SMS_INDEXES_V4) {
        db.execSQL(createIndex);
      }
      db.execSQL(DatabaseContract.Sms.UPDATE_AT_TRIGGER);
    }
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    super.onOpen(db);
    db.setForeignKeyConstraintsEnabled(true);
  }
}
