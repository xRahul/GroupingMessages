package in.rahulja.groupingmessages;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import in.rahulja.groupingmessages.model.Category;
import java.util.Objects;

class CategoryDiffCallback extends DiffUtil.ItemCallback<Category> {

  @Override
  public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override
  public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
    return oldItem.getColor() == newItem.getColor()
        && Objects.equals(oldItem.getName(), newItem.getName());
  }
}
