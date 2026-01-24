package in.rahulja.groupingmessages;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Map;

class CategoryListItemHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener, View.OnLongClickListener {

  private static final String COUNT_UNREAD = "count_unread";
  private static final String COUNT_READ = "count_read";
  private static final String CATEGORY_ID = "category_id";
  private static final String EDIT_CATEGORY_TAG = "EDIT_CATEGORY_TAG";
  private final Context context;
  private final TextView categoryNameTextView;
  private final TextView categoryUnreadCountTextView;
  private final TextView categoryReadCountTextView;
  private final RelativeLayout categoryListViewParent;
  private String categoryId;
  private String categoryName;
  private String categoryColor;

  CategoryListItemHolder(View itemView) {
    super(itemView);

    context = itemView.getContext();

    categoryNameTextView = itemView.findViewById(R.id.category_name_textview);
    categoryUnreadCountTextView = itemView.findViewById(R.id.category_unread_count_textview);
    categoryReadCountTextView = itemView.findViewById(R.id.category_read_count_textview);
    categoryListViewParent = itemView.findViewById(R.id.category_list_parent);

    itemView.setOnClickListener(this);
    itemView.setOnLongClickListener(this);
  }

  @SuppressWarnings("WeakerAccess")
  public void bindCategory(Map<String, String> category) {
    categoryId = category.get(DatabaseContract.Category._ID);
    categoryName = category.get(DatabaseContract.Category.KEY_NAME);
    categoryColor = category.get(DatabaseContract.Category.KEY_COLOR);
    categoryNameTextView.setText(category.get(DatabaseContract.Category.KEY_NAME));
    categoryUnreadCountTextView.setText(category.get(COUNT_UNREAD));
    categoryReadCountTextView.setText(category.get(COUNT_READ));
    if (category.get(DatabaseContract.Category.KEY_COLOR) != null) {
      categoryListViewParent.setBackgroundColor(
          Integer.parseInt(category.get(DatabaseContract.Category.KEY_COLOR))
      );
    }
  }

  @Override
  public void onClick(View v) {
    final Intent intent;
    intent = new Intent(context, SmsActivity.class);
    intent.putExtra(CATEGORY_ID, categoryId);
    context.startActivity(intent);
  }

  @Override
  public boolean onLongClick(View view) {
    Log.d("GM/catLongClick", "long clicked");
    PopupMenu menu = new PopupMenu(context, view);
    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        return onCategoryMenuItemClick(item);
      }
    });
    menu.inflate(R.menu.category_long_press_menu);
    menu.show();
    return true;
  }

  private boolean onCategoryMenuItemClick(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.category_popup_edit_item) {
      DialogFragment newFragment = new AddCategoryFragment();

      Bundle args = new Bundle();
      args.putString("ACTION", "EDIT");
      args.putLong(DatabaseContract.Category._ID, Long.parseLong(categoryId));
      args.putString(DatabaseContract.Category.KEY_NAME, categoryName);
      args.putInt(DatabaseContract.Category.KEY_COLOR, Integer.parseInt(categoryColor));

      newFragment.setArguments(args);
      newFragment.show(((MainActivity) context).getSupportFragmentManager(), EDIT_CATEGORY_TAG);
    } else if (id == R.id.category_popup_delete_item && !"1".equals(categoryId)) {
      DatabaseBridge.deleteCategory(context, Long.parseLong(categoryId));
      ((MainActivity) context).onPostResume();
    } else if (id == R.id.category_popup_delete_item && "1".equals(categoryId)) {
      Toast.makeText(context, "Cannot Delete Unknown Category", Toast.LENGTH_SHORT).show();
    } else if (id == R.id.category_popup_all_read_item) {
      DatabaseBridge.setAllCategorySmsAsRead(context, categoryId);
      ((MainActivity) context).onPostResume();
    } else if (id == R.id.category_popup_delete_all_sms) {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);

      builder.setTitle("Delete All Sms in " + categoryName + "?");
      builder.setMessage(
          "All sms from this category " +
              "will be deleted from this application's database. " +
              "Real SMS are not affected"
      );
      builder.setPositiveButton("Yes",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                int which) {

              Runnable runnable = new Runnable() {
                @Override
                public void run() {
                  DatabaseBridge.deleteAllSmsOfCategoryById(context, Long.parseLong(categoryId));
                  ((MainActivity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      ((MainActivity) context).onPostResume();
                    }
                  });
                }
              };
              new Thread(runnable).start();
            }
          });
      builder.setNegativeButton("No",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                int which) {
              // do nothing
            }
          });
      builder.show();
    }
    return true;
  }
}
