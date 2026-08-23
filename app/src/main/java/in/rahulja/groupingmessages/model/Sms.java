package in.rahulja.groupingmessages.model;

import in.rahulja.groupingmessages.DatabaseContract;

import android.content.ContentValues;
import android.database.Cursor;

public final class Sms {

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
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms._ID)),
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID)),
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_DATE)),
        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_VISIBILITY)),
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_ADDRESS)),
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_BODY)));
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Sms.KEY_CATEGORY_ID, categoryId);
    values.put(DatabaseContract.Sms.KEY_DATE, date);
    values.put(DatabaseContract.Sms.KEY_VISIBILITY, visibility);
    values.put(DatabaseContract.Sms.KEY_ADDRESS, address);
    values.put(DatabaseContract.Sms.KEY_BODY, body);
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
