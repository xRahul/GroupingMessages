package in.rahulja.groupingmessages.model;

import android.content.ContentValues;
import android.database.Cursor;

public final class Sms {

  private static final String COL_ID = "_id";
  private static final String COL_CATEGORY_ID = "category_id";
  private static final String COL_DATE = "date";
  private static final String COL_VISIBILITY = "visibility";
  private static final String COL_ADDRESS = "address";
  private static final String COL_BODY = "body";

  private final long id;
  private final long categoryId;
  private final long date;
  private final int visibility;
  private final String address;
  private final String body;

  public Sms(long id, long categoryId, long date, int visibility, String address, String body) {
    this.id = id;
    this.categoryId = categoryId;
    this.date = date;
    this.visibility = visibility;
    this.address = address;
    this.body = body;
  }

  public static Sms fromCursor(Cursor cursor) {
    return new Sms(
        cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CATEGORY_ID)),
        cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)),
        cursor.getInt(cursor.getColumnIndexOrThrow(COL_VISIBILITY)),
        cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS)),
        cursor.getString(cursor.getColumnIndexOrThrow(COL_BODY)));
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    values.put(COL_CATEGORY_ID, categoryId);
    values.put(COL_DATE, date);
    values.put(COL_VISIBILITY, visibility);
    values.put(COL_ADDRESS, address);
    values.put(COL_BODY, body);
    return values;
  }

  public long getId() {
    return id;
  }

  public long getCategoryId() {
    return categoryId;
  }

  public long getDate() {
    return date;
  }

  public int getVisibility() {
    return visibility;
  }

  public String getAddress() {
    return address;
  }

  public String getBody() {
    return body;
  }
}
