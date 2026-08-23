package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.simmetrics.metrics.JaroWinkler;
import org.simmetrics.metrics.Levenshtein;

@RunWith(RobolectricTestRunner.class)
public class TrainSmsTest {

  private static Object invokeGetMetric(String algo) throws Exception {
    Method method = TrainSms.class.getDeclaredMethod("getMetric", String.class);
    method.setAccessible(true);
    return method.invoke(null, algo);
  }

  @Test
  public void getMetricReturnsLevenshteinForLevenshtein() throws Exception {
    Object metric = invokeGetMetric("levenshtein");
    assertNotNull(metric);
    assertEquals(Levenshtein.class, metric.getClass());
  }

  @Test
  public void getMetricFallsBackToLevenshteinForUnsupportedNormalizedLevenshtein() throws Exception {
    Object metric = invokeGetMetric("normalizedLevenshtein");
    assertNotNull(metric);
    assertEquals(Levenshtein.class, metric.getClass());
  }

  @Test
  public void getMetricReturnsJaroWinkler() throws Exception {
    Object metric = invokeGetMetric("jaroWinkler");
    assertNotNull(metric);
    assertEquals(JaroWinkler.class, metric.getClass());
  }

  @Test
  public void getMetricReturnsLevenshteinForGarbageName() throws Exception {
    Object metric = invokeGetMetric("notARealAlgorithm");
    assertNotNull(metric);
    assertEquals(Levenshtein.class, metric.getClass());
  }

  @Test
  public void getMetricReturnsLevenshteinForNullName() throws Exception {
    Object metric = invokeGetMetric(null);
    assertNotNull(metric);
    assertEquals(Levenshtein.class, metric.getClass());
  }
}
