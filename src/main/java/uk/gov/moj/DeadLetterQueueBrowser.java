package uk.gov.moj;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTextMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.test.utils.core.messaging.ConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.JmsSessionFactory;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerException;

import javax.jms.*;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toCollection;
import static uk.gov.justice.services.test.utils.core.messaging.QueueUriProvider.artemisQueueUri;


/**
 * Utility class that allows to browse and clean messages in dead letter queue
 * <p>
 * Usage: DeadLetterQueueBrowser dlqBrowser = new DeadLetterQueueBrowser(); dlqBrowser.browse() will
 * return a list of {@link String} in the dlq dlqBrowser.removeMessages() will clean dlq
 * dlqBrowser.close() will release resources
 * <p>
 * Note:It has been observed there is sometimes a delay by the time the message lands in dlq.
 * Setting a delay of few milliseconds generally resolves this.
 *
 * @author gopal
 */
public class DeadLetterQueueBrowser implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(uk.gov.justice.services.test.utils.core.messaging.DeadLetterQueueBrowser.class);

    private static final List<String> DLQ_QUEUE_URIS = artemisQueueUri();
    private static final String DLQ_NAME = "DLQ";

    private List<Session> sessions;
    private List<JmsSessionFactory> jmsSessionFactories;
    private Queue dlqQueue;
    private final ConsumerClient consumerClient;

    public DeadLetterQueueBrowser() {
        consumerClient = new ConsumerClient();
        jmsSessionFactories = Lists.newArrayList();
        sessions = Lists.newArrayList();
        initialise();
    }

    @VisibleForTesting
    DeadLetterQueueBrowser(final Queue dlqQueue, final List<Session> sessions,
                           final List<JmsSessionFactory> jmsSessionFactories, final ConsumerClient consumerClient) {
        super();
        this.sessions = sessions;
        this.jmsSessionFactories = jmsSessionFactories;
        this.dlqQueue = dlqQueue;
        this.consumerClient = consumerClient;
    }

    private void initialise() {
        try {
            DLQ_QUEUE_URIS.forEach(u -> {
                LOGGER.info("Setting up session for uri: " + u);
                final JmsSessionFactory jmsSessionFactory = new JmsSessionFactory();
                sessions.add(jmsSessionFactory.session(u));
                jmsSessionFactories.add(jmsSessionFactory);
            });
            dlqQueue = new ActiveMQQueue(DLQ_NAME);
        } catch (Exception e) {
            close();
            final String message = "Failed to start dlq message consumer for " + "queue: '" + DLQ_NAME + "', "
                    + "queueUris: '" + DLQ_QUEUE_URIS + " ";
            LOGGER.error("Fatal error initialising Artemis {}  ", message);
            throw new MessageConsumerException(message, e);
        }
    }

    /**
     * allows browsing messages text in dlq
     *
     * @return list of {@link JsonObject}
     */
    public List<JsonObject> browseAsJson() {
        return browse().stream().map(this::convert).collect(toCollection(ArrayList::new));
    }

    /**
     * allows browsing original messages in dlq
     *
     * @return list of {@link JsonObject}
     */
    public void browseFullMessages() {
        browseMessages().stream().forEach(msg -> convertMessage(msg));
    }

    /**
     * allows browsing messages in dlq
     *
     * @return list of {@link String}
     */
    public List<Message> browseMessages() {

        final List<Message> messages = new ArrayList<>();
        for (Session session : sessions) {
            try (QueueBrowser dlqBrowser = session.createBrowser(dlqQueue);) {
                final Enumeration<Message> enumeration = dlqBrowser.getEnumeration();

                while (enumeration.hasMoreElements()) {
                    messages.add(enumeration.nextElement());
                }
            } catch (JMSException e) {
                final String message = "Fatal error getting messages from DLQ";
                LOGGER.error(message);
                throw new MessageConsumerException(message, e);
            }
        }
        LOGGER.info("Total number of messages across {} brokers is {}", sessions.size(), messages.size());
        return messages;
    }

    /**
     * allows browsing messages in dlq
     *
     * @return list of {@link String}
     */
    public List<String> browse() {

        final List<String> messages = new ArrayList<>();
        for (Session session : sessions) {
            try (QueueBrowser dlqBrowser = session.createBrowser(dlqQueue);) {
                final Enumeration enumeration = dlqBrowser.getEnumeration();

                while (enumeration.hasMoreElements()) {
                    final String message = ((TextMessage) enumeration.nextElement()).getText();
                    messages.add(message);
                }


            } catch (JMSException e) {
                final String message = "Fatal error getting messages from DLQ";
                LOGGER.error(message);
                throw new MessageConsumerException(message, e);
            }
        }
        LOGGER.info("Total number of messages across {} brokers is {}", sessions.size(), messages.size());
        return messages;
    }

    /**
     * removes messages from dlq
     */
    public void removeMessages() {
        for (Session session : sessions) {
            try (MessageConsumer messageConsumer = session.createConsumer(dlqQueue)) {
                consumerClient.cleanQueue(messageConsumer);
            } catch (JMSException e) {
                final String message = "Fatal error cleaning messges from DLQ";
                LOGGER.error(message);
                throw new MessageConsumerException(message, e);
            }
        }
    }

    private JsonObject convert(final String source) {
        try (final JsonReader reader = Json.createReader(new StringReader(source))) {
            return reader.readObject();
        }
    }

    private void convertMessage(final Message source) {
        Map coreMessage = ((ActiveMQTextMessage) source).getCoreMessage().toMap();
        String messageBody = ((ActiveMQTextMessage) source).getText();
        coreMessage.put("message", messageBody);
        LOGGER.info(new JSONObject(coreMessage).toString());
    }

    /**
     * clean up resources
     */
    public void close() {
        for (JmsSessionFactory jmsSessionFactory : jmsSessionFactories) {
            jmsSessionFactory.close();
        }
    }
}
