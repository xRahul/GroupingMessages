package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;


class SmsListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String BUCKET_ID = "bucketId";
    private final TextView smsBodyTextView;
    private final TextView smsFromTextView;
    private final TextView smsTimeTextView;
    private final Button smsBucketButton;

    private Map<String, String> sms;
    private Context context;
    private String[] bucketNames;

    SmsListItemHolder(Context contextTemp, View itemView) {
        super(itemView);

        // 1. Set the context
        context = contextTemp;

        // 2. Set up the UI widgets of the holder
        smsBodyTextView = (TextView) itemView.findViewById(R.id.sms_body_textview);
        smsFromTextView = (TextView) itemView.findViewById(R.id.sms_from_textview);
        smsTimeTextView = (TextView) itemView.findViewById(R.id.sms_time_textview);
        smsBucketButton = (Button) itemView.findViewById(R.id.bucket_button);
        bucketNames = context.getResources().getStringArray(R.array.buckets);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }

    private static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, new Locale("en"));

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    void bindSms(Map<String, String> smsTemp) {
        sms = smsTemp;
        // 4. Bind the data to the ViewHolder
        smsBodyTextView.setText(sms.get("body"));
        smsFromTextView.setText(sms.get("from"));
        smsTimeTextView.setText(getDate(Long.parseLong(sms.get("time")), "dd/MM/yyyy hh:mm:ss a"));
        smsBucketButton.setText(bucketNames[Integer.parseInt(sms.get(BUCKET_ID))] + " - " + "Change Bucket");


        if ("Critical".equals(bucketNames[Integer.parseInt(sms.get(BUCKET_ID))]))
        {
            smsBucketButton.setBackgroundColor(Color.parseColor("#E57373"));
            smsBucketButton.setTextColor(Color.BLACK);
        }
        else if ("None".equals(bucketNames[Integer.parseInt(sms.get(BUCKET_ID))]))
        {
            smsBucketButton.setBackgroundColor(Color.parseColor("#E0E0E0"));
            smsBucketButton.setTextColor(Color.BLACK);
        }
        else if ("Info".equals(bucketNames[Integer.parseInt(sms.get(BUCKET_ID))]))
        {
            smsBucketButton.setBackgroundColor(Color.parseColor("#81C784"));
            smsBucketButton.setTextColor(Color.BLACK);
        }
        else if ("Debug".equals(bucketNames[Integer.parseInt(sms.get(BUCKET_ID))]))
        {
            smsBucketButton.setBackgroundColor(Color.parseColor("#4DD0E1"));
            smsBucketButton.setTextColor(Color.BLACK);
        }
        else if ("Error".equals(bucketNames[Integer.parseInt(sms.get(BUCKET_ID))]))
        {
            smsBucketButton.setBackgroundColor(Color.parseColor("#FFF176"));
            smsBucketButton.setTextColor(Color.BLACK);
        }

        smsBucketButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == smsBucketButton.getId()){
            Intent i = new Intent(context, ChooseBucketActivity.class);
            i.putExtra("sms_position", getAdapterPosition());
            i.putExtra("old_bucket_id", Integer.parseInt(sms.get(BUCKET_ID)));
            ((AppCompatActivity) context).startActivityForResult(i, 111);
            Toast.makeText(v.getContext(), "ITEM PRESSED = " + getAdapterPosition(), Toast.LENGTH_SHORT).show();
        }
    }

}
