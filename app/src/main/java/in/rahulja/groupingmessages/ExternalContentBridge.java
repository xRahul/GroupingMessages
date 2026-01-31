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
    if (!latestSms.isEmpty()) {
      Log.i("GM/anInboxSms", latestSms.get(0).toString());
    }

    return latestSms;
  }

  public static Map<String, String> getContactNames(Context context, Set<String> addressSet) {
    Map<String, String> contactList = new HashMap<>();
    if (addressSet == null || addressSet.isEmpty()) {
      return contactList;
    }

    // Initialize map with empty strings
    Map<String, List<String>> normalizedToOriginals = new HashMap<>();
    List<String> chunkOriginals = new ArrayList<>();
    List<String> chunkNormalized = new ArrayList<>();

    for (String phoneNumber : addressSet) {
      contactList.put(phoneNumber, "");
      String normalized = android.telephony.PhoneNumberUtils.normalizeNumber(phoneNumber);
      if (normalized != null && !normalized.isEmpty()) {
        if (!normalizedToOriginals.containsKey(normalized)) {
          normalizedToOriginals.put(normalized, new ArrayList<>());
        }
        normalizedToOriginals.get(normalized).add(phoneNumber);
      }
    }

    List<String> allOriginals = new ArrayList<>(addressSet);
    int batchSize = 50;

    for (int i = 0; i < allOriginals.size(); i += batchSize) {
      chunkOriginals.clear();
      chunkNormalized.clear();

      int end = Math.min(i + batchSize, allOriginals.size());
      for (int j = i; j < end; j++) {
        String original = allOriginals.get(j);
        chunkOriginals.add(original);
        String normalized = android.telephony.PhoneNumberUtils.normalizeNumber(original);
        if (normalized != null && !normalized.isEmpty()) {
          chunkNormalized.add(normalized);
        }
      }

      queryContactsBatch(context, contactList, chunkOriginals, chunkNormalized,
          normalizedToOriginals);
    }

    return contactList;
  }

  private static void queryContactsBatch(Context context, Map<String, String> contactList,
      List<String> originals, List<String> normalized,
      Map<String, List<String>> normalizedToOriginals) {

    if (originals.isEmpty()) {
      return;
    }

    StringBuilder selection = new StringBuilder();
    List<String> args = new ArrayList<>();

    // data1 (NUMBER) IN (...)
    selection.append(ContactsContract.CommonDataKinds.Phone.NUMBER).append(" IN (");
    for (int k = 0; k < originals.size(); k++) {
      selection.append("?");
      if (k < originals.size() - 1) {
        selection.append(",");
      }
      args.add(originals.get(k));
    }
    selection.append(")");

    // OR data4 (NORMALIZED_NUMBER) IN (...)
    if (!normalized.isEmpty()) {
      selection.append(" OR ").append(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
          .append(" IN (");
      for (int k = 0; k < normalized.size(); k++) {
        selection.append("?");
        if (k < normalized.size() - 1) {
          selection.append(",");
        }
        args.add(normalized.get(k));
      }
      selection.append(")");
    }

    Cursor cursor = null;
    try {
      cursor = context.getContentResolver().query(
          ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
          new String[] {
              ContactsContract.CommonDataKinds.Phone.NUMBER,
              ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
              ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
          },
          selection.toString(),
          args.toArray(new String[0]),
          null
      );

      if (cursor != null) {
        int idxNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        int idxNormalized = cursor.getColumnIndex(
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
        int idxName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

        while (cursor.moveToNext()) {
          String name = cursor.getString(idxName);
          String number = cursor.getString(idxNumber);
          String norm = idxNormalized != -1 ? cursor.getString(idxNormalized) : null;

          if (name == null || name.isEmpty()) {
            continue;
          }

          // Check direct match
          if (number != null && contactList.containsKey(number)) {
            contactList.put(number, name);
          }

          // Check normalized match
          if (norm != null && normalizedToOriginals.containsKey(norm)) {
            for (String orig : normalizedToOriginals.get(norm)) {
              contactList.put(orig, name);
            }
          }
        }
      }
    } catch (Exception e) {
      Log.e("GM/ExternalContent", "Error querying contacts", e);
    } finally {
      if (cursor != null && !cursor.isClosed()) {
        cursor.close();
      }
    }
  }
}
