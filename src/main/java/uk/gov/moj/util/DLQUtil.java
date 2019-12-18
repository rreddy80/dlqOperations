package uk.gov.moj.util;

import uk.gov.moj.DeadLetterQueueBrowser;

import java.io.IOException;

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
        deadLetterQueueBrowser.browseFullMessages();
    }

    public static void logMessagesAndCleanDeadLetterQueue() {
        DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        logMessages(deadLetterQueueBrowser);
        deadLetterQueueBrowser.removeMessages();
        deadLetterQueueBrowser.close();
    }

    public static void downloadAllMessages() {
        DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser();
        deadLetterQueueBrowser.downloadFullMessages();
        deadLetterQueueBrowser.close();
    }
}
