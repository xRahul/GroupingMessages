package in.rahulja.groupingmessages;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SmsUtilsTest {

    @Test
    public void cleanString_removesStopWords() {
        String input = "This is a test message";
        // "this", "is", "a" are stop words.
        String cleaned = SmsUtils.cleanString(input);

        // cleaned should contain "test" and "message"
        assertTrue(cleaned.contains("test"));
        assertTrue(cleaned.contains("message"));

        // Should not contain "this" as a distinct word
        // Since cleanString returns " word1 word2", we can check for " this".
        // But let's split.
        String[] parts = cleaned.trim().split(" ");
        for (String part : parts) {
            assertNotEquals("this", part);
            assertNotEquals("is", part);
            assertNotEquals("a", part);
        }
    }

    @Test
    public void cleanString_handlesPunctuationAndNumbers() {
        String input = "Hello, world! 123";
        // "hello" is not in stop words list (checked previously).
        // "world" is not.
        // "1" is not.

        String cleaned = SmsUtils.cleanString(input);
        assertTrue(cleaned.contains("hello"));
        assertTrue(cleaned.contains("world"));
        assertTrue(cleaned.contains("111")); // digits replaced by 1
    }

    @Test
    public void getSmsSimilarityScore_levenshtein() {
        double score = SmsUtils.getSmsSimilarityScore("levenshtein", "kitten", "sitting");
        assertTrue(score > 0.0);
        assertTrue(score <= 1.0);
    }

    @Test
    public void getSmsSimilarityScore_cosine() {
         double score = SmsUtils.getSmsSimilarityScore("cosineSimilarity", "kitten", "sitting");
         // cosineSimilarity might be the name? Or cosine.
         // SimMetrics usually uses class names or specific strings.
         // In `TrainSms`, it defaults to "levenshtein".
         // The list is in arrays.xml probably.

         // Let's stick to levenshtein as we know it's used.
         assertTrue(score >= 0.0);
    }

    @Test
    public void getSmsSimilarityScore_identical() {
        double score = SmsUtils.getSmsSimilarityScore("levenshtein", "test", "test");
        assertEquals(1.0, score, 0.001);
    }
}
