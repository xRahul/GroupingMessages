# Implementation Plan - SMS Search and Filtering

Enable text-based search and real-time filtering of SMS messages (by body, sender address, and contact name) in `SmsActivity` to allow users to filter, inspect, and recategorize messages efficiently.

---

## User Review Required

> [!IMPORTANT]
> **Recategorization Index Bug Fix**: Currently, `SmsActivity.onActivityResult` resolves the target SMS by index (`currentSms.get(smsListPosition)`). When the list is filtered, the adapter position in the filtered list does not match the full list index. We will update `SmsActivity` to read `sms_id` directly from `receivedIntent` (already provided by `ChangeCategoryActivity`), making recategorization 100% reliable during active search filters.

> [!NOTE]
> **Scope**: Search will be integrated into `SmsActivity` (the per-category message view where recategorization happens). Filtering will be real-time (as you type) across SMS body text, sender phone/address, and resolved contact name.

---

## Proposed Changes

```
┌─────────────────────────────────────────────────────────────┐
│                        SmsActivity                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Toolbar: [<-] [Category Name]        [🔍 Search] [⚙️]  │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ (If searching) SearchView: [ "swiggy"              ✖] │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ RecyclerView (Filtered Results)                       │  │
│  │ ┌───────────────────────────────────────────────────┐ │  │
│  │ │ Sender: Swiggy (BZ-SWIGGY)            12:30 PM    │ │  │
│  │ │ "Your order from Bowl Company is on the way!"     │ │  │
│  │ │ [ Change Category - Orders ]                      │ │  │
│  │ └───────────────────────────────────────────────────┘ │  │
│  │ ┌───────────────────────────────────────────────────┐ │  │
│  │ │ Empty View (if no matches):                       │ │  │
│  │ │ "No messages matching 'swiggy'"                   │ │  │
│  │ └───────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

### Component 1: Resources & UI Layouts

#### [NEW] `app/src/main/res/menu/menu_sms.xml`
Create a dedicated menu for `SmsActivity` containing:
- `action_search`: `androidx.appcompat.widget.SearchView` with `app:showAsAction="always|collapseActionView"` and search hint `Search messages`.
- `action_settings`: Settings icon (`@drawable/ic_settings`).

#### [MODIFY] `app/src/main/res/values/strings.xml`
Add search-related string resources:
- `search_messages_hint`: "Search messages..."
- `no_matching_sms`: "No messages matching \"%1$s\""

#### [MODIFY] `app/src/main/res/layout/content_sms.xml`
Add an empty view `TextView` (`@id/empty_sms_search_view`) positioned in the center, visible only when the filtered list is empty.

---

### Component 2: Business Logic & Filtering Engine

#### [NEW] `app/src/main/java/in/rahulja/groupingmessages/util/SmsFilter.java`
A pure, testable utility class to perform fast, case-insensitive multi-field filtering:
- Matches substring across:
  1. `sms.getBody()`
  2. `sms.getAddress()`
  3. `contactNames.get(sms.getAddress())`
- Handles null values, extra whitespace, case transformations, and empty queries cleanly.

```java
public final class SmsFilter {
  private SmsFilter() {}

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
    String lowerQuery = query.trim().toLowerCase(Locale.getDefault());
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
    if (sms.getBody() != null && sms.getBody().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
      return true;
    }
    if (sms.getAddress() != null && sms.getAddress().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
      return true;
    }
    if (contactNames != null && sms.getAddress() != null) {
      String contact = contactNames.get(sms.getAddress());
      if (contact != null && contact.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
        return true;
      }
    }
    return false;
  }
}
```

---

### Component 3: UI Controller & Activity Integration

#### [MODIFY] `app/src/main/java/in/rahulja/groupingmessages/SmsActivity.java`
1. **Menu & SearchView Binding**:
   - Inflate `R.menu.menu_sms` in `onCreateOptionsMenu`.
   - Setup `SearchView` with `SearchView.OnQueryTextListener` to update `activeQuery` and call `applyFilter()`.
   - Setup `MenuItem.OnActionExpandListener` to reset search when collapsed.
2. **State Management**:
   - Track `allLoadedSms`, `cachedContactNames`, `cachedCategories`, and `activeQuery`.
   - In `onSmsLoaded`, cache incoming data and call `applyFilter()`.
3. **Empty View Handling**:
   - Toggle visibility between `listView` and `empty_sms_search_view`.
4. **Fix `onActivityResult` for Recategorization**:
   - Extract `smsId` directly from `receivedIntent.getStringExtra(ChangeCategoryActivity.SMS_ID)` or `receivedIntent.getStringExtra("sms_id")`.
   - Retrain as before, and preserve the search filter when the list refreshes.

---

## Verification Plan

### Automated Tests
1. **`SmsFilterTest`** (New Unit Tests):
   - Test empty and whitespace queries return original list.
   - Test matching by body (substring, case-insensitive).
   - Test matching by sender address / number.
   - Test matching by resolved contact display name.
   - Test non-matching query returns empty list.
   - Test null safety on all fields.
2. **`SmsListViewModelTest` & Full Test Suite**:
   - Run `./gradlew test` to ensure all existing + new tests pass without regressions.

### Manual Verification on Connected Pixel 7a
1. **Build and Install**:
   - Run `./gradlew assembleDebug` and `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. **Search Verification**:
   - Open any category with messages (e.g. Unknown / Transactions).
   - Tap Search icon in Toolbar.
   - Type a keyword present in an SMS (e.g., bank name, OTP, or sender number).
   - Verify only matching messages appear in real time.
3. **Recategorization Verification**:
   - While search query is active, tap "Change Category" on a filtered SMS card.
   - Select a target category.
   - Verify the message retrains and moves to the selected category successfully without any crash or position mismatch.
   - Verify remaining search results update accurately.
4. **Clear / Close Search**:
   - Clear query or tap back / collapse search view.
   - Verify full list of category messages restores immediately.
