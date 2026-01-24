package in.rahulja.groupingmessages;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import java.util.Objects;

/**
 * A simple {@link DialogFragment} subclass.
 */
public class AddCategoryFragment extends DialogFragment {

  // Use this instance of the interface to deliver action events
  private AddCategoryDialogListener mListener;

  // Override the Fragment.onAttach() method to instantiate the AddCategoryDialogListener
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    // Verify that the host activity implements the callback interface
    try {
      // Instantiate the AddCategoryDialogListener so we can send events to the host
      mListener = (AddCategoryDialogListener) context;
    } catch (ClassCastException e) {
      // The activity doesn't implement the interface, throw exception
      throw new ClassCastException(context.toString()
          + " must implement AddCategoryDialogListener");
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

    View categoryDialogView = View.inflate(getContext(), R.layout.dialog_add_category, null);

    final Bundle mArgs = getArguments();
    int positiveButton = R.string.add;
    if (mArgs != null && "EDIT".equals(mArgs.getString("ACTION"))) {
      String categoryNameArg = mArgs.getString(DatabaseContract.Category.KEY_NAME);
      int categoryColorArg = mArgs.getInt(DatabaseContract.Category.KEY_COLOR);
      EditText editTextCategoryName =
          categoryDialogView.findViewById(R.id.editTextAddCategory);
      editTextCategoryName.setText(categoryNameArg);
      com.flask.colorpicker.ColorPickerView colorPickerView =
          categoryDialogView.findViewById(
              R.id.pick_category_color);
      colorPickerView.setColor(categoryColorArg, true);
      positiveButton = R.string.edit;
    }

    builder.setTitle(R.string.add_category_dialog_title);

    builder.setView(categoryDialogView)
        // Add action buttons
        .setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            mListener.onDialogPositiveClick(AddCategoryFragment.this, mArgs);
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            mListener.onDialogNegativeClick(AddCategoryFragment.this, mArgs);
          }
        });

    return builder.create();
  }

  /**
   * interface that can be called by the parent activity
   */
  public interface AddCategoryDialogListener {
    void onDialogPositiveClick(DialogFragment dialog, Bundle bundle);

    void onDialogNegativeClick(DialogFragment dialog, Bundle bundle);
  }
}
