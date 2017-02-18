package in.rahulja.groupingmessages;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class SettingsFragment extends PreferenceFragment {

    public static final String PREF_VERSION = "key_version";
    public static final String PREF_DEVELOPER = "key_developer";
    public static final String PREF_RESET_MODEL = "key_reset_model";
    public static final String PREF_DELETE_CAT = "key_reset_model_delete_cat";
    private String versionSummary;
    private Preference versionPref;
    private String latestVersionUrl;
    private Preference developerPref;
    private Preference resetModelPref;
    private Preference deleteCatPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        initPreferenceView();
        checkLatestAppVersion();
        updatePreferenceView();
    }

    private void initPreferenceView() {
        versionPref = findPreference(PREF_VERSION);
        developerPref = findPreference(PREF_DEVELOPER);
        resetModelPref = findPreference(PREF_RESET_MODEL);
        deleteCatPref = findPreference(PREF_DELETE_CAT);
    }

    private void updatePreferenceView() {

        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);

        versionPref = findPreference(PREF_VERSION);
        versionPref.setSummary(versionSummary);
        developerPref = findPreference(PREF_DEVELOPER);
        resetModelPref = findPreference(PREF_RESET_MODEL);
        deleteCatPref = findPreference(PREF_DELETE_CAT);

        initializePreferenceListener();
    }

    private void checkLatestAppVersion() {
        versionSummary = BuildConfig.VERSION_NAME;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                asyncCheckLatestAppVersion();
            }
        });
        thread.start();
    }

    private void asyncCheckLatestAppVersion() {
        try {
            URL url = new URL(getString(R.string.latest_release_url));
            HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
            ucon.setInstanceFollowRedirects(false);
            URL secondURL = new URL(ucon.getHeaderField("Location"));
            String secondUrl = String.valueOf(secondURL);
            String latestVersion = Uri.parse(secondUrl).getLastPathSegment();
            Log.d("GM/updateUrl", secondUrl);
            if (getActivity() != null) {
                String checkUrl = getString(R.string.current_release_url_prefix) + BuildConfig.VERSION_NAME;
                if (secondUrl.equals(checkUrl)) {
                    versionSummary = BuildConfig.VERSION_NAME + " " +
                            getString(R.string.version_summary_latest);
                } else {
                    versionSummary = BuildConfig.VERSION_NAME + " " +
                            "(" + getString(R.string.version_summary_changed_latest) + latestVersion + ")";
                    latestVersionUrl = secondUrl;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        versionPref.setSummary(versionSummary);
                        Log.d("GM/versionChecked", versionSummary);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        DatabaseBridge.deleteCategories(
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
        DatabaseBridge.deleteModel(
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
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_url)));
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

