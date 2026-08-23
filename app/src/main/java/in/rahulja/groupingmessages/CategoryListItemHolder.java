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
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Category;

class CategoryListItemHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener, View.OnLongClickListener {

  private static final String CATEGORY_ID = "category_id";
  private static final long UNKNOWN_CATEGORY_ID = 1L;
  private static final String EDIT_CATEGORY_TAG = "EDIT_CATEGORY_TAG";
  private final Context context;
  private final TextView categoryNameTextView;
  private final TextView categoryUnreadCountTextView;
  private final TextView categoryReadCountTextView;
  private final RelativeLayout categoryListViewParent;
  private Category category;

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
  public void bindCategory(Category category, String unreadCountStr, String readCountStr) {
    this.category = category;
    categoryNameTextView.setText(category.getName());

    categoryUnreadCountTextView.setText(unreadCountStr);
    try {
      int unreadCount = Integer.parseInt(unreadCountStr);
      String contentDescription = context.getResources().getQuantityString(
          R.plurals.unread_messages_count, unreadCount, unreadCount);
      categoryUnreadCountTextView.setContentDescription(contentDescription);
    } catch (NumberFormatException e) {
      // ignore
    }

    categoryReadCountTextView.setText(readCountStr);
    try {
      int readCount = Integer.parseInt(readCountStr);
      String contentDescription = context.getResources().getQuantityString(
          R.plurals.read_messages_count, readCount, readCount);
      categoryReadCountTextView.setContentDescription(contentDescription);
    } catch (NumberFormatException e) {
      // ignore
    }

    categoryListViewParent.setBackgroundColor(category.getColor());
  }

  @Override
  public void onClick(View v) {
    final Intent intent;
    intent = new Intent(context, SmsActivity.class);
    intent.putExtra(CATEGORY_ID, String.valueOf(category.getId()));
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
      args.putLong(DatabaseContract.Category._ID, category.getId());
      args.putString(DatabaseContract.Category.KEY_NAME, category.getName());
      args.putInt(DatabaseContract.Category.KEY_COLOR, category.getColor());

      newFragment.setArguments(args);
      newFragment.show(((MainActivity) context).getSupportFragmentManager(), EDIT_CATEGORY_TAG);
    } else if (id == R.id.category_popup_delete_item && category.getId() != UNKNOWN_CATEGORY_ID) {
      ((MainActivity) context).requestDeleteCategory(category.getId());
    } else if (id == R.id.category_popup_delete_item) {
      Toast.makeText(context,
          context.getString(R.string.cannot_delete_unknown_category),
          Toast.LENGTH_SHORT).show();
    } else if (id == R.id.category_popup_all_read_item) {
      SmsDao.setAllCategorySmsAsRead(context, String.valueOf(category.getId()));
      ((MainActivity) context).onPostResume();
    } else if (id == R.id.category_popup_delete_all_sms) {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);

      builder.setTitle(
          context.getString(R.string.delete_all_sms_in_category_title, category.getName()));
      builder.setMessage(R.string.delete_all_sms_warning);
      builder.setPositiveButton(R.string.yes,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                int which) {

              Runnable runnable = new Runnable() {
                @Override
                public void run() {
                  SmsDao.deleteAllSmsOfCategoryById(context, category.getId());
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
      builder.setNegativeButton(R.string.no,
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
