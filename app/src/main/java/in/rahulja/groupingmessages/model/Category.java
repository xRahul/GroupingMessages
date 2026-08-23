package in.rahulja.groupingmessages.model;

import in.rahulja.groupingmessages.DatabaseContract;

import android.content.ContentValues;
import android.database.Cursor;

public final class Category {

  private final long id;
  private final String name;
  private final int color;

  public Category(long id, String name, int color) {
    this.id = id;
    this.name = name;
    this.color = color;
  }

  public static Category fromCursor(Cursor cursor) {
    return new Category(
        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Category._ID)),
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_NAME)),
        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_COLOR)));
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    values.put(DatabaseContract.Category.KEY_NAME, name);
    values.put(DatabaseContract.Category.KEY_COLOR, color);
    return values;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getColor() {
    return color;
  }
}
