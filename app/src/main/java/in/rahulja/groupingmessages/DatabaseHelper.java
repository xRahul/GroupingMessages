package in.rahulja.groupingmessages;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;


class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase db) {
        createConfigTable(db);
        createCategoryTable(db);
        createSmsTable(db);
        createTriggers(db);
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
        deleteAllTables(db);
        onCreate(db);
    }

    private void deleteAllTables(SQLiteDatabase db) {
        db.execSQL(DatabaseContract.Sms.DELETE_TABLE);
        db.execSQL(DatabaseContract.Category.DELETE_TABLE);
        db.execSQL(DatabaseContract.Config.DELETE_TABLE);
    }
}
