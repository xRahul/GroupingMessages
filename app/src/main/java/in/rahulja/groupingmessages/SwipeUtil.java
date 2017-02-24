package in.rahulja.groupingmessages;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

public abstract class SwipeUtil extends ItemTouchHelper.SimpleCallback {

    private Drawable background;
    private Drawable deleteIcon;

    private int xMarkMargin;

    private boolean initiated;
    private Context context;

    private int leftcolorCode;
    private String leftSwipeLable;


    public SwipeUtil(int dragDirs, int swipeDirs, Context context) {
        super(dragDirs, swipeDirs);
        this.context = context;
    }


    private void init() {
        background = new ColorDrawable();
        xMarkMargin = (int) context.getResources().getDimension(R.dimen.ic_clear_margin);
        deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        //deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        initiated = true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public abstract void onSwiped(RecyclerView.ViewHolder viewHolder, int direction);

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {

        return super.getSwipeDirs(recyclerView, viewHolder);
    }


    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        if (!initiated) {
            init();
        }

        int itemHeight = itemView.getBottom() - itemView.getTop();

        //Setting Swipe Background
//        ((ColorDrawable) background).setColor(getLeftcolorCode());
//        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
//        background.draw(c);

        int intrinsicWidth = deleteIcon.getIntrinsicWidth();
        int intrinsicHeight = deleteIcon.getIntrinsicWidth();

        int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - xMarkMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int xMarkBottom = xMarkTop + intrinsicHeight;


        //Setting Swipe Icon
        deleteIcon.setBounds(xMarkLeft - 20, xMarkTop - 10, xMarkRight, xMarkBottom + 10);
        deleteIcon.setColorFilter(getLeftcolorCode(), PorterDuff.Mode.DST);
        deleteIcon.draw(c);

//        //Setting Swipe Text
//        Paint paint = new Paint();
//        //paint.setColor(Color.WHITE);
//        paint.setColor(getLeftcolorCode());
//        paint.setTextSize(48);
//        paint.setTextAlign(Paint.Align.CENTER);
//        c.drawText(getLeftSwipeLable(), xMarkLeft + 10, xMarkTop + 10, paint);


        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    public String getLeftSwipeLable() {
        return leftSwipeLable;
    }

    public void setLeftSwipeLable(String leftSwipeLable) {
        this.leftSwipeLable = leftSwipeLable;
    }

    public int getLeftcolorCode() {
        return leftcolorCode;
    }

    public void setLeftcolorCode(int leftcolorCode) {
        this.leftcolorCode = leftcolorCode;
    }
}