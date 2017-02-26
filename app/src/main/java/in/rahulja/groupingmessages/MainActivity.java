package in.rahulja.groupingmessages;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements AddCategoryFragment.AddCategoryDialogListener {

    private static final String ADD_CATEGORY_TAG = "add_category_tag";
    private static final String COUNT_UNREAD = "count_unread";
    private static final String COUNT_READ = "count_read";
    private static final String GM_ADD_CAT = "GM/addCat";
    private static final String SMS_COUNT = "sms_count";

    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private long numRowsAddedToSms;
    private List<Map<String, String>> categoryList = new ArrayList<>();
    private ProgressBar pbCircle;
    private GridLayoutManager glm = new GridLayoutManager(getBaseContext(), 2);

    static List<Map<String, String>> addSenderTypeToListOfSms(List<Map<String, String>> listOfSms) {

        for (int i = 0; i < listOfSms.size(); i++) {
            Map<String, String> tempSms = listOfSms.get(i);
            String fromString = tempSms.get(DatabaseContract.Sms.KEY_ADDRESS);
            int senderType = DatabaseContract.Sms.SENDER_CONTACT;
            if ("0".equals(String.valueOf(tempSms.get(DatabaseContract.Sms.KEY_PERSON)))) {
                senderType = DatabaseContract.Sms.SENDER_COMPANY;
                if (fromString.matches(".*[0-9]{10}.*") && !fromString.matches(".*[a-zA-Z]+.*")) {
                    senderType = DatabaseContract.Sms.SENDER_NUMBER;
                }
            }

            tempSms.put(
                    DatabaseContract.Sms.KEY_SENDER_TYPE,
                    String.valueOf(senderType)
            );

            listOfSms.set(i, tempSms);
        }

        return listOfSms;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupActionBar();

        pbCircle = (ProgressBar) findViewById(R.id.progressBarCircle);

        createAddCategoryButton();
    }

    private void setupActionBar() {
        // set custom toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Return true to show menu
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void init() {
        getLatestSmsAndTrainThem();
        getCategoryListData();
        drawUi();
    }

    private void getCategoryListData() {
        showTitleProgressSpinner();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getAllCategoriesWithoutCount();
                addSmsCountToCategories();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        drawUi();
                        hideTitleProgressSpinner();
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkAndGetPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.READ_SMS))
            permissionsNeeded.add("Read SMS");
        if (!addPermission(permissionsList, Manifest.permission.READ_CONTACTS))
            permissionsNeeded.add("Read Contacts");
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("Write to external storage");

        Log.d("GM/permNeed", permissionsNeeded.toString());
        Log.d("GM/permList", permissionsList.toString());

        if (!permissionsList.isEmpty()) {
            if (!permissionsNeeded.isEmpty()) {

                StringBuilder message = new StringBuilder();
                message.append("You need to grant access to ")
                        .append(permissionsNeeded.get(0));
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message.append(", ").append(permissionsNeeded.get(i));

                showMessageOKCancel(message.toString(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                                );
                            }
                        }
                );
                return;
            }
            ActivityCompat.requestPermissions(
                    this,
                    permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
            );
            return;
        }
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        Log.d("GM/showPermMessage", message);
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void showTitleProgressSpinner() {
        // Show progress item
        if (pbCircle != null) {
            pbCircle.setVisibility(View.VISIBLE);
        }
    }

    public void hideTitleProgressSpinner() {
        // Hide progress item
        if (pbCircle != null) {
            pbCircle.setVisibility(View.INVISIBLE);
        }
    }

    private void getLatestSmsAndTrainThem() {

        showTitleProgressSpinner();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                asyncGetLatestSmsAndTrainThem();
            }
        };
        new Thread(runnable).start();
    }

    private void asyncGetLatestSmsAndTrainThem() {

        List<Map<String, String>> trainedLatestSmsFromInbox = TrainSms.getTrainedListOfSms(
                getBaseContext(),
                ExternalContentBridge.getLatestSmsFromInbox(getBaseContext()),
                DatabaseBridge.getSelfTrainedSms(getBaseContext())
        );

        trainedLatestSmsFromInbox = addSenderTypeToListOfSms(
                trainedLatestSmsFromInbox
        );

        numRowsAddedToSms = DatabaseBridge.storeTrainedInboxSms(
                getBaseContext(),
                trainedLatestSmsFromInbox
        );

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (numRowsAddedToSms > 0) {
                    Toast.makeText(
                            getBaseContext(),
                            String.valueOf(numRowsAddedToSms) + " new sms added",
                            Toast.LENGTH_SHORT
                    ).show();
                    getCategoryListData();
                }
                hideTitleProgressSpinner();
            }
        });
    }

    private void createAddCategoryButton() {
        FloatingActionButton fabAddCategory = (FloatingActionButton) findViewById(R.id.fab_add_category);
        fabAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new AddCategoryFragment();
                Bundle args = new Bundle();
                args.putString("ACTION", "CREATE");
                newFragment.setArguments(args);
                newFragment.show(getSupportFragmentManager(), ADD_CATEGORY_TAG);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("GM/checkPerm", String.valueOf(Build.VERSION.SDK_INT));
            checkAndGetPermissions();
        } else {
            init();
        }
    }

    private void addSmsCountToCategories() {

        List<Map<String, String>> categoryIdsWithSmsCount = DatabaseBridge.getCategoryIdsWithSmsCount(this);

        for (int i = 0; i < categoryList.size(); i++) {
            Map<String, String> categoryListItem = categoryList.get(i);

            for (int j = 0; j < categoryIdsWithSmsCount.size(); j++) {
                if (categoryIdsWithSmsCount.get(j).get(DatabaseContract.Sms.KEY_CATEGORY_ID)
                        .equals(categoryListItem.get(DatabaseContract.Category._ID))) {

                    String countKey = COUNT_UNREAD;
                    if (Integer.parseInt(categoryIdsWithSmsCount.get(j).get(DatabaseContract.Sms.KEY_READ)) == 1) {
                        countKey = COUNT_READ;
                    }

                    categoryListItem.put(
                            countKey,
                            String.valueOf(categoryIdsWithSmsCount.get(j).get(SMS_COUNT))
                    );
                }
            }
            categoryList.set(i, categoryListItem);
        }

        Log.i("GM/addCountToCategories", categoryList.toString());
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, Bundle oldArgs) {
        // User touched the dialog's positive button
        Log.i(GM_ADD_CAT, "User touched the add category dialog's add button");

        EditText categoryName = (EditText) dialog.getDialog().findViewById(R.id.editTextAddCategory);
        ColorPickerView cpView = (ColorPickerView) dialog.getDialog().findViewById(R.id.pick_category_color);

        if (categoryName.getText().toString().isEmpty()) {
            Toast.makeText(this, "Need Category Name", Toast.LENGTH_SHORT).show();
            Log.e(GM_ADD_CAT, "Need Category Name");
            return;
        }

        Map<String, String> newCategory = new HashMap<>();

        newCategory.put(DatabaseContract.Category.KEY_NAME, categoryName.getText().toString());
        newCategory.put(DatabaseContract.Category.KEY_VISIBILITY, String.valueOf(1));
        newCategory.put(DatabaseContract.Category.KEY_COLOR, String.valueOf(cpView.getSelectedColor()));

        if ("EDIT".equals(oldArgs.getString("ACTION"))) {
            newCategory.put(
                    DatabaseContract.Category._ID,
                    String.valueOf(oldArgs.getLong(DatabaseContract.Category._ID))
            );
            Boolean categoryUpdated = DatabaseBridge.updateCategory(this, newCategory);
            if (categoryUpdated) {
                Toast.makeText(this, "Successfully updated category: " + categoryName.getText(), Toast.LENGTH_SHORT).show();
                Log.i(GM_ADD_CAT, "Successfully updated category: " + categoryName.getText());
            }
        } else {
            Boolean categoryAdded = DatabaseBridge.addCategory(this, newCategory);
            if (categoryAdded) {
                Toast.makeText(this, "Successfully added category: " + categoryName.getText(), Toast.LENGTH_SHORT).show();
                Log.i(GM_ADD_CAT, "Successfully added category: " + categoryName.getText());
            }
        }
        getCategoryListData();
    }

    private void drawUi() {
        int positionIndex = glm.findFirstVisibleItemPosition();
        CategoryListArrayAdapter categoryItemsAdapter = new CategoryListArrayAdapter(getBaseContext(), categoryList);
        RecyclerView listView = (RecyclerView) findViewById(R.id.category_list_view);
        listView.setLayoutManager(glm);
        listView.setHasFixedSize(true);
        listView.setAdapter(categoryItemsAdapter);
        glm.scrollToPosition(positionIndex);
    }

    private void getAllCategoriesWithoutCount() {

        List<Map<String, String>> allCategories = DatabaseBridge.getAllVisibleCategories(this);

        if (categoryList == null) {
            categoryList = new ArrayList<>();
        } else {
            categoryList.clear();
        }

        for (Map<String, String> category : allCategories) {
            category.put(COUNT_UNREAD, "0");
            category.put(COUNT_READ, "0");
            categoryList.add(category);
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog, Bundle oldArgs) {
        // User touched the dialog's negative button
        Log.i(GM_ADD_CAT, "User touched the add category dialog's cancel button");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
            Map<String, Integer> perms = new HashMap<>();
            perms.put(Manifest.permission.READ_SMS, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.READ_CONTACTS, PackageManager.PERMISSION_GRANTED);
            for (int i = 0; i < permissions.length; i++)
                perms.put(permissions[i], grantResults[i]);
            if (perms.get(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    && perms.get(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                // All Permissions Granted
                init();
            } else {
                // Permission Denied
                Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                        .show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
