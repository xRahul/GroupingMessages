package in.rahulja.groupingmessages;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import in.rahulja.groupingmessages.model.Category;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CategoryListArrayAdapter extends RecyclerView.Adapter<CategoryListItemHolder> {

  private static final String COUNT_UNREAD = "count_unread";
  private static final String COUNT_READ = "count_read";

  private final List<Map<String, String>> categoryList;

  CategoryListArrayAdapter(List<Category> categories,
      Map<Long, String> unreadCountsByCategoryId, Map<Long, String> readCountsByCategoryId) {

    categoryList = new ArrayList<>();
    if (categories == null) {
      return;
    }

    for (Category category : categories) {
      Map<String, String> categoryItem = new HashMap<>();
      categoryItem.put(DatabaseContract.Category._ID, String.valueOf(category.getId()));
      categoryItem.put(DatabaseContract.Category.KEY_NAME, category.getName());
      categoryItem.put(DatabaseContract.Category.KEY_COLOR, String.valueOf(category.getColor()));
      categoryItem.put(COUNT_UNREAD,
          unreadCountsByCategoryId.getOrDefault(category.getId(), "0"));
      categoryItem.put(COUNT_READ,
          readCountsByCategoryId.getOrDefault(category.getId(), "0"));
      categoryList.add(categoryItem);
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
