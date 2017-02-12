package in.rahulja.groupingmessages;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooseBucketActivity extends AppCompatActivity {

    private Intent oldIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_bucket);

        setTitle("Choose bucket");
        oldIntent = getIntent();

        String[] bucketNames = getResources().getStringArray(R.array.buckets);

        ListView lv = (ListView) findViewById(R.id.activity_choose_bucket_list_view);

        // Create a List from String Array elements
        final List<String> bucketsList = new ArrayList<>(Arrays.asList(bucketNames));

        // Create an ArrayAdapter from List
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_list_item_1, bucketsList);

        // DataBind ListView with items from ArrayAdapter
        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("bucketId", position);
                resultIntent.putExtra("old_bucket_id", oldIntent.getIntExtra("old_bucket_id", 0));
                resultIntent.putExtra("smsPosition", oldIntent.getIntExtra("sms_position", 0));
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}
