package in.rahulja.groupingmessages;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import java.util.List;
import java.util.Map;
import in.rahulja.groupingmessages.db.CategoryDao;

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
    setupWindowInsets();

    setTitle("Choose category");
    oldIntent = getIntent();

    categories = CategoryDao.getAllVisibleCategories(this);

    categoryListView = findViewById(R.id.activity_choose_category_list_view);

    setCategoryListView();

    setCategoryListViewClickListener();
  }

  private void setupWindowInsets() {
    View root = findViewById(R.id.activity_choose_bucket);
    final int baseLeft = root.getPaddingLeft();
    final int baseTop = root.getPaddingTop();
    final int baseRight = root.getPaddingRight();
    final int baseBottom = root.getPaddingBottom();
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
      Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(baseLeft + bars.left, baseTop + bars.top,
          baseRight + bars.right, baseBottom + bars.bottom);
      return WindowInsetsCompat.CONSUMED;
    });
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

  @Override
  public void onDestroy() {
    oldIntent = null;
    categories = null;
    categoryListView = null;
    super.onDestroy();
  }
}
