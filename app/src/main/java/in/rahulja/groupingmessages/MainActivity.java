package in.rahulja.groupingmessages;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.flask.colorpicker.ColorPickerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.model.Category;
import in.rahulja.groupingmessages.vm.CategoriesViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
    implements AddCategoryFragment.AddCategoryDialogListener {

  private static final String ADD_CATEGORY_TAG = "add_category_tag";
  private static final String GM_ADD_CAT = "GM/addCat";

  private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
  private CategoriesViewModel categoriesViewModel;
  private ProgressBar pbCircle;
  private GridLayoutManager glm;
  private CategoryListArrayAdapter categoryItemsAdapter;
  private Map<Long, String> unreadCountsByCategoryId = new HashMap<>();
  private Map<Long, String> readCountsByCategoryId = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupActionBar();

    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
      Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return WindowInsetsCompat.CONSUMED;
    });

    pbCircle = findViewById(R.id.progressBarCircle);

    // fixed two-column grid is the current product behavior; kept as-is
    glm = new GridLayoutManager(this, 2);

    RecyclerView listView = findViewById(R.id.category_list_view);
    listView.setLayoutManager(glm);
    listView.setHasFixedSize(true);
    categoryItemsAdapter = new CategoryListArrayAdapter();
    listView.setAdapter(categoryItemsAdapter);

    categoriesViewModel =
        new ViewModelProvider(this).get(CategoriesViewModel.class);
    observeCategoriesViewModel();

    createAddCategoryButton();
  }

  private void observeCategoriesViewModel() {
    categoriesViewModel.getUnreadCounts().observe(this,
        unreadCounts -> unreadCountsByCategoryId = unreadCounts);
    categoriesViewModel.getReadCounts().observe(this,
        readCounts -> readCountsByCategoryId = readCounts);
    categoriesViewModel.getCategories().observe(this, this::onCategoriesLoaded);
    categoriesViewModel.getNewlyAddedSmsCount().observe(this, this::onSmsSyncFinished);
    categoriesViewModel.getAddedCategoryName().observe(this, this::onCategoryAdded);
  }

  private void onCategoriesLoaded(List<Category> loadedCategories) {
    if (isFinishing() || isDestroyed()) {
      return;
    }
    categoryItemsAdapter.submitCategories(loadedCategories,
        unreadCountsByCategoryId, readCountsByCategoryId);
  }

  private void onSmsSyncFinished(Long newlyAddedSmsCount) {
    if (newlyAddedSmsCount == null) {
      return;
    }
    if (newlyAddedSmsCount > 0) {
      Toast.makeText(
          getBaseContext(),
          getString(R.string.new_sms_added_count, newlyAddedSmsCount.intValue()),
          Toast.LENGTH_SHORT
      ).show();
    }
    hideTitleProgressSpinner();
    categoriesViewModel.consumeNewlyAddedSmsCount();
  }

  private void onCategoryAdded(String addedCategoryName) {
    if (addedCategoryName == null) {
      return;
    }
    Toast.makeText(this, getString(R.string.successfully_added_category, addedCategoryName),
        Toast.LENGTH_SHORT).show();
    Log.i(GM_ADD_CAT,
        "Successfully added category: " + addedCategoryName);
    categoriesViewModel.consumeAddedCategoryName();
  }

  private void setupActionBar() {
    // set custom toolbar
    Toolbar toolbar = findViewById(R.id.toolbar);
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
    showTitleProgressSpinner();
    categoriesViewModel.syncLatestSms();
    categoriesViewModel.refresh();
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private void checkAndGetPermissions() {
    List<String> permissionsNeeded = new ArrayList<>();

    final List<String> permissionsList = new ArrayList<>();
    if (!addPermission(permissionsList, Manifest.permission.READ_SMS)) {
      permissionsNeeded.add(getString(R.string.permission_read_sms_entry));
    }
    if (!addPermission(permissionsList, Manifest.permission.READ_CONTACTS)) {
      permissionsNeeded.add(getString(R.string.permission_read_contacts_entry));
    }

    Log.d("GM/permNeed", permissionsNeeded.toString());
    Log.d("GM/permList", permissionsList.toString());

    if (!permissionsList.isEmpty()) {
      if (!permissionsNeeded.isEmpty()) {

        StringBuilder message = new StringBuilder(
            getString(R.string.permission_rationale_prefix));
        message.append(permissionsNeeded.get(0));
        for (int i = 1; i < permissionsNeeded.size(); i++) {
          message.append(getString(R.string.permission_rationale_separator))
              .append(permissionsNeeded.get(i));
        }

        showMessageOKCancel(message.toString(),
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(
                    MainActivity.this,
                    permissionsList.toArray(new String[0]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                );
              }
            }
        );
        return;
      }
      ActivityCompat.requestPermissions(
          this,
          permissionsList.toArray(new String[0]),
          REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
      );
      return;
    }
    init();
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  @RequiresApi(api = Build.VERSION_CODES.M)
  private boolean addPermission(List<String> permissionsList, String permission) {
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      permissionsList.add(permission);
      // Check for Rationale Option
      return ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
    }
    return true;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
    Log.d("GM/showPermMessage", message);
    new AlertDialog.Builder(MainActivity.this)
        .setMessage(message)
        .setPositiveButton(R.string.ok, okListener)
        .setNegativeButton(R.string.cancel, null)
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

  private void createAddCategoryButton() {
    FloatingActionButton fabAddCategory =
        findViewById(R.id.fab_add_category);
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

  void requestDeleteCategory(long categoryId) {
    categoriesViewModel.deleteCategory(categoryId);
  }

  @Override
  public void onDialogPositiveClick(DialogFragment dialog, Bundle oldArgs) {
    // User touched the dialog's positive button
    Log.i(GM_ADD_CAT, "User touched the add category dialog's add button");

    EditText categoryName = dialog.getDialog().findViewById(R.id.editTextAddCategory);
    ColorPickerView cpView =
        dialog.getDialog().findViewById(R.id.pick_category_color);

    if (categoryName.getText().toString().isEmpty()) {
      Toast.makeText(this, R.string.need_category_name, Toast.LENGTH_SHORT).show();
      Log.e(GM_ADD_CAT, "Need Category Name");
      return;
    }

    if ("EDIT".equals(oldArgs.getString("ACTION"))) {
      Map<String, String> updatedCategory = new HashMap<>();
      updatedCategory.put(DatabaseContract.Category.KEY_NAME,
          categoryName.getText().toString());
      updatedCategory.put(DatabaseContract.Category.KEY_VISIBILITY, String.valueOf(1));
      updatedCategory.put(DatabaseContract.Category.KEY_COLOR,
          String.valueOf(cpView.getSelectedColor()));
      updatedCategory.put(
          DatabaseContract.Category._ID,
          String.valueOf(oldArgs.getLong(DatabaseContract.Category._ID))
      );
      Boolean categoryUpdated = CategoryDao.updateCategory(this, updatedCategory);
      if (categoryUpdated) {
        Toast.makeText(this,
            getString(R.string.successfully_updated_category, categoryName.getText().toString()),
            Toast.LENGTH_SHORT).show();
        Log.i(GM_ADD_CAT, "Successfully updated category: " + categoryName.getText());
      }
      categoriesViewModel.refresh();
    } else {
      categoriesViewModel.addCategory(
          categoryName.getText().toString(),
          cpView.getSelectedColor()
      );
    }
  }

  @Override
  public void onDialogNegativeClick(DialogFragment dialog, Bundle oldArgs) {
    // User touched the dialog's negative button
    Log.i(GM_ADD_CAT, "User touched the add category dialog's cancel button");
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
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
        Toast.makeText(MainActivity.this, R.string.some_permission_denied, Toast.LENGTH_SHORT)
            .show();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void onDestroy() {
    pbCircle = null;
    glm = null;
    super.onDestroy();
  }
}
