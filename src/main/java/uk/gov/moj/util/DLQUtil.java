package uk.gov.moj.util;

import uk.gov.justice.services.test.utils.core.messaging.DeadLetterQueueBrowser;

import java.util.List;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLQUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger(DLQUtil.class);

    public static void cleanDeadLetterQueue() {
        DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        deadLetterQueueBrowser.removeMessages();
        deadLetterQueueBrowser.close();
    }

    public static void logMessagesInDeadletterQueue() {
        DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        logMessages(deadLetterQueueBrowser);
        deadLetterQueueBrowser.close();
    }

    public static int getDeadLetterQueueMessageCount() {
        try (DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser()) {
            return deadLetterQueueBrowser.browseAsJson().size();
        }
    }

    private static void logMessages(DeadLetterQueueBrowser deadLetterQueueBrowser) {
        LOGGER.info("Displaying messages in dead letter queue ");
        List<JsonObject> messages = deadLetterQueueBrowser.browseAsJson();
        messages.forEach(s -> LOGGER.info(s.toString()));
        LOGGER.info("Total number of messages in  dead letter queue {} ", messages.size());
    }

    public static void logMessagesAndCleanDeadLetterQueue() {
        DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        logMessages(deadLetterQueueBrowser);
        deadLetterQueueBrowser.removeMessages();
        deadLetterQueueBrowser.close();
    }
}
