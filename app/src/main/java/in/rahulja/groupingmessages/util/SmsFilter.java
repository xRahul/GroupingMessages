package in.rahulja.groupingmessages.util;

import in.rahulja.groupingmessages.model.Sms;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SmsFilter {

  private SmsFilter() {
  }

  public static List<Sms> filter(
      List<Sms> allSms,
      String query,
      Map<String, String> contactNamesByAddress
  ) {
    if (allSms == null || allSms.isEmpty()) {
      return Collections.emptyList();
    }
    if (query == null || query.trim().isEmpty()) {
      return allSms;
    }
    String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
    List<Sms> result = new ArrayList<>();
    for (Sms sms : allSms) {
      if (matches(sms, lowerQuery, contactNamesByAddress)) {
        result.add(sms);
      }
    }
    return result;
  }

  private static boolean matches(
      Sms sms,
      String lowerQuery,
      Map<String, String> contactNames
  ) {
    if (sms == null) {
      return false;
    }
    if (sms.getBody() != null && sms.getBody().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
      return true;
    }
    if (sms.getAddress() != null && sms.getAddress().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
      return true;
    }
    if (contactNames != null && sms.getAddress() != null) {
      String contact = contactNames.get(sms.getAddress());
      if (contact != null && contact.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
        return true;
      }
    }
    return false;
  }
}
