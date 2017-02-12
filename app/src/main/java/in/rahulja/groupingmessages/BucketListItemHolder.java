package in.rahulja.groupingmessages;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;


class BucketListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView bucketNameTextView;
    private final TextView bucketCountTextView;
    private final RelativeLayout bucketListViewParent;

    BucketListItemHolder(View itemView) {
        super(itemView);

        // 1. Set the context

        // 2. Set up the UI widgets of the holder
        bucketNameTextView = (TextView) itemView.findViewById(R.id.bucket_name_textview);
        bucketCountTextView = (TextView) itemView.findViewById(R.id.bucket_count_textview);
        bucketListViewParent = (RelativeLayout) itemView.findViewById(R.id.bucket_list_parent);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }


    void bindBucket(Map<String, String> bucket) {
        Log.d("asdfasf", bucket.toString());
        Log.d("asdfasf", bucketNameTextView.toString());
        // 4. Bind the data to the ViewHolder
        bucketNameTextView.setText(bucket.get("name"));
        bucketCountTextView.setText(bucket.get("count"));

        if ("Critical".equals(bucket.get("name"))) {
            bucketListViewParent.setBackgroundColor(Color.parseColor("#E57373"));
        } else if ("None".equals(bucket.get("name"))) {
            bucketListViewParent.setBackgroundColor(Color.parseColor("#E0E0E0"));
        } else if ("Info".equals(bucket.get("name"))) {
            bucketListViewParent.setBackgroundColor(Color.parseColor("#81C784"));
        } else if ("Debug".equals(bucket.get("name"))) {
            bucketListViewParent.setBackgroundColor(Color.parseColor("#4DD0E1"));
        } else if ("Error".equals(bucket.get("name"))) {
            bucketListViewParent.setBackgroundColor(Color.parseColor("#FFF176"));
        }

    }

    @Override
    public void onClick(View v) {
        // just empty
    }
}
