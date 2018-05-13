package in.rahulja.groupingmessages;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

public abstract class SwipeUtil extends ItemTouchHelper.SimpleCallback {

  private Drawable deleteIcon;

  private int xMarkMargin;

  private boolean initiated;
  private Context context;

  private int leftColorCode;
  private String leftSwipeLabel;

  SwipeUtil(int dragDirs, int swipeDirs, Context context) {
    super(dragDirs, swipeDirs);
    this.context = context;
  }

  private void init() {
    xMarkMargin = (int) context.getResources().getDimension(R.dimen.ic_clear_margin);
    deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
    initiated = true;
  }

  @Override
  public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
      RecyclerView.ViewHolder target) {
    return false;
  }

  @Override
  public abstract void onSwiped(RecyclerView.ViewHolder viewHolder, int direction);

  @Override
  public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
      float dX, float dY, int actionState, boolean isCurrentlyActive) {

    View itemView = viewHolder.itemView;
    if (!initiated) {
      init();
    }

    int itemHeight = itemView.getBottom() - itemView.getTop();

    int intrinsicWidth = deleteIcon.getIntrinsicWidth();
    int intrinsicHeight = deleteIcon.getIntrinsicWidth();

    int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
    int xMarkRight = itemView.getRight() - xMarkMargin;
    int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
    int xMarkBottom = xMarkTop + intrinsicHeight;

    //Setting Swipe Icon
    deleteIcon.setBounds(xMarkLeft - 20, xMarkTop - 10, xMarkRight, xMarkBottom + 10);
    deleteIcon.setColorFilter(getLeftColorCode(), PorterDuff.Mode.DST);
    deleteIcon.draw(c);

    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
  }

  @SuppressWarnings("unused")
  public String getLeftSwipeLabel() {
    return leftSwipeLabel;
  }

  public void setLeftSwipeLabel(String leftSwipeLabel) {
    this.leftSwipeLabel = leftSwipeLabel;
  }

  private int getLeftColorCode() {
    return leftColorCode;
  }

  public void setLeftColorCode(int leftColorCode) {
    this.leftColorCode = leftColorCode;
  }
}