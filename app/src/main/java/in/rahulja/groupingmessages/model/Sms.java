package in.rahulja.groupingmessages.model;

import in.rahulja.groupingmessages.DatabaseContract;

import android.content.ContentValues;
import android.database.Cursor;

public final class Sms {

  private final long id;
  private final long categoryId;
  private final long date;
  private final int visibility;
  private final int read;
  private final String address;
  private final String body;
  private final long similarTo;

  public Sms(long id, long categoryId, long date, int visibility, int read, String address,
      String body, long similarTo) {
    this.id = id;
    this.categoryId = categoryId;
    this.date = date;
    this.visibility = visibility;
    this.read = read;
    this.address = address;
    this.body = body;
    this.similarTo = similarTo;
  }

  public static Sms fromCursor(Cursor cursor) {
    return new Sms(
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms._ID)),
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_CATEGORY_ID)),
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_DATE)),
        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_VISIBILITY)),
        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_READ)),
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_ADDRESS)),
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_BODY)),
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Sms.KEY_SIMILAR_TO)));
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

  public int getRead() {
    return read;
  }

  public String getAddress() {
    return address;
  }

  public String getBody() {
    return body;
  }

  public long getSimilarTo() {
    return similarTo;
  }
}
