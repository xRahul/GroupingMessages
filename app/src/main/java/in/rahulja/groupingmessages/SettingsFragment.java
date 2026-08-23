package in.rahulja.groupingmessages;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
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

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.preferences, rootKey);
    initPreferenceView();
    updatePreferenceView();
    initVersionCheck();
  }

  private void initPreferenceView() {
    versionPref = findPreference(PREF_VERSION);
    developerPref = findPreference(PREF_DEVELOPER);
    resetModelPref = findPreference(PREF_RESET_MODEL);
    deleteCatPref = findPreference(PREF_DELETE_CAT);
    exportDbPref = findPreference(PREF_EXPORT_DB);
    importDbPref = findPreference(PREF_IMPORT_DB);
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
        versionSummary = BuildConfig.VERSION_NAME + " "
            + getString(R.string.version_summary_latest);
        break;
      case OUTDATED:
        latestVersionUrl = settingsViewModel.getLatestVersionUrl();
        versionSummary = BuildConfig.VERSION_NAME + " "
            + "(" + getString(R.string.version_summary_changed_latest)
            + SettingsViewModel.lastPathSegment(latestVersionUrl) + ")";
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

    builder.setTitle("Import & Overwrite existing db");
    builder.setMessage(
        "Import & Overwrite existing db with one at \n" +
            new File(
                getActivity().getExternalFilesDir(null),
                DatabaseBackup.BACKUP_DB_PATH
            ).getAbsolutePath()
    );
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {

            AppExecutors.disk(() -> asyncImportDb());
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

  private void asyncImportDb() {
    try {
      DatabaseBackup.importDb(getActivity());
    } catch (Exception e) {
      Log.e("GM/importDb", Log.getStackTraceString(e));
      AppExecutors.main(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getActivity(), "Import failed!", Toast.LENGTH_SHORT).show();
        }
      });
      return;
    }
    AppExecutors.main(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(
            getActivity(),
            "Import completed!",
            Toast.LENGTH_SHORT
        ).show();
      }
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

    builder.setTitle("Export Apllication db");
    builder.setMessage(
        "Export application db & overwrite if old backup exist at \n" +
            new File(
                getActivity().getExternalFilesDir(null),
                DatabaseBackup.BACKUP_DB_PATH
            ).getAbsolutePath()
    );
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {

            AppExecutors.disk(() -> asyncExportDb());
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

  private void asyncExportDb() {
    try {
      DatabaseBackup.exportDb(getActivity());
    } catch (Exception e) {
      Log.e("GM/exportDb", Log.getStackTraceString(e));
      AppExecutors.main(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getActivity(), "Export failed!", Toast.LENGTH_SHORT).show();
        }
      });
      return;
    }

    AppExecutors.main(new Runnable() {
      @Override
      public void run() {
        showShareExportedDbAlert();
        Toast.makeText(
            getActivity(),
            "Export completed!",
            Toast.LENGTH_SHORT
        ).show();
      }
    });
  }

  private void showShareExportedDbAlert() {
    AlertDialog.Builder builder = new AlertDialog.Builder(
        getActivity()
    );

    builder.setTitle("Share exported db");
    builder.setMessage(
        "Share the database as file that you've just exported"
    );
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {
            File dbFile = new File(
                getActivity().getExternalFilesDir(null),
                DatabaseBackup.BACKUP_DB_PATH
            );
            Uri dbUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                dbFile
            );
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, dbUri);
            shareIntent.setType("*/*");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share database via"));
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

    builder.setTitle("Reset model and delete all categories?");
    builder.setMessage(
        "SMS will be retained. All categories will be deleted except unknown. All sms will move to unknown category"
    );
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {

            Runnable runnable = new Runnable() {
              @Override
              public void run() {
                asyncDeleteCategories();
              }
            };
            new Thread(runnable).start();
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

  private void asyncDeleteCategories() {
    CategoryDao.deleteCategories(
        getActivity()
    );
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(
            getActivity(),
            "Model reset and categories' deletion complete!",
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

    builder.setTitle("Reset trained model?");
    builder.setMessage(
        "SMS and categories will be retained. All sms will move to unknown category"
    );
    builder.setPositiveButton("Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int which) {

            Runnable runnable = new Runnable() {
              @Override
              public void run() {
                asyncResetModel();
              }
            };
            new Thread(runnable).start();
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

  private void asyncResetModel() {
    SmsDao.deleteModel(
        getActivity()
    );
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(
            getActivity(),
            "Model reset complete!",
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
        if (versionPref.getSummary().toString()
            .contains(getString(R.string.version_summary_changed_latest))) {
          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latestVersionUrl));
          startActivity(intent);
        }
        Log.d("GM/versionClick", versionPref.toString());
        return true;
      }
    });
  }
}
