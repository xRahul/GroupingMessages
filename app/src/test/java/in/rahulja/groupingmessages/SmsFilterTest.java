package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import in.rahulja.groupingmessages.model.Sms;
import in.rahulja.groupingmessages.util.SmsFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class SmsFilterTest {

  private List<Sms> sampleList;
  private Map<String, String> contactNames;

  @Before
  public void setUp() {
    sampleList = new ArrayList<>();
    sampleList.add(new Sms(1L, 1L, 1000L, 1, 0, "VM-HDFCBK", "Your OTP for Rs 500 txn is 123456", 0L));
    sampleList.add(new Sms(2L, 1L, 2000L, 1, 1, "AD-SWIGGY", "Your food order has been delivered", 0L));
    sampleList.add(new Sms(3L, 2L, 3000L, 1, 1, "+15551234567", "Hey, are we still meeting tonight?", 0L));
    sampleList.add(new Sms(4L, 2L, 4000L, 1, 0, "+15559876543", "Don't forget the milk", 0L));

    contactNames = new HashMap<>();
    contactNames.put("+15551234567", "Alice Wonderland");
    contactNames.put("+15559876543", "Bob Builder");
  }

  @Test
  public void filterWithNullOrEmptyQueryReturnsAllSms() {
    List<Sms> nullQuery = SmsFilter.filter(sampleList, null, contactNames);
    assertEquals(4, nullQuery.size());

    List<Sms> emptyQuery = SmsFilter.filter(sampleList, "", contactNames);
    assertEquals(4, emptyQuery.size());

    List<Sms> whitespaceQuery = SmsFilter.filter(sampleList, "   ", contactNames);
    assertEquals(4, whitespaceQuery.size());
  }

  @Test
  public void filterWithNullOrEmptyListReturnsEmptyList() {
    List<Sms> emptyInput = SmsFilter.filter(Collections.emptyList(), "test", contactNames);
    assertNotNull(emptyInput);
    assertTrue(emptyInput.isEmpty());

    List<Sms> nullInput = SmsFilter.filter(null, "test", contactNames);
    assertNotNull(nullInput);
    assertTrue(nullInput.isEmpty());
  }

  @Test
  public void filterMatchesBodyTextCaseInsensitive() {
    List<Sms> otpResult = SmsFilter.filter(sampleList, "otp", contactNames);
    assertEquals(1, otpResult.size());
    assertEquals(1L, otpResult.get(0).getId());

    List<Sms> deliveredResult = SmsFilter.filter(sampleList, "DELIVERED", contactNames);
    assertEquals(1, deliveredResult.size());
    assertEquals(2L, deliveredResult.get(0).getId());
  }

  @Test
  public void filterMatchesSenderAddressCaseInsensitive() {
    List<Sms> addressResult = SmsFilter.filter(sampleList, "hdfcbk", contactNames);
    assertEquals(1, addressResult.size());
    assertEquals(1L, addressResult.get(0).getId());

    List<Sms> swiggyResult = SmsFilter.filter(sampleList, "swiggy", contactNames);
    assertEquals(1, swiggyResult.size());
    assertEquals(2L, swiggyResult.get(0).getId());

    List<Sms> phoneResult = SmsFilter.filter(sampleList, "9876543", contactNames);
    assertEquals(1, phoneResult.size());
    assertEquals(4L, phoneResult.get(0).getId());
  }

  @Test
  public void filterMatchesResolvedContactName() {
    List<Sms> aliceResult = SmsFilter.filter(sampleList, "Alice", contactNames);
    assertEquals(1, aliceResult.size());
    assertEquals(3L, aliceResult.get(0).getId());

    List<Sms> bobResult = SmsFilter.filter(sampleList, "builder", contactNames);
    assertEquals(1, bobResult.size());
    assertEquals(4L, bobResult.get(0).getId());
  }

  @Test
  public void filterWithNoMatchesReturnsEmptyList() {
    List<Sms> noMatch = SmsFilter.filter(sampleList, "nonexistentkeywordxyz", contactNames);
    assertNotNull(noMatch);
    assertTrue(noMatch.isEmpty());
  }

  @Test
  public void filterHandlesNullFieldsGracefully() {
    List<Sms> listWithNulls = new ArrayList<>();
    listWithNulls.add(new Sms(5L, 1L, 5000L, 1, 0, null, null, 0L));
    listWithNulls.add(new Sms(6L, 1L, 6000L, 1, 0, "TEST", null, 0L));
    listWithNulls.add(new Sms(7L, 1L, 7000L, 1, 0, null, "TEST BODY", 0L));

    List<Sms> result = SmsFilter.filter(listWithNulls, "TEST", null);
    assertEquals(2, result.size());
    assertEquals(6L, result.get(0).getId());
    assertEquals(7L, result.get(1).getId());
  }
}
