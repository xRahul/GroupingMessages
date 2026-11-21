package in.rahulja.groupingmessages;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import in.rahulja.groupingmessages.db.BackupCrypto;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.DatabaseBackup;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.vm.AppExecutors;
import in.rahulja.groupingmessages.vm.SettingsViewModel;

public class SettingsFragment extends PreferenceFragmentCompat {

  public static final String PREF_VERSION = "key_version";
  public static final String PREF_DEVELOPER = "key_developer";
  public static final String PREF_RESET_MODEL = "key_reset_model";
  public static final String PREF_DELETE_CAT = "key_reset_model_delete_cat";
  public static final String PREF_EXPORT_DB = "key_export_db";
  public static final String PREF_IMPORT_DB = "key_import_db";
  private String versionSummary;
  private Preference versionPref;
  private String latestVersionUrl;
  private SettingsViewModel settingsViewModel;
  private Preference developerPref;
  private Preference resetModelPref;
  private Preference deleteCatPref;
  private Preference exportDbPref;
  private Preference importDbPref;
  private SwitchPreferenceCompat checkUpdatesPref;
  private ActivityResultLauncher<String> exportLauncher;
  private ActivityResultLauncher<String[]> importLauncher;
  private char[] pendingExportPassphrase;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    registerBackupLaunchers();
    setPreferencesFromResource(R.xml.preferences, rootKey);
    initPreferenceView();
    updatePreferenceView();
    initVersionCheck();
  }

  private void registerBackupLaunchers() {
    exportLauncher = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/octet-stream"),
        uri -> {
          if (uri != null) {
            asyncExportDb(uri);
          } else {
            clearPendingExportPassphrase();
          }
        });
    importLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        uri -> {
          if (uri != null) {
            asyncImportDb(uri);
          }
        });
  }

  private void initPreferenceView() {
    versionPref = findPreference(PREF_VERSION);
    developerPref = findPreference(PREF_DEVELOPER);
    resetModelPref = findPreference(PREF_RESET_MODEL);
    deleteCatPref = findPreference(PREF_DELETE_CAT);
    exportDbPref = findPreference(PREF_EXPORT_DB);
    importDbPref = findPreference(PREF_IMPORT_DB);
    checkUpdatesPref = findPreference(SettingsViewModel.PREF_CHECK_UPDATES);
  }

  private void updatePreferenceView() {
    // Re-finding is safe
    versionPref = findPreference(PREF_VERSION);
    if (versionSummary != null) {
        versionPref.setSummary(versionSummary);
    }
    developerPref = findPreference(PREF_DEVELOPER);
    resetModelPref = findPreference(PREF_RESET_MODEL);
    deleteCatPref = findPreference(PREF_DELETE_CAT);
    exportDbPref = findPreference(PREF_EXPORT_DB);
    importDbPref = findPreference(PREF_IMPORT_DB);
    checkUpdatesPref = findPreference(SettingsViewModel.PREF_CHECK_UPDATES);

    initializePreferenceListener();
  }

  private void initVersionCheck() {
    versionSummary = BuildConfig.VERSION_NAME;
    if (versionPref != null) {
      versionPref.setSummary(versionSummary);
    }
    settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    settingsViewModel.getVersionState().observe(this,
        state -> applyVersionState(state));
    settingsViewModel.checkLatestVersion();
  }

  private void applyVersionState(SettingsViewModel.VersionState state) {
    if (!isAdded()) {
      return;
    }
    switch (state) {
      case UP_TO_DATE:
        latestVersionUrl = null;
        versionSummary = BuildConfig.VERSION_NAME + " "
            + getString(R.string.version_summary_latest);
        break;
      case OUTDATED:
        latestVersionUrl = settingsViewModel.getLatestVersionUrl();
        versionSummary = BuildConfig.VERSION_NAME + " "
            + "(" + getString(R.string.version_summary_changed_latest)
            + SettingsViewModel.lastPathSegment(latestVersionUrl) + ")";
        break;
      case DISABLED:
        versionSummary = BuildConfig.VERSION_NAME + " "
            + getString(R.string.version_summary_check_disabled);
        break;
      case ERROR:
      default:
        // legacy behavior: failed check leaves the plain version summary
        return;
    }
    if (versionPref != null) {
      versionPref.setSummary(versionSummary);
    }
    Log.d("GM/versionChecked", versionSummary);
  }

  private void initializePreferenceListener() {
    Log.d("GM/initPref", "Listener");

    initializePreferenceClickListener();
  }

  private void initializePreferenceClickListener() {
    initializeResetModelPreferenceClickListener();

    initializeDeleteCategoriesPreferenceClickListener();

    initializeVersionPreferenceClickListener();

    initializeUpdateCheckPreferenceListener();

    initializeDeveloperPreferenceClickListener();

    initializeExportDbPreferenceClickListener();

    initializeImportDbPreferenceClickListener();
  }

  private void initializeImportDbPreferenceClickListener() {
    importDbPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference importDbPref) {
        importDbPreferenceClick(importDbPref);
        return true;
      }
    });
  }

  private void importDbPreferenceClick(Preference importDbPref) {
    Log.d("GM/importDbClick", importDbPref.toString());

    AlertDialog.Builder builder = new AlertDialog.Builder(
        getActivity()
    );

    builder.setTitle(R.string.import_db_title);
    builder.setMessage(R.string.import_db_warning);
    builder.setPositiveButton(R.string.yes,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            importLauncher.launch(new String[] {
                "application/octet-stream",
                "application/x-sqlite3",
                "application/vnd.sqlite3"
            });
          }
        });
    builder.setNegativeButton(R.string.no,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            // do nothing
          }
        });
    builder.show();
  }

  private void asyncImportDb(Uri uri) {
    Context appContext = requireContext().getApplicationContext();
    AppExecutors.disk(() -> {
      boolean encrypted;
      try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
        if (in == null) {
          throw new java.io.IOException("Could not open the selected file");
        }
        encrypted = BackupCrypto.isEncrypted(in);
      } catch (Exception e) {
        Log.e("GM/importDb", Log.getStackTraceString(e));
        AppExecutors.main(() -> {
          if (isAdded()) {
            Toast.makeText(appContext, R.string.import_db_failed, Toast.LENGTH_SHORT).show();
          }
        });
        return;
      }
      AppExecutors.main(() -> {
        if (!isAdded()) {
          return;
        }
        if (encrypted) {
          promptImportPassphrase(uri);
        } else {
          asyncImportDb(uri, null);
        }
      });
    });
  }

  private void promptImportPassphrase(Uri uri) {
    EditText input = new EditText(requireContext());
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    input.setHint(R.string.import_passphrase_hint);
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.import_passphrase_title)
        .setMessage(R.string.import_passphrase_message)
        .setView(input)
        .setPositiveButton(R.string.ok, (dialog, which) ->
            asyncImportDb(uri, input.getText().toString().toCharArray()))
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void asyncImportDb(Uri uri, char[] passphrase) {
    Context appContext = requireContext().getApplicationContext();
    AppExecutors.disk(() -> {
      try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
        if (in == null) {
          throw new java.io.IOException("Could not open the selected file");
        }
        DatabaseBackup.importFrom(appContext, in, passphrase);
      } catch (Exception e) {
        Log.e("GM/importDb", Log.getStackTraceString(e));
        AppExecutors.main(() -> {
          if (isAdded()) {
            Toast.makeText(appContext, R.string.import_db_failed, Toast.LENGTH_SHORT).show();
          }
        });
        return;
      } finally {
        if (passphrase != null) {
          Arrays.fill(passphrase, '\0');
        }
      }
      AppExecutors.main(() -> {
        if (!isAdded()) {
          return;
        }
        Toast.makeText(appContext, R.string.import_db_completed, Toast.LENGTH_SHORT).show();
      });
    });
  }

  private void initializeExportDbPreferenceClickListener() {
    exportDbPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference exportDbPref) {
        exportDbPreferenceClick(exportDbPref);
        return true;
      }
    });
  }

  private void exportDbPreferenceClick(Preference exportDbPref) {
    Log.d("GM/exportDbClick", exportDbPref.toString());

    AlertDialog.Builder builder = new AlertDialog.Builder(
        getActivity()
    );

    builder.setTitle(R.string.export_db_title);
    builder.setMessage(R.string.export_db_warning);
    builder.setPositiveButton(R.string.yes,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            promptExportPassphrase();
          }
        });
    builder.setNegativeButton(R.string.no,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            // do nothing
          }
        });
    builder.show();
  }

  private void promptExportPassphrase() {
    EditText input = new EditText(requireContext());
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    input.setHint(R.string.export_passphrase_hint);
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.export_encrypt_title)
        .setMessage(R.string.export_encrypt_message)
        .setView(input)
        .setPositiveButton(R.string.ok, (dialog, which) -> {
          clearPendingExportPassphrase();
          String value = input.getText().toString();
          pendingExportPassphrase = value.isEmpty() ? null : value.toCharArray();
          exportLauncher.launch("grouping-messages-backup.db");
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void clearPendingExportPassphrase() {
    if (pendingExportPassphrase != null) {
      Arrays.fill(pendingExportPassphrase, '\0');
      pendingExportPassphrase = null;
    }
  }

  private void asyncExportDb(Uri uri) {
    Context appContext = requireContext().getApplicationContext();
    char[] passphrase = pendingExportPassphrase;
    pendingExportPassphrase = null;
    AppExecutors.disk(() -> {
      try (OutputStream out = appContext.getContentResolver().openOutputStream(uri)) {
        if (out == null) {
          throw new java.io.IOException("Could not open the selected file");
        }
        if (passphrase != null && passphrase.length > 0) {
          DatabaseBackup.exportEncryptedTo(appContext, out, passphrase);
        } else {
          DatabaseBackup.exportTo(appContext, out);
        }
      } catch (Exception e) {
        Log.e("GM/exportDb", Log.getStackTraceString(e));
        AppExecutors.main(() -> {
          if (isAdded()) {
            Toast.makeText(appContext, R.string.export_db_failed, Toast.LENGTH_SHORT).show();
          }
        });
        return;
      } finally {
        if (passphrase != null) {
          Arrays.fill(passphrase, '\0');
        }
      }
      AppExecutors.main(() -> {
        if (!isAdded()) {
          return;
        }
        Toast.makeText(appContext, R.string.export_db_completed, Toast.LENGTH_SHORT).show();
      });
    });
  }

  private void initializeDeleteCategoriesPreferenceClickListener() {
    deleteCatPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference deleteCatPref) {
        deleteCategoriesPreferenceClick(deleteCatPref);
        return true;
      }
    });
  }

  private void deleteCategoriesPreferenceClick(Preference deleteCatPref) {
    Log.d("GM/developerClick", deleteCatPref.toString());

    AlertDialog.Builder builder = new AlertDialog.Builder(
        getActivity()
    );

    builder.setTitle(R.string.reset_categories_title);
    builder.setMessage(R.string.reset_categories_warning);
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {

            Context appContext =
                requireContext().getApplicationContext();
            AppExecutors.disk(() -> asyncDeleteCategories(appContext));
          }
        });
    builder.setNegativeButton("No",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            // do nothing
          }
        });
    builder.show();
  }

  private void asyncDeleteCategories(Context appContext) {
    CategoryDao.deleteCategories(
        appContext
    );
    AppExecutors.main(new Runnable() {
      @Override
      public void run() {
        if (!isAdded()) {
          return;
        }
        Toast.makeText(
            appContext,
            R.string.reset_categories_completed,
            Toast.LENGTH_SHORT
        ).show();
      }
    });
  }

  private void initializeResetModelPreferenceClickListener() {
    resetModelPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference resetModelPref) {
        resetModelPreferenceClick(resetModelPref);
        return true;
      }
    });
  }

  private void resetModelPreferenceClick(Preference resetModelPref) {
    Log.d("GM/resetModelClick", resetModelPref.toString());

    AlertDialog.Builder builder = new AlertDialog.Builder(
        getActivity()
    );

    builder.setTitle(R.string.reset_model_title);
    builder.setMessage(R.string.reset_model_warning);
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {

            Context appContext =
                requireContext().getApplicationContext();
            AppExecutors.disk(() -> asyncResetModel(appContext));
          }
        });
    builder.setNegativeButton("No",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            // do nothing
          }
        });
    builder.show();
  }

  private void asyncResetModel(Context appContext) {
    SmsDao.deleteModel(
        appContext
    );
    AppExecutors.main(new Runnable() {
      @Override
      public void run() {
        if (!isAdded()) {
          return;
        }
        Toast.makeText(
            appContext,
            R.string.reset_model_completed,
            Toast.LENGTH_SHORT
        ).show();
      }
    });
  }

  private void initializeDeveloperPreferenceClickListener() {
    developerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference developerPref) {
        Log.d("GM/developerClick", developerPref.toString());
        Intent intent =
            new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_url)));
        startActivity(intent);
        return true;
      }
    });
  }

  private void initializeVersionPreferenceClickListener() {
    versionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference versionPref) {
        if (latestVersionUrl != null
            && SettingsViewModel.isTrustedReleaseUrl(latestVersionUrl)) {
          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latestVersionUrl));
          startActivity(intent);
        }
        Log.d("GM/versionClick", versionPref.toString());
        return true;
      }
    });
  }

  private void initializeUpdateCheckPreferenceListener() {
    if (checkUpdatesPref == null) {
      return;
    }
    checkUpdatesPref.setOnPreferenceChangeListener((preference, newValue) -> {
      settingsViewModel.checkLatestVersion(Boolean.TRUE.equals(newValue));
      return true;
    });
  }
}
