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


class BucketListArrayAdapter extends RecyclerView.Adapter<BucketListItemHolder> {

    private Context context;
    private List<Map<String, String>> bucketList;

    BucketListArrayAdapter(Context context, List<Map<String, String>> objects) {

        this.context = context;
        if (objects == null) {
            this.bucketList = new ArrayList<>();
        } else {
            this.bucketList = objects;
        }
    }

    // 2. Override the onCreateViewHolder method
    @Override
    public BucketListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 3. Inflate the view and return the new ViewHolder
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.bucket_list_item, parent, false);

        return new BucketListItemHolder(view);
    }

    // 4. Override the onBindViewHolder method
    @Override
    public void onBindViewHolder(BucketListItemHolder holder, int position) {

        // 5. Use position to access the correct Bakery object
        Map<String, String> sms = this.bucketList.get(position);

        // 6. Bind the bakery object to the holder
        holder.bindBucket(sms);
    }

    @Override
    public int getItemCount() {
        return this.bucketList.size();
    }
}

