package in.rahulja.groupingmessages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.widget.DiffUtil;
import in.rahulja.groupingmessages.model.Category;
import in.rahulja.groupingmessages.model.Sms;
import org.junit.Test;

public class DiffCallbackTest {

  private static Category category(long id, String name, int color) {
    return new Category(id, name, color);
  }

  private static Sms sms(long id, long categoryId, long date, int visibility, int read,
      String address, String body, long similarTo) {
    return new Sms(id, categoryId, date, visibility, read, address, body, similarTo);
  }

  @Test
  public void category_itemsSame_whenIdsEqual() {
    CategoryDiffCallback callback = new CategoryDiffCallback();
    assertTrue(callback.areItemsTheSame(
        category(5, "Bank", 0xFF0000FF), category(5, "Renamed Bank", 0xFFFF0000)));
  }

  @Test
  public void category_itemsDifferent_whenIdsDiffer() {
    CategoryDiffCallback callback = new CategoryDiffCallback();
    assertFalse(callback.areItemsTheSame(
        category(5, "Bank", 0xFF0000FF), category(6, "Bank", 0xFF0000FF)));
  }

  @Test
  public void category_contentsSame_whenAllFieldsEqual() {
    CategoryDiffCallback callback = new CategoryDiffCallback();
    assertTrue(callback.areContentsTheSame(
        category(5, "Bank", 0xFF0000FF), category(5, "Bank", 0xFF0000FF)));
  }

  @Test
  public void category_contentsDifferent_whenAnyFieldDiffers() {
    CategoryDiffCallback callback = new CategoryDiffCallback();
    assertFalse(callback.areContentsTheSame(
        category(5, "Bank", 0xFF0000FF), category(5, "Renamed Bank", 0xFF0000FF)));
    assertFalse(callback.areContentsTheSame(
        category(5, "Bank", 0xFF0000FF), category(5, "Bank", 0xFFFF0000)));
  }

  @Test
  public void sms_itemsSame_whenIdsEqual() {
    SmsDiffCallback callback = new SmsDiffCallback();
    assertTrue(callback.areItemsTheSame(
        sms(7, 1, 100L, 1, 0, "12345", "hi", 7),
        sms(7, 2, 200L, 0, 1, "other", "other body", 8)));
  }

  @Test
  public void sms_itemsDifferent_whenIdsDiffer() {
    SmsDiffCallback callback = new SmsDiffCallback();
    assertFalse(callback.areItemsTheSame(
        sms(7, 1, 100L, 1, 0, "12345", "hi", 7),
        sms(8, 1, 100L, 1, 0, "12345", "hi", 7)));
  }

  @Test
  public void sms_contentsSame_whenAllFieldsEqual() {
    SmsDiffCallback callback = new SmsDiffCallback();
    assertTrue(callback.areContentsTheSame(
        sms(7, 1, 100L, 1, 0, "12345", "hi", 7),
        sms(7, 1, 100L, 1, 0, "12345", "hi", 7)));
  }

  @Test
  public void sms_contentsDifferent_whenEachFieldDiffers() {
    SmsDiffCallback callback = new SmsDiffCallback();
    Sms base = sms(7, 1, 100L, 1, 0, "12345", "hi", 7);
    Sms[] variants = {
        sms(7, 9, 100L, 1, 0, "12345", "hi", 7),
        sms(7, 1, 999L, 1, 0, "12345", "hi", 7),
        sms(7, 1, 100L, 0, 0, "12345", "hi", 7),
        sms(7, 1, 100L, 1, 1, "12345", "hi", 7),
        sms(7, 1, 100L, 1, 0, "98765", "hi", 7),
        sms(7, 1, 100L, 1, 0, "12345", "changed", 7),
        sms(7, 1, 100L, 1, 0, "12345", "hi", 99),
    };
    for (Sms variant : variants) {
      assertFalse("variant should differ from base", callback.areContentsTheSame(base, variant));
    }
  }

  @Test
  public void sms_contentsSame_whenNullableFieldsNullOnBoth() {
    SmsDiffCallback callback = new SmsDiffCallback();
    assertTrue(callback.areContentsTheSame(
        sms(7, 1, 100L, 1, 0, null, null, 7),
        sms(7, 1, 100L, 1, 0, null, null, 7)));
    assertFalse(callback.areContentsTheSame(
        sms(7, 1, 100L, 1, 0, null, "hi", 7),
        sms(7, 1, 100L, 1, 0, "12345", "hi", 7)));
    assertFalse(callback.areContentsTheSame(
        sms(7, 1, 100L, 1, 0, "12345", null, 7),
        sms(7, 1, 100L, 1, 0, "12345", "hi", 7)));
  }
}
