package in.rahulja.groupingmessages.model;

import in.rahulja.groupingmessages.DatabaseContract;

import android.database.Cursor;

public final class ConfigEntry {

  private final String key;
  private final String value;

  public ConfigEntry(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public static ConfigEntry fromCursor(Cursor cursor) {
    return new ConfigEntry(
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Config.KEY_NAME)),
        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.Config.KEY_VALUE)));
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
