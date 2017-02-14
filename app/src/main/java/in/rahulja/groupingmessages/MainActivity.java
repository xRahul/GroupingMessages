package in.rahulja.groupingmessages;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements AddCategoryFragment.AddCategoryDialogListener {

    public static final String GM_ADD_CAT = "GM/addCat";
    private SQLiteDatabase db;

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
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        Log.i(GM_ADD_CAT, "User touched the add category dialog's add button");

        EditText categoryName = (EditText) dialog.getDialog().findViewById(R.id.editTextAddCategory);

        // Create insert entries
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Category.KEY_NAME, categoryName.getText().toString());
        values.put(DatabaseContract.Category.KEY_VISIBILITY, 1);

        long addCatRowId = db.insert(DatabaseContract.Category.TABLE_NAME, null, values);

        if (addCatRowId == -1) {
            Toast.makeText(this, "Error while adding new category", Toast.LENGTH_SHORT).show();
            Log.e(GM_ADD_CAT, "Error while adding new category");
        } else {
            Toast.makeText(this, "Successfully added category: " + categoryName.getText(), Toast.LENGTH_SHORT).show();
            Log.i(GM_ADD_CAT, "Successfully added category: " + categoryName.getText());
            //refresh_ui();
        }

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
