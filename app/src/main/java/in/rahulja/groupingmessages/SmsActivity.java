package in.rahulja.groupingmessages;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmsActivity extends AppCompatActivity {

  public static final String KEY_FROM = "from";
  public static final String KEY_CATEGORY_NAME = "category_name";
  public static final String CATEGORY_ID = "category_id";
  LinearLayoutManager llm = new LinearLayoutManager(this);
  private List<Map<String, String>> smsList;
  private long categoryId;
  private Map<String, String> categories;
  private ProgressBar pbCircle;
  private RecyclerView listView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sms);
    setupActionBar();

    pbCircle = (ProgressBar) findViewById(R.id.progressBarCircle);

    categoryId = Long.parseLong(getIntent().getStringExtra(CATEGORY_ID));
    smsList = new ArrayList<>();
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    init();
  }

  private void init() {
    getDataInBackground();
    drawUi();
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

  private void getDataInBackground() {
    showTitleProgressSpinner();
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        loadAllCategories();
        Log.d("GM/ChooseCatLoad", "categoriesLoaded" + categories.toString());
        getCategorySmsData();

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

  private void drawUi() {
    int positionIndex = llm.findFirstVisibleItemPosition();
    SmsListArrayAdapter smsItemsAdapter = new SmsListArrayAdapter(this, smsList);
    listView = (RecyclerView) findViewById(R.id.sms_list_view);
    listView.setLayoutManager(llm);
    listView.setHasFixedSize(true);
    listView.setAdapter(smsItemsAdapter);
    setSwipeForRecyclerView();
    if (smsList.size() > positionIndex) {
      llm.scrollToPosition(positionIndex);
    } else {
      llm.scrollToPosition(smsList.size() - 1);
    }
  }

  private void setSwipeForRecyclerView() {

    SwipeUtil swipeHelper = new SwipeUtil(0, ItemTouchHelper.LEFT, this) {
      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int swipedPosition = viewHolder.getAdapterPosition();
        SmsListArrayAdapter adapter = (SmsListArrayAdapter) listView.getAdapter();
        adapter.pendingRemoval(swipedPosition);
      }

      @Override
      public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        SmsListArrayAdapter adapter = (SmsListArrayAdapter) listView.getAdapter();
        if (adapter.isPendingRemoval(position)) {
          return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
      }
    };

    ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(swipeHelper);
    mItemTouchHelper.attachToRecyclerView(listView);

    //set swipe label
    swipeHelper.setLeftSwipeLable("Delete");
    //set swipe background-Color
    swipeHelper.setLeftcolorCode(ContextCompat.getColor(this, android.R.color.holo_red_dark));
  }

  private void getCategorySmsData() {

    smsList = DatabaseBridge.getVisibleSmsFromCategory(this, categoryId);

    Log.d("GM/GotFilteredSMS", String.valueOf(smsList.size()));

    Set<String> addressSet = new HashSet<>();

    for (int i = 0; i < smsList.size(); i++) {
      if (!"0".equals(String.valueOf(smsList.get(i).get(DatabaseContract.Sms.KEY_PERSON)))) {
        addressSet.add(smsList.get(i).get(DatabaseContract.Sms.KEY_ADDRESS));
      }
    }

    Log.d("GM/addressSet", addressSet.toString());

    Map<String, String> contactNames = ExternalContentBridge.getContactNames(this, addressSet);

    for (int i = 0; i < smsList.size(); i++) {
      Map<String, String> tempSms = smsList.get(i);
      String fromString = tempSms.get(DatabaseContract.Sms.KEY_ADDRESS);
      if (!"0".equals(String.valueOf(tempSms.get(DatabaseContract.Sms.KEY_PERSON)))) {
        fromString = contactNames.get(fromString);
      }

      tempSms.put(KEY_FROM, fromString);
      tempSms.put(KEY_CATEGORY_NAME, categories.get(
          String.valueOf(tempSms.get(DatabaseContract.Sms.KEY_CATEGORY_ID))
      ));

      smsList.set(i, tempSms);
    }
  }

  private void loadAllCategories() {

    List<Map<String, String>> allCategories = DatabaseBridge.getAllVisibleCategories(this);
    categories = new HashMap<>();
    for (Map<String, String> category : allCategories) {
      categories.put(
          category.get(DatabaseContract.Category._ID),
          category.get(DatabaseContract.Category.KEY_NAME)
      );
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent receivedIntent) {
    super.onActivityResult(requestCode, resultCode, receivedIntent);
    if (requestCode == 111 && resultCode == RESULT_OK) {
      final long newCategoryId = Long.parseLong(
          receivedIntent.getStringExtra(CATEGORY_ID)
      );
      final int smsListPosition = Integer.parseInt(
          receivedIntent.getStringExtra("sms_list_position")
      );
      Log.d("GM/choseCat", receivedIntent.getExtras().toString());

      final Map<String, String> trainedSms = smsList.get(smsListPosition);
      trainedSms.put(
          DatabaseContract.Sms.KEY_CATEGORY_ID,
          String.valueOf(newCategoryId)
      );
      trainedSms.put(
          DatabaseContract.Sms.KEY_SIMILAR_TO,
          trainedSms.get(DatabaseContract.Sms._ID)
      );
      trainedSms.put(
          DatabaseContract.Sms.KEY_SIM_SCORE,
          String.valueOf(1.0)
      );

      showTitleProgressSpinner();
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          asyncRetrainAllSms(trainedSms);
        }
      };
      new Thread(runnable).start();
    }
  }

  private void asyncRetrainAllSms(Map<String, String> trainedSms) {
    DatabaseBridge.updateSmsData(getBaseContext(), trainedSms);

    List<Map<String, String>> retrainedSmsList = TrainSms.retrainExistingSms(
        getBaseContext(),
        trainedSms
    );

    final long numRetrainedSms = DatabaseBridge.storeReTrainedSms(
        getBaseContext(),
        retrainedSmsList
    );

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(
            getBaseContext(),
            "Trained " + numRetrainedSms + " Sms",
            Toast.LENGTH_SHORT
        ).show();
        getDataInBackground();
        hideTitleProgressSpinner();
      }
    });
  }

  @Override
  public void onDestroy() {
    llm = null;
    smsList = null;
    categories = null;
    pbCircle = null;
    listView = null;
    super.onDestroy();
  }
}
