package in.rahulja.groupingmessages.model;

import android.content.ContentValues;
import android.database.Cursor;

public final class Category {

  private static final String COL_ID = "_id";
  private static final String COL_NAME = "name";
  private static final String COL_COLOR = "color";

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
        cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
        cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLOR)));
  }

  public ContentValues toContentValues() {
    ContentValues values = new ContentValues();
    values.put(COL_NAME, name);
    values.put(COL_COLOR, color);
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
