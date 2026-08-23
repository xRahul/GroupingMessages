package in.rahulja.groupingmessages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import in.rahulja.groupingmessages.classify.SmsCategorizer;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Functional timing budget for the new categorization engine: a full batch
 * build plus classifying 100 messages must stay well under 2 seconds.
 */
public class PerformanceBenchmarkTest {

    private static final long BUDGET_MS = 2000;
    private static final int MESSAGE_COUNT = 100;

    private static final List<String> STOPWORDS = Arrays.asList(
        "the", "is", "a", "an", "and", "or", "for", "to", "of", "in", "on", "your", "you");

    private static final List<String> EXEMPLARS = Arrays.asList(
        "use 123456 to login to your account do not share this otp with anyone",
        "your one time password is 456789 valid for 10 minutes only",
        "987654 is your verification code for secure account access",
        "rs 2500 debited from account xx1234 on 12 05 available balance rs 15000",
        "your account xx9876 credited with rs 5000 on 15 06 ref no 223344",
        "rs 12000 spent on debit card ending 4321 at amazon on 03 07",
        "get flat 50 percent off on all orders today limited time offer shop now",
        "mega sale up to 70 percent discount on shoes and fashion grab the deal now",
        "exclusive offer buy one get one free only for premium members this weekend",
        "hey are you coming to the party tonight let me know soon",
        "thanks for the help yesterday really appreciate it call me when free",
        "lunch tomorrow at the usual place see you there at 1 pm");

    @Test
    public void batchBuildAndClassifyHundredMessagesStayUnderTwoSeconds() {
        long start = System.nanoTime();

        SmsCategorizer.Batch batch =
            SmsCategorizer.Batch.build(EXEMPLARS, STOPWORDS, SmsCategorizer.MODE_BALANCED);
        assertEquals(EXEMPLARS.size(), batch.size());
        double totalScore = 0.0;
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            double[] scores = batch.scores("otp code " + i + " valid for your login today");
            assertEquals(EXEMPLARS.size(), scores.length);
            totalScore += scores[0];
        }

        long elapsedMs =
            (System.nanoTime() - start) / 1_000_000L;
        System.out.println(String.format(
            "Batch build + %d classifications: %d ms", MESSAGE_COUNT, elapsedMs));
        assertTrue("expected engine work < " + BUDGET_MS + " ms, took " + elapsedMs + " ms",
            elapsedMs < BUDGET_MS);
        assertTrue("scores should be non-degenerate", totalScore > 0.0);
    }
}
