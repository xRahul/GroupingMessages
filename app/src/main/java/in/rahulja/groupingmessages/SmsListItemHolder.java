package in.rahulja.groupingmessages;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import in.rahulja.groupingmessages.db.SmsDao;
import in.rahulja.groupingmessages.model.Sms;
import in.rahulja.groupingmessages.vm.AppExecutors;

@SuppressWarnings("WeakerAccess") class SmsListItemHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener, View.OnLongClickListener {

  private final TextView smsBodyTextView;
  private final TextView smsFromTextView;
  private final TextView smsTimeTextView;
  private final TextView smsConfidenceTextView;
  private final Button smsCategoryButton;
  private final CheckBox smsSelectionCheckBox;
  private final CardView listItemContent;
  private final int readBackgroundColor;
  private final int unreadBackgroundColor;

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
    smsConfidenceTextView = itemView.findViewById(R.id.sms_confidence_textview);
    smsCategoryButton = itemView.findViewById(R.id.bucket_button);
    smsSelectionCheckBox = itemView.findViewById(R.id.sms_selection_checkbox);
    listItemContent = itemView.findViewById(R.id.sms_list_item_content);
    readBackgroundColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface);
    unreadBackgroundColor =
        resolveThemeColor(com.google.android.material.R.attr.colorSurfaceVariant);

    regularLayout = itemView.findViewById(R.id.regularLayout);
    swipeLayout = itemView.findViewById(R.id.swipeLayout);
    undo = itemView.findViewById(R.id.undo);

    // 3. Set the "onClick" listener of the holder
    itemView.setOnClickListener(this);
    itemView.setOnLongClickListener(this);
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

  public void bindSms(Sms smsTemp, String fromDisplay, String categoryName,
      String suggestionText, boolean selectionMode, boolean selected) {
    sms = smsTemp;
    smsSelectionCheckBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
    smsSelectionCheckBox.setChecked(selected);
    // 4. Bind the data to the ViewHolder
    smsBodyTextView.setText(sms.getBody());

    if (sms.getRead() == 0) {
      listItemContent.setCardBackgroundColor(unreadBackgroundColor);
    } else {
      listItemContent.setCardBackgroundColor(readBackgroundColor);
    }

    smsFromTextView.setText(fromDisplay);
    smsTimeTextView.setText(getDate(sms.getDate()));
    smsConfidenceTextView.setText(suggestionText != null
        ? suggestionText
        : sms.getSimScore() > 0.0f
            ? context.getString(R.string.classification_confidence,
                Math.round(sms.getSimScore() * 100))
            : context.getString(R.string.unclassified));
    smsCategoryButton.setText(
        context.getString(R.string.change_category_with_name, categoryName)
    );

    smsCategoryButton.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    if (context instanceof SmsActivity && ((SmsActivity) context).isSelectionMode()
        && v.getId() != smsCategoryButton.getId()) {
      ((SmsActivity) context).toggleSelection(sms);
      return;
    }
    if (v.getId() == smsCategoryButton.getId()) {
      Intent i = new Intent(context, ChangeCategoryActivity.class);
      i.putExtra("sms_id", sms.getId());
      i.putExtra("sms_list_position", getAdapterPosition());

      ((AppCompatActivity) context).startActivityForResult(i, 111);
    } else if (v.getId() == listItemContent.getId() || v.getId() == smsBodyTextView.getId()) {
      if (context instanceof SmsActivity && ((SmsActivity) context).isReviewMode()) {
        ((SmsActivity) context).reviewSms(sms);
        return;
      }
      if (context instanceof SmsActivity && ((SmsActivity) context).isAllCategories()) {
        ((SmsActivity) context).openCategory(sms.getCategoryId());
        return;
      }
      final long smsId = sms.getId();
      AppExecutors.disk(() -> SmsDao.setSmsAsRead(context, String.valueOf(smsId)));
      listItemContent.setCardBackgroundColor(readBackgroundColor);
    }
  }

  private int resolveThemeColor(int attr) {
    TypedValue value = new TypedValue();
    if (!context.getTheme().resolveAttribute(attr, value, true)) {
      return android.graphics.Color.TRANSPARENT;
    }
    return value.resourceId != 0
        ? ContextCompat.getColor(context, value.resourceId)
        : value.data;
  }

  @Override
  public boolean onLongClick(View view) {
    if (context instanceof SmsActivity) {
      ((SmsActivity) context).startSelection(sms);
      return true;
    }
    return false;
  }
}
