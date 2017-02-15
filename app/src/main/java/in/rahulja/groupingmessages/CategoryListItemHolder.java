package in.rahulja.groupingmessages;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;


class CategoryListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView categoryNameTextView;
    private final TextView categoryCountTextView;
    private final RelativeLayout categoryListViewParent;

    CategoryListItemHolder(View itemView) {
        super(itemView);

        // 1. Set the context

        // 2. Set up the UI widgets of the holder
        categoryNameTextView = (TextView) itemView.findViewById(R.id.category_name_textview);
        categoryCountTextView = (TextView) itemView.findViewById(R.id.category_count_textview);
        categoryListViewParent = (RelativeLayout) itemView.findViewById(R.id.category_list_parent);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }


    void bindCategory(Map<String, String> category) {
        // 4. Bind the data to the ViewHolder
        categoryNameTextView.setText(category.get("name"));
        categoryCountTextView.setText("0");
        if (category.get("color") != null) {
            categoryListViewParent.setBackgroundColor(Integer.parseInt(category.get("color")));
        }
    }

    @Override
    public void onClick(View v) {
        // just empty
    }
}
