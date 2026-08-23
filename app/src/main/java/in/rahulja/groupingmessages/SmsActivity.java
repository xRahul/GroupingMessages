package in.rahulja.groupingmessages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import in.rahulja.groupingmessages.db.CategoryDao;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Sms;
import in.rahulja.groupingmessages.vm.AppExecutors;
import in.rahulja.groupingmessages.vm.SmsListViewModel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmsActivity extends AppCompatActivity {

  public static final String CATEGORY_ID = "category_id";
  private static final int CHANGE_CATEGORY_REQUEST_CODE = 111;

  private LinearLayoutManager llm;
  private ProgressBar pbCircle;
  private RecyclerView listView;
  private long categoryId;
  private List<Sms> currentSms;
  private SmsListArrayAdapter smsItemsAdapter;
  private SmsListViewModel smsListViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sms);
    setupActionBar();

    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
      Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return WindowInsetsCompat.CONSUMED;
    });

    pbCircle = findViewById(R.id.progressBarCircle);
    listView = findViewById(R.id.sms_list_view);

    categoryId = Long.parseLong(getIntent().getStringExtra(CATEGORY_ID));

    llm = new LinearLayoutManager(this);
    listView.setLayoutManager(llm);
    listView.setHasFixedSize(true);
    smsItemsAdapter = new SmsListArrayAdapter(this, this::onSmsRemovedByUser);
    listView.setAdapter(smsItemsAdapter);
    setSwipeForRecyclerView();

    smsListViewModel = new ViewModelProvider(this).get(SmsListViewModel.class);
    smsListViewModel.getSms(categoryId).observe(this, this::onSmsLoaded);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    showTitleProgressSpinner();
    smsListViewModel.refresh(categoryId);
  }

  private void onSmsLoaded(List<Sms> loadedSms) {
    if (isFinishing() || isDestroyed()) {
      return;
    }
    AppExecutors.disk(() -> {

      Set<String> addressSet = new HashSet<>();
      for (Sms sms : loadedSms) {
        addressSet.add(sms.getAddress());
      }

      Map<String, String> contactNames = ExternalContentBridge.getContactNames(this, addressSet);

      Map<Long, String> categories = new HashMap<>();
      for (Map<String, String> category : CategoryDao.getAllVisibleCategories(this)) {
        categories.put(
            Long.parseLong(category.get(DatabaseContract.Category._ID)),
            category.get(DatabaseContract.Category.KEY_NAME)
        );
      }

      AppExecutors.main(() -> {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        currentSms = loadedSms;
        smsItemsAdapter.submitSms(loadedSms, contactNames, categories);
      });
    });
  }

  private void setSwipeForRecyclerView() {

    SwipeUtil swipeHelper = new SwipeUtil(0, ItemTouchHelper.LEFT, this) {
      @Override
      public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int swipedPosition = viewHolder.getAdapterPosition();
        SmsListArrayAdapter adapter = (SmsListArrayAdapter) listView.getAdapter();
        if (adapter != null) {
          adapter.pendingRemoval(swipedPosition);
        }
      }

      @Override
      public int getSwipeDirs(@NonNull RecyclerView recyclerView,
          RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        SmsListArrayAdapter adapter = (SmsListArrayAdapter) listView.getAdapter();
        if (adapter != null && adapter.isPendingRemoval(position)) {
          return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
      }
    };

    ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(swipeHelper);
    mItemTouchHelper.attachToRecyclerView(listView);

    //set swipe label
    swipeHelper.setLeftSwipeLabel(getString(R.string.delete));
    //set swipe background-Color
    swipeHelper.setLeftColorCode(ContextCompat.getColor(this, android.R.color.holo_red_dark));
  }

  private void onSmsRemovedByUser(Sms sms) {
    smsListViewModel.swipeDelete(sms, categoryId);
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

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent receivedIntent) {
    super.onActivityResult(requestCode, resultCode, receivedIntent);
    if (requestCode == CHANGE_CATEGORY_REQUEST_CODE && resultCode == RESULT_OK) {
      final long newCategoryId = Long.parseLong(
          receivedIntent.getStringExtra(CATEGORY_ID)
      );
      final int smsListPosition = Integer.parseInt(
          receivedIntent.getStringExtra("sms_list_position")
      );
      if (receivedIntent.getExtras() != null) {
        Log.d("GM/choseCat", receivedIntent.getExtras().toString());
      }

      if (currentSms == null || smsListPosition >= currentSms.size()) {
        return;
      }
      final long smsId = currentSms.get(smsListPosition).getId();

      showTitleProgressSpinner();
      AppExecutors.disk(() -> asyncRetrainAllSms(smsId, newCategoryId));
    }
  }

  private void asyncRetrainAllSms(long smsId, long newCategoryId) {

    // re-read the full row instead of reusing the possibly stale displayed map
    Map<String, String> trainedSms = SmsDao.getById(getBaseContext(), smsId);
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

    SmsDao.updateSmsData(getBaseContext(), trainedSms);

    List<Map<String, String>> retrainedSmsList = TrainSms.retrainExistingSms(
        getBaseContext(),
        trainedSms
    );

    final long numRetrainedSms = SmsDao.storeReTrainedSms(
        getBaseContext(),
        retrainedSmsList
    );

    AppExecutors.main(() -> {
      if (isFinishing() || isDestroyed()) {
        return;
      }
      Toast.makeText(
          getBaseContext(),
          getString(R.string.trained_sms_count, numRetrainedSms),
          Toast.LENGTH_SHORT
      ).show();
      hideTitleProgressSpinner();
      smsListViewModel.refresh(categoryId);
    });
  }

  @Override
  public void onDestroy() {
    llm = null;
    pbCircle = null;
    listView = null;
    smsItemsAdapter = null;
    currentSms = null;
    super.onDestroy();
  }
}
