package in.rahulja.groupingmessages;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseContract.Category.CREATE_TABLE);
        // Create insert entries
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
        db.execSQL(DatabaseContract.Category.DELETE_TABLE);
        onCreate(db);
    }
}
