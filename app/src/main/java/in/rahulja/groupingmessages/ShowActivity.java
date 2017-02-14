package in.rahulja.groupingmessages;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class ShowActivity extends AppCompatActivity {

    public static final String COUNT = "count";
    public static final String BUCKET_ID = "bucketId";
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    private String[] fromSms;
    private List<Map<String, String>> listOfSms;
    private List<Map<String, String>> listOfBuckets;
    private JSONObject jsonPredictData;
    private JSONObject jsonTrainData;
    private String androidId;
    private Boolean predicted = false;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        fromSms = getResources().getStringArray(R.array.from_sms);
        String[] bucketNames = getResources().getStringArray(R.array.buckets);
        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        listOfBuckets = new ArrayList<>();
        for (String bucket_name : bucketNames) {
            Map<String, String> tempBucket = new HashMap<>();
            tempBucket.put("name", bucket_name);
            tempBucket.put(COUNT, "0");
            listOfBuckets.add(tempBucket);
        }
        Log.d("SM/lb", listOfBuckets.toString());

//        try {
//            saveAllSms();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        readAllSms();
        bucketizeSmsFromApi();

    }

    private void bucketizeSmsFromApi() {

        createPredictData();

        updateSmsListFromPredictions();

        if (!predicted) {
            for (int i = 0; i < listOfSms.size(); i++) {
                Map<String, String> tempSms = new HashMap<>();
                tempSms.put("from", listOfSms.get(i).get("from"));
                tempSms.put("body", listOfSms.get(i).get("body"));
                tempSms.put("time", listOfSms.get(i).get("time"));
                tempSms.put(BUCKET_ID, String.valueOf(0));
                listOfSms.set(i, tempSms);


                Map<String, String> tempBucket = new HashMap<>();
                tempBucket.put("name", listOfBuckets.get(0).get("name"));
                tempBucket.put(COUNT, String.valueOf(Integer.parseInt(listOfBuckets.get(0).get(COUNT)) + 1));
                listOfBuckets.set(0, tempBucket);

            }
        }
        Log.d("SM/lb", listOfSms.toString());
    }

    private void updateSmsListFromPredictions() {
        new HttpAsyncTask1().execute("http://www.google.com/SmartMessages.php");
    }

    private void hitTrainApi() {
        new HttpAsyncTask2().execute("http://www.google.com/SmartMessages.php");
    }


    public String POST(String urlStr, JSONObject data) {
        try {
            BufferedReader br;
            URL url = new URL(urlStr); //Enter URL here
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
            httpURLConnection.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
            httpURLConnection.connect();

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(data.toString());
            wr.flush();
            wr.close();

            if (200 <= httpURLConnection.getResponseCode() && httpURLConnection.getResponseCode() <= 299) {
                br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            Log.d("qwqwqwqw", sb.toString());
            Log.d("asasasas", androidId);
            Log.d("mmmmmmmm", data.toString());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

                    ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
                    mViewPager.setAdapter(mSectionsPagerAdapter);

                    TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
                    tabLayout.setupWithViewPager(mViewPager);
                }
            });

            return sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void createPredictData() {
        try {
            JSONObject mainObj = new JSONObject();
            mainObj.put("uid", androidId);
            mainObj.put("predict", "True");

            JSONArray dataArray = new JSONArray();

            for (int i = 0; i < listOfSms.size(); i++) {
                JSONObject tempSms = new JSONObject();
                tempSms.put("msg_from", listOfSms.get(i).get("from"));
                tempSms.put("msg", listOfSms.get(i).get("body"));
                dataArray.put(tempSms);
            }

            mainObj.put("data", dataArray);
            jsonPredictData = mainObj;

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createTrainData(Map<String, String> trainMap) {
        try {
            JSONObject mainObj = new JSONObject();
            mainObj.put("uid", androidId);
            mainObj.put("predict", "False");

            JSONArray dataArray = new JSONArray();

            JSONObject tempSms = new JSONObject();
            tempSms.put("msg_from", trainMap.get("from"));
            tempSms.put("msg", trainMap.get("body"));
            tempSms.put("bucket_id", trainMap.get(BUCKET_ID));
            dataArray.put(tempSms);

            mainObj.put("data", dataArray);
            jsonTrainData = mainObj;

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void readAllSms() {
        listOfSms = new ArrayList<>();
        try {
            Uri uri = Uri.parse(SMS_URI_INBOX);
            String[] projection = new String[]{"_id", "address", "body", "date"};
            StringBuilder searchString = new StringBuilder();
            searchString.append("read = 0 AND address IN (");
            for (int i = 0; i <= fromSms.length - 1; i++) {
                if (i == fromSms.length - 1) {
                    searchString.append("'").append(fromSms[i]).append("')");
                } else {
                    searchString.append("'").append(fromSms[i]).append("', ");
                }
            }
            Log.d("SM/searchString", searchString.toString());
            Cursor cur = getContentResolver().query(uri, projection, searchString.toString(), null, "date desc");
            if (cur != null && cur.moveToFirst()) {
                int indexAddress = cur.getColumnIndex("address");
                int indexBody = cur.getColumnIndex("body");
                int indexDate = cur.getColumnIndex("date");
                do {
                    String strAddress = cur.getString(indexAddress);
                    String strBody = cur.getString(indexBody);
                    long longDate = cur.getLong(indexDate);

                    Map<String, String> tempSms = new HashMap<>();
                    tempSms.put("from", strAddress);
                    tempSms.put("body", strBody);
                    tempSms.put("time", String.valueOf(longDate));

                    listOfSms.add(tempSms);

                } while (cur.moveToNext());

                Log.d("SM/SizeMsgs", String.valueOf(listOfSms.size()));

                if (!cur.isClosed()) {
                    cur.close();
                }
            }
        } catch (SQLiteException ex) {
            Log.d("SQLiteException", ex.getMessage());
        }
    }

    // function to get all sms from device on csv
    private void saveAllSms() throws IOException {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "AnalysisData.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            writer = new CSVWriter(new FileWriter(filePath, true));
        } else {
            writer = new CSVWriter(new FileWriter(filePath));
        }


        listOfSms = new ArrayList<>();
        try {
            Uri uri = Uri.parse(SMS_URI_INBOX);
            String[] projection = new String[]{"_id", "address", "body", "date"};
            StringBuilder searchString = new StringBuilder();
            searchString.append("read = 0 AND address IN (");
            for (int i = 0; i <= fromSms.length - 1; i++) {
                if (i == fromSms.length - 1) {
                    searchString.append("'").append(fromSms[i]).append("')");
                } else {
                    searchString.append("'").append(fromSms[i]).append("', ");
                }
            }
            Log.d("SM/searchString", searchString.toString());
            Cursor cur = getContentResolver().query(uri, projection, null, null, "date desc");
            if (cur != null && cur.moveToFirst()) {
                int indexAddress = cur.getColumnIndex("address");
                int indexBody = cur.getColumnIndex("body");
                int indexDate = cur.getColumnIndex("date");
                do {
                    String strAddress = cur.getString(indexAddress);
                    String strBody = cur.getString(indexBody);
                    long longDate = cur.getLong(indexDate);

                    String[] data = {
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("en")).format(longDate),
                            strAddress,
                            strBody
                    };
                    writer.writeNext(data);

                } while (cur.moveToNext());

                Log.d("SM/SizeMsgs", String.valueOf(listOfSms.size()));

                if (!cur.isClosed()) {
                    cur.close();
                }
            }
        } catch (SQLiteException ex) {
            Log.d("SQLiteException", ex.getMessage());
        }
        writer.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 111 && resultCode == RESULT_OK) {
            Integer bucketId = data.getIntExtra(BUCKET_ID, 0);
            Integer smsPosition = data.getIntExtra("smsPosition", 0);
            Integer oldBucketId = data.getIntExtra("old_bucket_id", 0);

            Log.d("SH/asdf", String.valueOf(bucketId) + String.valueOf(smsPosition) + String.valueOf(oldBucketId));

            Map<String, String> tempSms = new HashMap<>();
            tempSms.put("from", listOfSms.get(smsPosition).get("from"));
            tempSms.put("body", listOfSms.get(smsPosition).get("body"));
            tempSms.put("time", listOfSms.get(smsPosition).get("time"));
            tempSms.put(BUCKET_ID, String.valueOf(bucketId));
            listOfSms.set(smsPosition, tempSms);

            createTrainData(tempSms);
            hitTrainApi();

            Map<String, String> tempBucket = new HashMap<>();
            tempBucket.put("name", listOfBuckets.get(bucketId).get("name"));
            tempBucket.put(COUNT, String.valueOf(Integer.parseInt(listOfBuckets.get(bucketId).get(COUNT)) + 1));
            listOfBuckets.set(bucketId, tempBucket);
            tempBucket = new HashMap<>();
            tempBucket.put("name", listOfBuckets.get(oldBucketId).get("name"));
            tempBucket.put(COUNT, String.valueOf(Integer.parseInt(listOfBuckets.get(oldBucketId).get(COUNT)) - 1));
            listOfBuckets.set(oldBucketId, tempBucket);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_BUCKETS_LIST = "temp_buckets_list";
        private static final String ARG_SMS_LIST = "temp_sms_list";

        public PlaceholderFragment() {
            // empty as it's constructor
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, List<Map<String, String>> tempListOfSms, List<Map<String, String>> tempListOfBuckets) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putSerializable(ARG_SMS_LIST, (Serializable) tempListOfSms);
            Log.d("QWERTY", tempListOfBuckets.toString());
            args.putSerializable(ARG_BUCKETS_LIST, (Serializable) tempListOfBuckets);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int i = getArguments().getInt(ARG_SECTION_NUMBER);
            if (i == 0) {
                return createBucketsView(inflater, container);
            } else if (i == 1) {
                return createSmsView(inflater, container);
            }

            return null;
        }

        private View createBucketsView(LayoutInflater inflater, ViewGroup container) {
            BucketListArrayAdapter bucketItemsAdapter = new BucketListArrayAdapter(this.getContext(), (List<Map<String, String>>) getArguments().getSerializable(ARG_BUCKETS_LIST));
            View rootView = inflater.inflate(R.layout.buckets_tab, container, false);
            RecyclerView listView = (RecyclerView) rootView.findViewById(R.id.buckets_list_view);
            listView.setLayoutManager(new LinearLayoutManager(this.getContext()));
            listView.setHasFixedSize(true);
            listView.setAdapter(bucketItemsAdapter);
            return rootView;
        }


        private View createSmsView(LayoutInflater inflater, ViewGroup container) {
            SmsListArrayAdapter smsListAdapter = new SmsListArrayAdapter(this.getContext(), (List<Map<String, String>>) getArguments().getSerializable(ARG_SMS_LIST));
            View rootView = inflater.inflate(R.layout.sms_tab, container, false);
            RecyclerView listView = (RecyclerView) rootView.findViewById(R.id.sms_list_view);
            listView.setLayoutManager(new LinearLayoutManager(this.getContext()));
            listView.setHasFixedSize(true);
            listView.setAdapter(smsListAdapter);
            return rootView;
        }
    }

    public class HttpAsyncTask1 extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            predicted = false;
            return POST(urls[0], jsonPredictData);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(getBaseContext(), "prediction failed!", Toast.LENGTH_LONG).show();
            } else {
                try {
                    JSONArray predict = new JSONArray(result);
                    for (int i = 0; i < listOfSms.size(); i++) {
                        Map<String, String> tempSms = new HashMap<>();
                        tempSms.put("from", listOfSms.get(i).get("from"));
                        tempSms.put("body", listOfSms.get(i).get("body"));
                        tempSms.put("time", listOfSms.get(i).get("time"));
                        tempSms.put(BUCKET_ID, String.valueOf(predict.get(i)));
                        listOfSms.set(i, tempSms);

                        Map<String, String> tempBucket = new HashMap<>();
                        tempBucket.put("name", listOfBuckets.get(predict.getInt(i)).get("name"));
                        tempBucket.put(COUNT, String.valueOf(Integer.parseInt(listOfBuckets.get(predict.getInt(i)).get(COUNT)) + 1));
                        listOfBuckets.set(predict.getInt(i), tempBucket);

                        tempBucket = new HashMap<>();
                        tempBucket.put("name", listOfBuckets.get(0).get("name"));
                        tempBucket.put(COUNT, String.valueOf(Integer.parseInt(listOfBuckets.get(0).get(COUNT)) - 1));
                        listOfBuckets.set(0, tempBucket);

                        predicted = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class HttpAsyncTask2 extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return POST(urls[0], jsonTrainData);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(getBaseContext(), "train failed!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), "train success!", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position, listOfSms, listOfBuckets);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Buckets";
            } else if (position == 1) {
                return "SMS";
            }
            return null;
        }
    }
}