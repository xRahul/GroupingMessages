package in.rahulja.groupingmessages;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import in.rahulja.groupingmessages.model.Sms;
import java.util.Objects;

class SmsDiffCallback extends DiffUtil.ItemCallback<Sms> {

  @Override
  public boolean areItemsTheSame(@NonNull Sms oldItem, @NonNull Sms newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override
  public boolean areContentsTheSame(@NonNull Sms oldItem, @NonNull Sms newItem) {
    return oldItem.getCategoryId() == newItem.getCategoryId()
        && oldItem.getDate() == newItem.getDate()
        && oldItem.getVisibility() == newItem.getVisibility()
        && oldItem.getRead() == newItem.getRead()
        && oldItem.getAddress().equals(newItem.getAddress())
        && oldItem.getBody().equals(newItem.getBody())
        && oldItem.getSimilarTo() == newItem.getSimilarTo();
  }
}
