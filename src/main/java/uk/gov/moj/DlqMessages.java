package uk.gov.moj;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.gov.moj.util.CommandLineUtil.parseCommandLineOptions;
import static uk.gov.moj.util.DLQUtil.*;


public class DlqMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(DlqMessages.class);

    public static void main(final String[] args) throws MissingArgumentException {

        parseCommandLineOptions(args);

        final String artemisHost = System.getProperty("ARTEMIS_URI");
        final String requestedOperation = System.getProperty("REQUESTED_OPERATION");
        if (artemisHost != null && requestedOperation != null) {
            LOGGER.info("Parameters are valid, trying to connect to JMS and run requested operation");
            RunRequestedOperation(requestedOperation);
        } else {
            System.exit(1);
        }

    }

    private static void RunRequestedOperation(String requestedOperation) {
        switch(requestedOperation) {
            case "listMessages":
                logMessagesInDeadletterQueue();
            case "removeMessages":
                cleanDeadLetterQueue();
            case "countMessages":
                getDeadLetterQueueMessageCount();
            case "listAndRemove":
                logMessagesAndCleanDeadLetterQueue();
        }
    }
}
