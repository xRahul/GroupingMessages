package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;


class SmsListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private static final String KEY_FROM = "from";
    private final TextView smsBodyTextView;
    private final TextView smsFromTextView;
    private final TextView smsTimeTextView;
    private final Button smsCategoryButton;

    private Map<String, String> sms;
    private Context context;

    SmsListItemHolder(Context contextTemp, View itemView) {
        super(itemView);

        // 1. Set the context
        context = contextTemp;

        // 2. Set up the UI widgets of the holder
        smsBodyTextView = (TextView) itemView.findViewById(R.id.sms_body_textview);
        smsFromTextView = (TextView) itemView.findViewById(R.id.sms_from_textview);
        smsTimeTextView = (TextView) itemView.findViewById(R.id.sms_time_textview);
        smsCategoryButton = (Button) itemView.findViewById(R.id.bucket_button);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }

    private static String getDate(long milliSeconds, String dateFormat) {
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
        smsBodyTextView.setText(sms.get(DatabaseContract.Sms.KEY_BODY));

        smsFromTextView.setText(sms.get(KEY_FROM));
        smsTimeTextView.setText(
                getDate(
                        Long.parseLong(sms.get(DatabaseContract.Sms.KEY_DATE)),
                        "dd/MM/yyyy hh:mm:ss a"
                )
        );
        smsCategoryButton.setText(
                String.format("Change Category - %s", sms.get("category_name"))
        );

        smsCategoryButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == smsCategoryButton.getId()) {
            Intent i = new Intent(context, ChangeCategoryActivity.class);
            i.putExtra("sms_id", Long.parseLong(sms.get(DatabaseContract.Sms._ID)));
            i.putExtra("sms_list_position", getAdapterPosition());

            ((AppCompatActivity) context).startActivityForResult(i, 111);
        }
    }

}
