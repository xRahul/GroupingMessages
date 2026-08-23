package in.rahulja.groupingmessages;

import androidx.recyclerview.widget.ListAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import in.rahulja.groupingmessages.model.Category;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class CategoryListArrayAdapter extends ListAdapter<Category, CategoryListItemHolder> {

  private Map<Long, String> unreadCountsByCategoryId = Collections.emptyMap();
  private Map<Long, String> readCountsByCategoryId = Collections.emptyMap();

  CategoryListArrayAdapter() {
    super(new CategoryDiffCallback());
  }

  void submitCategories(List<Category> categories,
      Map<Long, String> unreadCountsByCategoryId, Map<Long, String> readCountsByCategoryId) {

    // counts live outside the diffed item; when they change, rebind everything
    // (same wholesale redraw as the pre-DiffUtil rebuild) so cards stay fresh
    boolean countsChanged =
        !this.unreadCountsByCategoryId.equals(unreadCountsByCategoryId)
            || !this.readCountsByCategoryId.equals(readCountsByCategoryId);
    this.unreadCountsByCategoryId = unreadCountsByCategoryId != null
        ? unreadCountsByCategoryId : Collections.emptyMap();
    this.readCountsByCategoryId = readCountsByCategoryId != null
        ? readCountsByCategoryId : Collections.emptyMap();

    if (countsChanged) {
      submitList(categories, this::notifyDataSetChanged);
    } else {
      submitList(categories);
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
    Category category = getItem(position);
    holder.bindCategory(category,
        unreadCountsByCategoryId.getOrDefault(category.getId(), "0"),
        readCountsByCategoryId.getOrDefault(category.getId(), "0"));
    holder.itemView.setLongClickable(true);
  }
}
