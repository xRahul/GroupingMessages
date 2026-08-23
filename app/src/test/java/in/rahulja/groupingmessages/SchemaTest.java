package in.rahulja.groupingmessages;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class SchemaTest {

  @Test
  public void freshInstallCreatesValidSchema() {
    Context ctx = RuntimeEnvironment.getApplication();
    SQLiteDatabase db = DatabaseHelper.getInstance(ctx).getWritableDatabase();
    Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
    List<String> tables = new ArrayList<>();
    while (c.moveToNext()) {
      tables.add(c.getString(0));
    }
    c.close();

    List<String> fkRefs = new ArrayList<>();
    Cursor fk = db.rawQuery("PRAGMA foreign_key_list(sms)", null);
    while (fk.moveToNext()) {
      fkRefs.add(fk.getString(2) + "." + fk.getString(3));
    }
    fk.close();
    db.close();

    assertTrue(tables.contains("sms"));
    assertTrue(tables.contains("category"));
    assertTrue(tables.contains("config"));
    assertTrue(tables.contains("android_metadata"));
    assertTrue(fkRefs.contains("category.category_id"));
    assertTrue(fkRefs.contains("sms.similar_to"));
  }
}
