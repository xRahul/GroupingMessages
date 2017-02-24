package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Map;


class SmsListItemHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    private static final String KEY_FROM = "from";
    private final TextView smsBodyTextView;
    private final TextView smsFromTextView;
    private final TextView smsTimeTextView;
    private final Button smsCategoryButton;
    private final CardView listItemContent;
    RelativeLayout regularLayout;
    LinearLayout swipeLayout;
    TextView undo;

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
        listItemContent = (CardView) itemView.findViewById(R.id.sms_list_item_content);

        regularLayout = (RelativeLayout) itemView.findViewById(R.id.regularLayout);
        swipeLayout = (LinearLayout) itemView.findViewById(R.id.swipeLayout);
        undo = (TextView) itemView.findViewById(R.id.undo);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
        listItemContent.setOnClickListener(this);
    }

    private String getDate(long milliSeconds) {

        return DateUtils.getRelativeDateTimeString(
                context,
                milliSeconds,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_TIME
        ).toString();
    }

    void bindSms(Map<String, String> smsTemp) {
        sms = smsTemp;
        // 4. Bind the data to the ViewHolder
        smsBodyTextView.setText(sms.get(DatabaseContract.Sms.KEY_BODY));

        if (Integer.parseInt(sms.get(DatabaseContract.Sms.KEY_READ)) == 0) {
            listItemContent.setCardBackgroundColor(Color.LTGRAY);
        }

        smsFromTextView.setText(sms.get(KEY_FROM));
        smsTimeTextView.setText(getDate(Long.parseLong(sms.get(DatabaseContract.Sms.KEY_DATE))));
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

        else if (v.getId() == listItemContent.getId()) {
            DatabaseBridge.setSmsAsRead(context, sms.get(DatabaseContract.Sms._ID));
            listItemContent.setCardBackgroundColor(Color.WHITE);
        }

    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }
}
