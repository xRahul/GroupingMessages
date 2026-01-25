package in.rahulja.groupingmessages;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("WeakerAccess") class ExternalContentBridge {

  private static final String LAST_SMS_TIME_CONFIG = "lastSmsTime";
  private static final String SMS_URI_INBOX = "content://sms/inbox";
  private static final String GM_CURSOR = "GM/cursor";
  private static final String CURSOR_IS_NULL = "Cursor is null: ";

  private ExternalContentBridge() {
    //empty constructor
  }

  public static List<Map<String, String>> getLatestSmsFromInbox(Context context) {

    long lastSmsTime = Long.parseLong(DatabaseBridge.getConfig(context, LAST_SMS_TIME_CONFIG));

    List<Map<String, String>> latestSms = new ArrayList<>();
    Uri uri = Uri.parse(SMS_URI_INBOX);
    String[] projection = new String[] {
        DatabaseContract.Sms._ID,
        DatabaseContract.Sms.KEY_DATE,
        DatabaseContract.Sms.KEY_PERSON,
        DatabaseContract.Sms.KEY_READ,
        DatabaseContract.Sms.KEY_SEEN,
        DatabaseContract.Sms.KEY_SUBJECT,
        DatabaseContract.Sms.KEY_BODY,
        DatabaseContract.Sms.KEY_ADDRESS,
    };
    StringBuilder searchString = new StringBuilder();
    searchString.append(DatabaseContract.Sms.KEY_DATE)
        .append(" > ").append(String.valueOf(lastSmsTime));

    Log.d("GM/searchString", searchString.toString());

    Cursor cursor = context.getContentResolver().query(
        uri,
        projection,
        searchString.toString(),
        null,
        DatabaseContract.Sms.KEY_DATE + " asc"
    );

    if (cursor != null && cursor.moveToFirst()) {
      int indexDate = cursor.getColumnIndex(DatabaseContract.Sms.KEY_DATE);
      int indexPerson = cursor.getColumnIndex(DatabaseContract.Sms.KEY_PERSON);
      int indexRead = cursor.getColumnIndex(DatabaseContract.Sms.KEY_READ);
      int indexSeen = cursor.getColumnIndex(DatabaseContract.Sms.KEY_SEEN);
      int indexSubject = cursor.getColumnIndex(DatabaseContract.Sms.KEY_SUBJECT);
      int indexBody = cursor.getColumnIndex(DatabaseContract.Sms.KEY_BODY);
      int indexAddress = cursor.getColumnIndex(DatabaseContract.Sms.KEY_ADDRESS);
      Map<String, String> smsTemp;

      while (!cursor.isAfterLast()) {

        long longDate = cursor.getLong(indexDate);
        long longPerson = cursor.getLong(indexPerson);
        long longRead = cursor.getLong(indexRead);
        long longSeen = cursor.getLong(indexSeen);
        String strSubject = cursor.getString(indexSubject);
        String strBody = cursor.getString(indexBody);
        String strAddress = cursor.getString(indexAddress);

        smsTemp = new HashMap<>();
        smsTemp.put(DatabaseContract.Sms.KEY_DATE, String.valueOf(longDate));
        smsTemp.put(DatabaseContract.Sms.KEY_PERSON, String.valueOf(longPerson));
        smsTemp.put(DatabaseContract.Sms.KEY_READ, String.valueOf(longRead));
        smsTemp.put(DatabaseContract.Sms.KEY_SEEN, String.valueOf(longSeen));
        smsTemp.put(DatabaseContract.Sms.KEY_SUBJECT, strSubject);
        smsTemp.put(DatabaseContract.Sms.KEY_BODY, strBody);
        smsTemp.put(DatabaseContract.Sms.KEY_ADDRESS, strAddress);

        latestSms.add(smsTemp);
        cursor.moveToNext();
      }
    } else {
      Log.e(GM_CURSOR, CURSOR_IS_NULL + "getLatestSmsFromInbox");
    }

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }

    Log.i("GM/inboxSmsCount", String.valueOf(latestSms.size()));

    return latestSms;
  }

  public static Map<String, String> getContactNames(Context context, Set<String> addressSet) {

    Map<String, String> contactList = new HashMap<>();

    for (String phoneNumber : addressSet) {
      contactList.put(phoneNumber, getContactName(context, phoneNumber));
    }

    return contactList;
  }

  private static String getContactName(Context context, String phoneNumber) {
    String contactName = "";
    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber));
    Cursor cursor = context.getContentResolver().query(uri,
        new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);
    if (cursor == null) {
      return contactName;
    }
    if (cursor.moveToFirst()) {
      contactName = cursor.getString(cursor
          .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
    }
    if (!cursor.isClosed()) {
      cursor.close();
    }
    return contactName;
  }
}
