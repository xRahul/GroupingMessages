package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;


class CategoryListItemHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    private static final String COUNT = "count";
    private static final String CATEGORY_ID = "category_id";
    private static final String EDIT_CATEGORY_TAG = "EDIT_CATEGORY_TAG";
    private final Context context;
    private final TextView categoryNameTextView;
    private final TextView categoryCountTextView;
    private final RelativeLayout categoryListViewParent;
    private String categoryId;
    private String categoryName;
    private String categoryColor;

    CategoryListItemHolder(View itemView) {
        super(itemView);

        context = itemView.getContext();

        categoryNameTextView = (TextView) itemView.findViewById(R.id.category_name_textview);
        categoryCountTextView = (TextView) itemView.findViewById(R.id.category_count_textview);
        categoryListViewParent = (RelativeLayout) itemView.findViewById(R.id.category_list_parent);

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }


    void bindCategory(Map<String, String> category) {
        categoryId = category.get(DatabaseContract.Category._ID);
        categoryName = category.get(DatabaseContract.Category.KEY_NAME);
        categoryColor = category.get(DatabaseContract.Category.KEY_COLOR);
        categoryNameTextView.setText(category.get(DatabaseContract.Category.KEY_NAME));
        categoryCountTextView.setText(category.get(COUNT));
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

                } else if (id == R.id.category_popup_delete_item) {
                    DatabaseBridge.deleteCategory(context, Long.parseLong(categoryId));
                    ((MainActivity) context).onPostResume();
                }
                return true;
            }
        });
        menu.inflate(R.menu.category_long_press_menu);
        menu.show();
        return true;
    }
}
