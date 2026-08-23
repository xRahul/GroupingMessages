package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import in.rahulja.groupingmessages.db.AppDatabase;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.ConfigDao;
import in.rahulja.groupingmessages.db.DatabaseBackup;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class BackupRestoreTest {

  private static final String PROBE_KEY = "backup_probe_key";
  private static final String PROBE_VALUE = "backup_probe_value";

  private Context ctx;
  private File backupFile;

  @Before
  public void setUp() throws Exception {
    ctx = RuntimeEnvironment.getApplication();
    // Robolectric keeps static state across tests of a class while giving each
    // test a fresh data dir; drop the helper singleton so the next open binds
    // to this test's own database file instead of a previous test's
    java.lang.reflect.Field f =
        DatabaseHelper.class.getDeclaredField("sInstance");
    f.setAccessible(true);
    DatabaseHelper inst = (DatabaseHelper) f.get(null);
    if (inst != null) {
      inst.close();
      f.set(null, null);
    }
    AppDatabase.get(ctx);

    File sd = ctx.getExternalFilesDir(null);
    assertNotNull(sd);
    backupFile = new File(sd, DatabaseBackup.BACKUP_DB_PATH);
  }

  @Test
  public void exportThenMutateThenImportRestoresOriginalData() throws Exception {
    ConfigDao.put(ctx, PROBE_KEY, PROBE_VALUE);
    long categoryId = CategoryDao.insert(ctx, "ExportedCategory");

    DatabaseBackup.exportDb(ctx);
    assertTrue(backupFile.isFile());

    // Mutate live database after export
    ConfigDao.put(ctx, PROBE_KEY, "mutated");
    assertTrue(CategoryDao.deleteById(ctx, categoryId) > 0);
    assertNull(CategoryDao.getById(ctx, categoryId));

    DatabaseBackup.importDb(ctx);

    // Lazy reopen picks up the restored file
    AppDatabase.get(ctx);
    assertEquals(PROBE_VALUE, ConfigDao.getValue(ctx, PROBE_KEY));
    assertNotNull(CategoryDao.getById(ctx, categoryId));
    assertEquals("ExportedCategory",
        CategoryDao.getById(ctx, categoryId).getName());
  }

  @Test
  public void noTmpResidueAfterSuccessfulExportAndImport() throws Exception {
    ConfigDao.put(ctx, PROBE_KEY, PROBE_VALUE);

    DatabaseBackup.exportDb(ctx);
    assertNoTmpFiles();

    DatabaseBackup.importDb(ctx);
    assertNoTmpFiles();
  }

  @Test
  public void missingBackupFileThrowsWithoutTouchingLiveDb() throws Exception {
    ConfigDao.put(ctx, PROBE_KEY, PROBE_VALUE);
    assertFalse(backupFile.exists());

    try {
      DatabaseBackup.importDb(ctx);
      fail("Expected IOException for missing backup file");
    } catch (IOException expected) {
      // exception must propagate to caller, which shows the existing error toast
    }
    assertNoTmpFiles();
    AppDatabase.get(ctx);
    assertEquals(PROBE_VALUE, ConfigDao.getValue(ctx, PROBE_KEY));
  }

  @Test
  public void corruptBackupSurfacesExceptionInsteadOfProcessDeath()
      throws Exception {
    ConfigDao.put(ctx, PROBE_KEY, PROBE_VALUE);

    DatabaseBackup.exportDb(ctx);
    try (FileOutputStream out = new FileOutputStream(backupFile)) {
      out.write("this is definitely not a sqlite database".getBytes(
          StandardCharsets.UTF_8));
    }

    // Corruption surfaces from importDb as an exception that propagates to
    // the caller - never as process death, and never by swapping the live db
    try {
      DatabaseBackup.importDb(ctx);
      fail("Expected corrupted backup to surface an exception");
    } catch (IOException expected) {
      // importDb rejects the file before touching the live database
    }
    assertNoTmpFiles();
    AppDatabase.get(ctx);
    assertEquals(PROBE_VALUE, ConfigDao.getValue(ctx, PROBE_KEY));
  }

  private void assertNoTmpFiles() {
    File[] external = ctx.getExternalFilesDir(null).listFiles();
    assertNotNull(external);
    for (File file : external) {
      assertFalse("Unexpected temp residue: " + file,
          file.getName().endsWith(".tmp"));
    }
    File[] databases = ctx.getDatabasePath("x").getParentFile().listFiles();
    assertNotNull(databases);
    for (File file : databases) {
      assertFalse("Unexpected temp residue: " + file,
          file.getName().endsWith(".tmp"));
    }
  }
}
