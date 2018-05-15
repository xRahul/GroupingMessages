package in.rahulja.groupingmessages;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SmsListArrayAdapter extends RecyclerView.Adapter<SmsListItemHolder> {

  private static final int PENDING_REMOVAL_TIMEOUT = 3000; // 3sec
  private Context context;
  private List<Map<String, String>> smsList;
  private ArrayList<Map<String, String>> itemsPendingRemoval;
  private Handler handler = new Handler(); // hanlder for running delayed runnables
  private HashMap<Map<String, String>, Runnable> pendingRunnables = new HashMap<>();
  // map of items to pending runnables, so we can cancel a removal if need be

  SmsListArrayAdapter(Context context, List<Map<String, String>> objects) {

    this.context = context;
    this.smsList = objects;
    this.itemsPendingRemoval = new ArrayList<>();
  }

  // 2. Override the onCreateViewHolder method
  @Override
  public SmsListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    // 3. Inflate the view and return the new ViewHolder
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_list_item, parent, false);
    return new SmsListItemHolder(this.context, view);
  }

  // 4. Override the onBindViewHolder method
  @Override
  public void onBindViewHolder(SmsListItemHolder holder, int position) {

    final Map<String, String> data = smsList.get(position);

    if (itemsPendingRemoval.contains(data)) {
      holder.regularLayout.setVisibility(View.GONE);
      holder.swipeLayout.setVisibility(View.VISIBLE);
      holder.undo.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          undoOpt(data);
        }
      });
    } else {
      holder.regularLayout.setVisibility(View.VISIBLE);
      holder.swipeLayout.setVisibility(View.GONE);
      holder.bindSms(data);
    }
  }

  private void undoOpt(Map<String, String> smsItem) {
    Runnable pendingRemovalRunnable = pendingRunnables.get(smsItem);
    pendingRunnables.remove(smsItem);
    if (pendingRemovalRunnable != null) {
      handler.removeCallbacks(pendingRemovalRunnable);
    }
    itemsPendingRemoval.remove(smsItem);
    // this will rebind the row in "normal" state
    notifyItemChanged(smsList.indexOf(smsItem));
  }

  @Override
  public int getItemCount() {
    return this.smsList.size();
  }

  void pendingRemoval(int position) {

    final Map<String, String> data = smsList.get(position);
    if (!itemsPendingRemoval.contains(data)) {
      itemsPendingRemoval.add(data);
      // this will redraw row in "undo" state
      notifyItemChanged(position);
      // let's create, store and post a runnable to remove the data
      Runnable pendingRemovalRunnable = new Runnable() {
        @Override
        public void run() {
          remove(smsList.indexOf(data));
        }
      };
      handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
      pendingRunnables.put(data, pendingRemovalRunnable);
    }
  }

  private void remove(int position) {
    Map<String, String> data = smsList.get(position);
    if (itemsPendingRemoval.contains(data)) {
      itemsPendingRemoval.remove(data);
    }
    if (smsList.contains(data)) {
      smsList.remove(position);
      notifyItemRemoved(position);
      DatabaseBridge.deleteSmsByMap(context, data);
    }
  }

  public boolean isPendingRemoval(int position) {
    Map<String, String> data = smsList.get(position);
    return itemsPendingRemoval.contains(data);
  }
}
