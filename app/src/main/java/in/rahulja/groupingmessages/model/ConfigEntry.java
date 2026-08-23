package in.rahulja.groupingmessages.model;

import android.database.Cursor;

public final class ConfigEntry {

  private static final String COL_NAME = "name";
  private static final String COL_VALUE = "value";

  private final String key;
  private final String value;

  public ConfigEntry(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public static ConfigEntry fromCursor(Cursor cursor) {
    return new ConfigEntry(
        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
        cursor.getString(cursor.getColumnIndexOrThrow(COL_VALUE)));
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
