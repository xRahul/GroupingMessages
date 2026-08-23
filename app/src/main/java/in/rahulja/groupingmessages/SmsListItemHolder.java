package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Sms;

@SuppressWarnings("WeakerAccess") class SmsListItemHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener, View.OnLongClickListener {

  private final TextView smsBodyTextView;
  private final TextView smsFromTextView;
  private final TextView smsTimeTextView;
  private final Button smsCategoryButton;
  private final CardView listItemContent;

  private RelativeLayout regularLayout;
  private LinearLayout swipeLayout;
  private TextView undo;

  RelativeLayout getRegularLayout() {
    return regularLayout;
  }

  LinearLayout getSwipeLayout() {
    return swipeLayout;
  }

  TextView getUndo() {
    return undo;
  }

  private Sms sms;
  private Context context;

  SmsListItemHolder(Context contextTemp, View itemView) {
    super(itemView);

    // 1. Set the context
    context = contextTemp;

    // 2. Set up the UI widgets of the holder
    smsBodyTextView = itemView.findViewById(R.id.sms_body_textview);
    smsFromTextView = itemView.findViewById(R.id.sms_from_textview);
    smsTimeTextView = itemView.findViewById(R.id.sms_time_textview);
    smsCategoryButton = itemView.findViewById(R.id.bucket_button);
    listItemContent = itemView.findViewById(R.id.sms_list_item_content);

    regularLayout = itemView.findViewById(R.id.regularLayout);
    swipeLayout = itemView.findViewById(R.id.swipeLayout);
    undo = itemView.findViewById(R.id.undo);

    // 3. Set the "onClick" listener of the holder
    itemView.setOnClickListener(this);
    listItemContent.setOnClickListener(this);
    smsBodyTextView.setOnClickListener(this);
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

  public void bindSms(Sms smsTemp, String fromDisplay, String categoryName) {
    sms = smsTemp;
    // 4. Bind the data to the ViewHolder
    smsBodyTextView.setText(sms.getBody());

    if (sms.getRead() == 0) {
      listItemContent.setCardBackgroundColor(Color.LTGRAY);
    } else {
      listItemContent.setCardBackgroundColor(Color.WHITE);
    }

    smsFromTextView.setText(fromDisplay);
    smsTimeTextView.setText(getDate(sms.getDate()));
    smsCategoryButton.setText(
        context.getString(R.string.change_category_with_name, categoryName)
    );

    smsCategoryButton.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == smsCategoryButton.getId()) {
      Intent i = new Intent(context, ChangeCategoryActivity.class);
      i.putExtra("sms_id", sms.getId());
      i.putExtra("sms_list_position", getAdapterPosition());

      ((AppCompatActivity) context).startActivityForResult(i, 111);
    } else if (v.getId() == listItemContent.getId() || v.getId() == smsBodyTextView.getId()) {
      SmsDao.setSmsAsRead(context, String.valueOf(sms.getId()));
      listItemContent.setCardBackgroundColor(Color.WHITE);
    }
  }

  @Override
  public boolean onLongClick(View view) {
    return false;
  }
}
