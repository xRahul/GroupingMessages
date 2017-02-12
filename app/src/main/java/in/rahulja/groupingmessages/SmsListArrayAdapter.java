package in.rahulja.groupingmessages;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Map;


class SmsListArrayAdapter extends RecyclerView.Adapter<SmsListItemHolder> {

    private Context context;
    private List<Map<String, String>> smsList;

    SmsListArrayAdapter(Context context, List<Map<String, String>> objects) {

        this.context = context;
        this.smsList = objects;
    }

    // 2. Override the onCreateViewHolder method
    @Override
    public SmsListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 3. Inflate the view and return the new ViewHolder
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.sms_list_item, parent, false);

        return new SmsListItemHolder(this.context, view);
    }

    // 4. Override the onBindViewHolder method
    @Override
    public void onBindViewHolder(SmsListItemHolder holder, int position) {

        // 5. Use position to access the correct Bakery object
        Map<String, String> sms = this.smsList.get(position);

        // 6. Bind the bakery object to the holder
        holder.bindSms(sms);
    }

    @Override
    public int getItemCount() {
        return this.smsList.size();
    }
}

