package in.rahulja.groupingmessages;

import android.content.Context;
import android.os.Handler;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import in.rahulja.groupingmessages.model.Sms;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("WeakerAccess") class SmsListArrayAdapter
    extends ListAdapter<Sms, SmsListItemHolder> {

  interface OnSmsRemovedListener {
    void onSmsRemoved(Sms sms);
  }

  private static final int PENDING_REMOVAL_TIMEOUT = 3000; // 3sec
  private static final String ZERO = "0";

  private final Context context;
  private final OnSmsRemovedListener onSmsRemovedListener;
  private final Set<Long> itemsPendingRemoval = new HashSet<>();
  private final Map<Long, Runnable> pendingRunnables = new HashMap<>();
  private final Handler handler = new Handler(); // hanlder for running delayed runnables

  private Map<String, String> contactNamesByAddress = Collections.emptyMap();
  private Map<Long, String> categoryNamesById = Collections.emptyMap();

  SmsListArrayAdapter(Context context, OnSmsRemovedListener listener) {
    super(new SmsDiffCallback());
    this.context = context;
    this.onSmsRemovedListener = listener;
  }

  void submitSms(List<Sms> smsList, Map<String, String> contactNamesByAddress,
      Map<Long, String> categoryNamesById) {

    // resolved display names live outside the diffed item; when they change,
    // rebind everything so rows never show a stale name/category label
    boolean displayDataChanged =
        !this.contactNamesByAddress.equals(contactNamesByAddress)
            || !this.categoryNamesById.equals(categoryNamesById);
    this.contactNamesByAddress = contactNamesByAddress != null
        ? contactNamesByAddress : Collections.emptyMap();
    this.categoryNamesById = categoryNamesById != null
        ? categoryNamesById : Collections.emptyMap();

    if (displayDataChanged) {
      submitList(smsList, this::notifyDataSetChanged);
    } else {
      submitList(smsList);
    }
  }

  // 2. Override the onCreateViewHolder method
  @NonNull
  @Override
  public SmsListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    // 3. Inflate the view and return the new ViewHolder
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_list_item, parent, false);
    return new SmsListItemHolder(this.context, view);
  }

  // 4. Override the onBindViewHolder method
  @Override
  public void onBindViewHolder(@NonNull SmsListItemHolder holder, int position) {

    final Sms data = getItem(position);

    if (itemsPendingRemoval.contains(data.getId())) {
      holder.getRegularLayout().setVisibility(View.GONE);
      holder.getSwipeLayout().setVisibility(View.VISIBLE);
      holder.getUndo().setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          undoOpt(data);
        }
      });
    } else {
      holder.getRegularLayout().setVisibility(View.VISIBLE);
      holder.getSwipeLayout().setVisibility(View.GONE);
      holder.bindSms(data,
          displayNameFor(data),
          categoryNamesById.getOrDefault(data.getCategoryId(), ""));
    }
  }

  private String displayNameFor(Sms sms) {
    String contactName = contactNamesByAddress.get(sms.getAddress());
    return contactName != null ? contactName : sms.getAddress();
  }

  private void undoOpt(Sms smsItem) {
    Runnable pendingRemovalRunnable = pendingRunnables.remove(smsItem.getId());
    if (pendingRemovalRunnable != null) {
      handler.removeCallbacks(pendingRemovalRunnable);
    }
    itemsPendingRemoval.remove(smsItem.getId());
    // this will rebind the row in "normal" state
    notifyItemChanged(positionOf(smsItem.getId()));
  }

  public void pendingRemoval(int position) {

    final Sms data = getItem(position);
    if (!itemsPendingRemoval.contains(data.getId())) {
      itemsPendingRemoval.add(data.getId());
      // this will redraw row in "undo" state
      notifyItemChanged(position);
      // let's create, store and post a runnable to remove the data
      Runnable pendingRemovalRunnable = new Runnable() {
        @Override
        public void run() {
          remove(data);
        }
      };
      handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
      pendingRunnables.put(data.getId(), pendingRemovalRunnable);
    }
  }

  private void remove(Sms data) {
    itemsPendingRemoval.remove(data.getId());
    pendingRunnables.remove(data.getId());
    // the row disappears once the refreshed list lands via submitList
    onSmsRemovedListener.onSmsRemoved(data);
  }

  public boolean isPendingRemoval(int position) {
    return itemsPendingRemoval.contains(getItem(position).getId());
  }

  private int positionOf(long smsId) {
    List<Sms> currentList = getCurrentList();
    for (int i = 0; i < currentList.size(); i++) {
      if (currentList.get(i).getId() == smsId) {
        return i;
      }
    }
    return RecyclerView.NO_POSITION;
  }
}
