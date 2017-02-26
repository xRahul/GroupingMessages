package in.rahulja.groupingmessages;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class ChangeCategoryActivity extends AppCompatActivity {

    public static final String CATEGORY_ID = "category_id";
    public static final String SMS_ID = "sms_id";
    public static final String SMS_LIST_POSITION = "sms_list_position";
    private Intent oldIntent;
    private List<Map<String, String>> categories;
    private ListView categoryListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_category);

        setTitle("Choose category");
        oldIntent = getIntent();

        categories = DatabaseBridge.getAllVisibleCategories(this);

        categoryListView = (ListView) findViewById(R.id.activity_choose_category_list_view);

        setCategoryListView();

        setCategoryListViewClickListener();
    }

    private void setCategoryListViewClickListener() {
        categoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                returnResult(view);
            }
        });
    }

    private void returnResult(View view) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(
                CATEGORY_ID,
                ((TextView) view.findViewById(R.id.id_category_textview))
                        .getText().toString()
        );
        resultIntent.putExtra(
                SMS_ID,
                String.valueOf(oldIntent.getLongExtra(SMS_ID, 0))
        );
        resultIntent.putExtra(
                SMS_LIST_POSITION,
                String.valueOf(oldIntent.getIntExtra(SMS_LIST_POSITION, 0))
        );
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void setCategoryListView() {
        String[] from = {
                DatabaseContract.Category._ID,
                DatabaseContract.Category.KEY_NAME
        };
        int[] to = {
                R.id.id_category_textview,
                R.id.name_category_textview
        };

        SimpleAdapter arrayAdapter = new SimpleAdapter(
                this,
                categories,
                R.layout.choose_category_list_item,
                from,
                to
        );

        // DataBind ListView with items from ArrayAdapter
        categoryListView.setAdapter(arrayAdapter);
    }
}
