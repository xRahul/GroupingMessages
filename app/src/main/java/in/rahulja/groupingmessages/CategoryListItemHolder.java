package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;


class CategoryListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String COUNT = "count";
    private static final String CATEGORY_ID = "category_id";
    private final Context context;
    private final TextView categoryNameTextView;
    private final TextView categoryCountTextView;
    private final RelativeLayout categoryListViewParent;
    private String categoryId;

    CategoryListItemHolder(View itemView) {
        super(itemView);

        context = itemView.getContext();

        categoryNameTextView = (TextView) itemView.findViewById(R.id.category_name_textview);
        categoryCountTextView = (TextView) itemView.findViewById(R.id.category_count_textview);
        categoryListViewParent = (RelativeLayout) itemView.findViewById(R.id.category_list_parent);

        itemView.setOnClickListener(this);
    }


    void bindCategory(Map<String, String> category) {
        categoryId = category.get(DatabaseContract.Category._ID);
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
}
