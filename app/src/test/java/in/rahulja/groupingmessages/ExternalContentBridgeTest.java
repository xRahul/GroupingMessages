package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ExternalContentBridgeTest {

    public static class MockContactsProvider extends ContentProvider {
        public int queryCount = 0;

        @Override
        public boolean onCreate() { return true; }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            queryCount++;
            return new MatrixCursor(new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            });
        }

        @Override public String getType(Uri uri) { return null; }
        @Override public Uri insert(Uri uri, ContentValues values) { return null; }
        @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
        @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    }

    @Test
    public void testGetContactNames_BatchedQuery() {
        MockContactsProvider provider = new MockContactsProvider();
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, provider);

        Context context = RuntimeEnvironment.getApplication();
        Set<String> addresses = new HashSet<>();
        addresses.add("12345");
        addresses.add("67890");
        addresses.add("11111");

        ExternalContentBridge.getContactNames(context, addresses);

        // Expect 1 query for 3 addresses (batch size 50)
        assertEquals(1, provider.queryCount);
    }
}
