package in.rahulja.groupingmessages;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CategoryListArrayAdapter extends RecyclerView.Adapter<CategoryListItemHolder> {

  private List<Map<String, String>> categoryList;

  CategoryListArrayAdapter(Context contextParam, List<Map<String, String>> objects) {

    if (objects == null) {
      categoryList = new ArrayList<>();
    } else {
      categoryList = objects;
    }
  }

  @NonNull @Override
  public CategoryListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.category_list_item, parent, false);
    return new CategoryListItemHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull CategoryListItemHolder holder, int position) {
    holder.bindCategory(this.categoryList.get(position));
    holder.itemView.setLongClickable(true);
  }

  @Override
  public int getItemCount() {
    return this.categoryList.size();
  }
}

