package in.rahulja.groupingmessages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements AddCategoryFragment.AddCategoryDialogListener {

    public static final String GM_ADD_CAT = "GM/addCat";
    private SQLiteDatabase db;
    private List<Map<String, String>> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabAddCategory = (FloatingActionButton) findViewById(R.id.fab_add_category);
        fabAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new AddCategoryFragment();
                newFragment.show(getSupportFragmentManager(), "add_category_tag");
            }
        });

        // Create new helper
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        // Get the database. If it does not exist, this is where it will
        // also be created.
        db = dbHelper.getWritableDatabase();

        create_ui();
    }

    private void create_ui() {
        refresh_ui();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        Log.i(GM_ADD_CAT, "User touched the add category dialog's add button");

        EditText categoryName = (EditText) dialog.getDialog().findViewById(R.id.editTextAddCategory);
        ColorPickerView cpView = (ColorPickerView) dialog.getDialog().findViewById(R.id.pick_category_color);

        // Create insert entries
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Category.KEY_NAME, categoryName.getText().toString());
        values.put(DatabaseContract.Category.KEY_VISIBILITY, 1);
        values.put(DatabaseContract.Category.KEY_COLOR, cpView.getSelectedColor());

        long addCatRowId = db.insert(DatabaseContract.Category.TABLE_NAME, null, values);

        if (addCatRowId == -1) {
            Toast.makeText(this, "Error while adding new category", Toast.LENGTH_SHORT).show();
            Log.e(GM_ADD_CAT, "Error while adding new category");
        } else {
            Toast.makeText(this, "Successfully added category: " + categoryName.getText(), Toast.LENGTH_SHORT).show();
            Log.i(GM_ADD_CAT, "Successfully added category: " + categoryName.getText());
            refresh_ui();
        }

    }

    private void refresh_ui() {
        getDataFromDb();

        CategoryListArrayAdapter categoryItemsAdapter = new CategoryListArrayAdapter(this, categoryList);
        RecyclerView listView = (RecyclerView) findViewById(R.id.category_list_view);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setHasFixedSize(true);
        listView.setAdapter(categoryItemsAdapter);

    }

    private void getDataFromDb() {
        // Filter results WHERE "title" = 'My Title'
        String selection = DatabaseContract.Category.KEY_VISIBILITY + " = ?";
        String[] selectionArgs = {"1"};
        
        Cursor cursor = db.query(
                DatabaseContract.Category.TABLE_NAME,           // The table to query
                DatabaseContract.Category.KEY_ARRAY,            // The columns to return
                selection,                                      // The columns for the WHERE clause
                selectionArgs,                                  // The values for the WHERE clause
                null,                                           // don't group the rows
                null,                                           // don't filter by row groups
                DatabaseContract.Category.DEFAULT_SORT_ORDER    // The sort order
        );

        if (categoryList == null) {
            categoryList = new ArrayList<>();
        } else {
            categoryList.clear();
        }

        while (cursor.moveToNext()) {
            long categoryId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category._ID));
            String categoryName = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_NAME));
            String categoryColor = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseContract.Category.KEY_COLOR));

            Map<String, String> categoryMap = new HashMap<>();
            categoryMap.put("name", categoryName);
            categoryMap.put("color", categoryColor);
            categoryMap.put("id", String.valueOf(categoryId));
            categoryList.add(categoryMap);
        }
        cursor.close();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
        Log.i(GM_ADD_CAT, "User touched the add category dialog's cancel button");
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
