package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import android.database.Cursor;
import android.database.MatrixCursor;
import in.rahulja.groupingmessages.model.Category;
import in.rahulja.groupingmessages.model.ConfigEntry;
import in.rahulja.groupingmessages.model.Sms;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class ModelLayerTest {

  private static final String[] CATEGORY_COLUMNS = {
      DatabaseContract.Category._ID,
      DatabaseContract.Category.KEY_NAME,
      DatabaseContract.Category.KEY_COLOR,
  };

  private static final String[] SMS_COLUMNS = {
      DatabaseContract.Sms._ID,
      DatabaseContract.Sms.KEY_CATEGORY_ID,
      DatabaseContract.Sms.KEY_DATE,
      DatabaseContract.Sms.KEY_VISIBILITY,
      DatabaseContract.Sms.KEY_ADDRESS,
      DatabaseContract.Sms.KEY_BODY,
  };

  private static final String[] CONFIG_COLUMNS = {
      DatabaseContract.Config.KEY_NAME,
      DatabaseContract.Config.KEY_VALUE,
  };

  @Test
  public void categoryFromCursorMapsColumns() {
    MatrixCursor cursor = new MatrixCursor(CATEGORY_COLUMNS);
    cursor.addRow(new Object[] {7L, "promo", -65536});
    cursor.moveToFirst();

    Category category = Category.fromCursor(cursor);

    assertEquals(7L, category.getId());
    assertEquals("promo", category.getName());
    assertEquals(-65536, category.getColor());
    assertFalse(cursor.isClosed());
  }

  @Test
  public void smsFromCursorMapsColumns() {
    MatrixCursor cursor = new MatrixCursor(SMS_COLUMNS);
    cursor.addRow(new Object[] {42L, 3L, 1699999999999L, 1, "+91 98765 43210", "Hi there"});
    cursor.moveToFirst();

    Sms sms = Sms.fromCursor(cursor);

    assertEquals(42L, sms.getId());
    assertEquals(3L, sms.getCategoryId());
    assertEquals(1699999999999L, sms.getDate());
    assertEquals(1, sms.getVisibility());
    assertEquals("+91 98765 43210", sms.getAddress());
    assertEquals("Hi there", sms.getBody());
    assertFalse(cursor.isClosed());
  }

  @Test
  public void configEntryFromCursorMapsColumns() {
    MatrixCursor cursor = new MatrixCursor(CONFIG_COLUMNS);
    cursor.addRow(new Object[] {"last_sync", "20260823"});
    cursor.moveToFirst();

    ConfigEntry entry = ConfigEntry.fromCursor(cursor);

    assertEquals("last_sync", entry.getKey());
    assertEquals("20260823", entry.getValue());
    assertFalse(cursor.isClosed());
  }

  @Test
  public void categoryContentValuesRoundTrip() {
    Category original = new Category(7L, "promo", -65536);

    MatrixCursor cursor = new MatrixCursor(CATEGORY_COLUMNS);
    cursor.addRow(new Object[] {
        original.getId(),
        original.toContentValues().getAsString(DatabaseContract.Category.KEY_NAME),
        original.toContentValues().getAsInteger(DatabaseContract.Category.KEY_COLOR),
    });
    cursor.moveToFirst();
    Category parsed = Category.fromCursor(cursor);

    assertEquals(original.getId(), parsed.getId());
    assertEquals(original.getName(), parsed.getName());
    assertEquals(original.getColor(), parsed.getColor());
  }

  @Test
  public void smsContentValuesRoundTrip() {
    Sms original = new Sms(42L, 3L, 1699999999999L, 0, "+91 98765 43210", "Hi there");

    MatrixCursor cursor = new MatrixCursor(SMS_COLUMNS);
    cursor.addRow(new Object[] {
        original.getId(),
        original.toContentValues().getAsLong(DatabaseContract.Sms.KEY_CATEGORY_ID),
        original.toContentValues().getAsLong(DatabaseContract.Sms.KEY_DATE),
        original.toContentValues().getAsInteger(DatabaseContract.Sms.KEY_VISIBILITY),
        original.toContentValues().getAsString(DatabaseContract.Sms.KEY_ADDRESS),
        original.toContentValues().getAsString(DatabaseContract.Sms.KEY_BODY),
    });
    cursor.moveToFirst();
    Sms parsed = Sms.fromCursor(cursor);

    assertEquals(original.getId(), parsed.getId());
    assertEquals(original.getCategoryId(), parsed.getCategoryId());
    assertEquals(original.getDate(), parsed.getDate());
    assertEquals(original.getVisibility(), parsed.getVisibility());
    assertEquals(original.getAddress(), parsed.getAddress());
    assertEquals(original.getBody(), parsed.getBody());
  }

  @Test
  public void fromCursorThrowsWhenColumnMissing() {
    MatrixCursor cursor = new MatrixCursor(new String[] {
        DatabaseContract.Sms._ID,
        DatabaseContract.Sms.KEY_CATEGORY_ID,
        DatabaseContract.Sms.KEY_DATE,
        DatabaseContract.Sms.KEY_VISIBILITY,
        DatabaseContract.Sms.KEY_ADDRESS,
    });
    cursor.addRow(new Object[] {42L, 3L, 1699999999999L, 1, "+91 98765 43210"});
    cursor.moveToFirst();

    try {
      Sms.fromCursor(cursor);
      throw new AssertionError("expected missing body column to throw");
    } catch (IllegalArgumentException expected) {
      assertSame(IllegalArgumentException.class, expected.getClass());
    }
  }
}
