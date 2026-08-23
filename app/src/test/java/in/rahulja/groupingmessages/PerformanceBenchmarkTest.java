package in.rahulja.groupingmessages;

import org.junit.Test;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class PerformanceBenchmarkTest {

    private static final int ITERATIONS = 10000;
    private static final String ALGO = "levenshtein";
    private static final String S1 = "hello world";
    private static final String S2 = "hello there";

    @Test
    public void benchmarkReflectionVsDirect() throws Exception {
        // Warmup
        runReflection(100);
        runOptimized(100);

        // Measure Baseline (Reflection inside loop)
        long reflectionDuration = Long.MAX_VALUE;
        long optimizedDuration = Long.MAX_VALUE;

        // Best-of-3 rounds so JIT/GC noise cannot flip the comparison
        for (int round = 0; round < 3; round++) {
            long startTime = System.nanoTime();
            runReflection(ITERATIONS);
            long endTime = System.nanoTime();
            reflectionDuration = Math.min(reflectionDuration, endTime - startTime);

            startTime = System.nanoTime();
            runOptimized(ITERATIONS);
            endTime = System.nanoTime();
            optimizedDuration = Math.min(optimizedDuration, endTime - startTime);
        }

        System.out.println(String.format("Best reflection approach (%d iterations): %d ms",
            ITERATIONS, TimeUnit.NANOSECONDS.toMillis(reflectionDuration)));
        System.out.println(String.format("Best optimized approach (%d iterations): %d ms",
            ITERATIONS, TimeUnit.NANOSECONDS.toMillis(optimizedDuration)));

        // Assert improvement (Optimized should be at least 2x faster, usually much more)
        assertTrue("Optimized approach should be faster", optimizedDuration < reflectionDuration);

        double speedup = (double) reflectionDuration / optimizedDuration;
        System.out.println(String.format("Speedup: %.2fx", speedup));
    }

    private void runReflection(int iterations) {
        for (int i = 0; i < iterations; i++) {
            try {
                Method method = StringMetrics.class.getMethod(ALGO);
                StringMetric m = (StringMetric) method.invoke(null);
                m.compare(S1, S2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void runOptimized(int iterations) throws Exception {
        Method method = StringMetrics.class.getMethod(ALGO);
        StringMetric m = (StringMetric) method.invoke(null);

        for (int i = 0; i < iterations; i++) {
            m.compare(S1, S2);
        }
    }
}
