package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class MigrationTest {

  private static final String CREATE_CONFIG_V1 =
      "CREATE TABLE config (" +
          "_id INTEGER PRIMARY KEY," +
          "name TEXT not null unique," +
          "value TEXT," +
          "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
          "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

  private static final String CREATE_CATEGORY_V1 =
      "CREATE TABLE category (" +
          "_id INTEGER PRIMARY KEY," +
          "name TEXT," +
          "color TEXT," +
          "visibility INTEGER DEFAULT 1," +
          "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
          "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

  private static final String SMS_COMMON_COLUMNS =
          "_id INTEGER PRIMARY KEY," +
          "date INTEGER," +
          "person INTEGER," +
          "read INTEGER DEFAULT 0," +
          "seen INTEGER DEFAULT 0," +
          "subject TEXT," +
          "address TEXT," +
          "body TEXT,";

  private static final String SMS_V1_TAIL =
          "category_id INTEGER," +
          "similar_to INTEGER," +
          "similarity_score REAL DEFAULT 0.0," +
          "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
          "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
          "FOREIGN KEY (category_id) REFERENCES category(_id)," +
          "FOREIGN KEY (similar_to) REFERENCES sms(_id))";

  private static final String SMS_V2_EXTRA_COLUMNS =
          "cleaned_sms TEXT DEFAULT ''," +
          "visibility INTEGER DEFAULT 1," +
          "sender_type INTEGER";

  @Before
  public void resetHelperSingletonAndDbFile() throws Exception {
    Context ctx = RuntimeEnvironment.getApplication();
    Field instance = DatabaseHelper.class.getDeclaredField("sInstance");
    instance.setAccessible(true);
    instance.set(null, null);
    ctx.deleteDatabase(DatabaseContract.DATABASE_NAME);
  }

  @After
  public void releaseHelperSingleton() throws Exception {
    Field instance = DatabaseHelper.class.getDeclaredField("sInstance");
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  public void migrateFromV1AddsColumnsIndexesAndPreservesRows() {
    Context ctx = RuntimeEnvironment.getApplication();
    SQLiteDatabase db = ctx.openOrCreateDatabase(
        DatabaseContract.DATABASE_NAME, Context.MODE_PRIVATE, null);
    db.execSQL(CREATE_CONFIG_V1);
    db.execSQL(CREATE_CATEGORY_V1);
    db.execSQL("CREATE TABLE sms (" + SMS_COMMON_COLUMNS + SMS_V1_TAIL);
    seedLegacyRows(db);
    db.execSQL("PRAGMA user_version = 1");
    db.close();

    db = DatabaseHelper.getInstance(ctx).getWritableDatabase();
    assertUpgradedState(db);
    DatabaseHelper.getInstance(ctx).close();
  }

  @Test
  public void migrateFromV2CreatesIndexesAndPreservesRows() {
    Context ctx = RuntimeEnvironment.getApplication();
    SQLiteDatabase db = ctx.openOrCreateDatabase(
        DatabaseContract.DATABASE_NAME, Context.MODE_PRIVATE, null);
    db.execSQL(CREATE_CONFIG_V1);
    db.execSQL(CREATE_CATEGORY_V1);
    db.execSQL("CREATE TABLE sms (" + SMS_COMMON_COLUMNS
        + SMS_V2_EXTRA_COLUMNS + "," + SMS_V1_TAIL);
    seedLegacyRows(db);
    db.execSQL("PRAGMA user_version = 2");
    db.close();

    db = DatabaseHelper.getInstance(ctx).getWritableDatabase();
    assertUpgradedState(db);
    DatabaseHelper.getInstance(ctx).close();
  }

  @Test
  public void freshInstallHasVersionThreeWithIndexes() {
    Context ctx = RuntimeEnvironment.getApplication();
    SQLiteDatabase db = DatabaseHelper.getInstance(ctx).getWritableDatabase();

    assertEquals(DatabaseContract.DATABASE_VERSION, db.getVersion());
    assertSmsIndexesExist(db);
    DatabaseHelper.getInstance(ctx).close();
  }

  private static void seedLegacyRows(SQLiteDatabase db) {
    db.execSQL("INSERT INTO category (_id, name, color, visibility)"
        + " VALUES (1, 'Unknown', 'white', 1)");
    db.execSQL("INSERT INTO sms (_id, date, person, address, body, category_id)"
        + " VALUES (10, 1000, 0, '+15551234567', 'first message', 1)");
    db.execSQL("INSERT INTO sms (_id, date, person, address, body, category_id)"
        + " VALUES (11, 2000, 0, '+15557654321', 'second message', 1)");
  }

  private static void assertUpgradedState(SQLiteDatabase db) {
    assertEquals(DatabaseContract.DATABASE_VERSION, db.getVersion());
    assertSmsIndexesExist(db);

    Cursor c = db.rawQuery(
        "SELECT _id, body FROM sms ORDER BY _id", null);
    List<String> bodies = new ArrayList<>();
    while (c.moveToNext()) {
      bodies.add(c.getString(c.getColumnIndexOrThrow("body")));
    }
    c.close();
    assertEquals(Arrays.asList("first message", "second message"), bodies);

    c = db.rawQuery("SELECT cleaned_sms, visibility FROM sms WHERE _id = 10", null);
    assertTrue(c.moveToFirst());
    assertEquals("", c.getString(c.getColumnIndexOrThrow("cleaned_sms")));
    assertEquals(1, c.getInt(c.getColumnIndexOrThrow("visibility")));
    c.close();

    c = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null);
    List<String> tables = new ArrayList<>();
    while (c.moveToNext()) {
      tables.add(c.getString(0));
    }
    c.close();
    assertTrue(tables.containsAll(Arrays.asList("sms", "category", "config")));
  }

  private static void assertSmsIndexesExist(SQLiteDatabase db) {
    Cursor c = db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_sms_%'", null);
    List<String> indexes = new ArrayList<>();
    while (c.moveToNext()) {
      indexes.add(c.getString(0));
    }
    c.close();
    assertEquals(3, indexes.size());
    assertTrue(indexes.containsAll(Arrays.asList(
        "idx_sms_category", "idx_sms_date", "idx_sms_visibility")));
  }
}
