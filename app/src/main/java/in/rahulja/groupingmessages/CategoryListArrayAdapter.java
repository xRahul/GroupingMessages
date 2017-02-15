package in.rahulja.groupingmessages;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class CategoryListArrayAdapter extends RecyclerView.Adapter<CategoryListItemHolder> {

    private Context context;
    private List<Map<String, String>> categoryList;

    CategoryListArrayAdapter(Context context, List<Map<String, String>> objects) {

        this.context = context;
        if (objects == null) {
            this.categoryList = new ArrayList<>();
        } else {
            this.categoryList = objects;
        }
    }

    // 2. Override the onCreateViewHolder method
    @Override
    public CategoryListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 3. Inflate the view and return the new ViewHolder
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.category_list_item, parent, false);

        return new CategoryListItemHolder(view);
    }

    // 4. Override the onBindViewHolder method
    @Override
    public void onBindViewHolder(CategoryListItemHolder holder, int position) {

        // 5. Use position to access the correct category object
        Map<String, String> category = this.categoryList.get(position);

        // 6. Bind the category object to the holder
        holder.bindCategory(category);
    }

    @Override
    public int getItemCount() {
        return this.categoryList.size();
    }
}

