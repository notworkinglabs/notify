package in.notwork.notify.client.queues.impl;

import com.rabbitmq.client.*;
import in.notwork.notify.client.router.Router;
import in.notwork.notify.client.queues.Queue;
import in.notwork.notify.client.util.PropertiesUtil;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static in.notwork.notify.client.util.NotifyConstants.*;

/**
 * @author rishabh.
 */
public class RabbitMQ implements Queue {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQ.class);

    protected String configuredQueue;
    protected String host;
    protected String username;
    protected String password;

    protected boolean durable;
    protected boolean autoDelete;
    protected boolean exclusive;

    private Channel channel;
    private Connection connection;

    public RabbitMQ() {
        initProperties();
    }

    private void initProperties() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading queue properties...");
        }
        configuredQueue = PropertiesUtil.getProperty(QUEUE_NAME);
        host = PropertiesUtil.getProperty(QUEUE_HOST);
        username = PropertiesUtil.getProperty(QUEUE_USERNAME);
        password = PropertiesUtil.getProperty(QUEUE_PASSWORD);
        durable = PropertiesUtil.getBooleanProperty(RMQ_DURABLE);
        autoDelete = PropertiesUtil.getBooleanProperty(RMQ_AUTO_DELETE);
        exclusive = PropertiesUtil.getBooleanProperty(RMQ_EXCLUSIVE);
        if (LOG.isDebugEnabled()) {
            LOG.debug("queue:" + configuredQueue
                    + "|host:" + host + "|username:" + username
                    + "|durable:" + durable + "|autodelete:" + autoDelete
                    + "|exclusive:" + exclusive);
        }
    }

    @Override
    public void connect() throws IOException, TimeoutException {
        Config config = new Config().withRecoveryPolicy(RecoveryPolicies.recoverAlways());
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        // TODO Figure out how to use SSL
        // factory.useSslProtocol();
        connection = Connections.create(factory, config);
        channel = connection.createChannel();
        channel.queueDeclare(configuredQueue, durable, exclusive, autoDelete, null);
    }

    @Override
    public void publish(byte[] bytes) throws IOException {
        if (null == channel) {
            throw new IOException("Connection unavailable. Please check if you are connected to the queue.");
        } else {
            channel.basicPublish("", configuredQueue, null, bytes);
        }
    }

    @Override
    public void disconnect() throws IOException, TimeoutException {
        if (null == channel || null == connection) {
            throw new IOException("Connection unavailable. Please check if you are connected to the queue.");
        } else {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Closing channel...");
            }
            channel.close();
            if (LOG.isWarnEnabled()) {
                LOG.warn("Closing connection...");
            }
            connection.close();
        }
    }

    @Override
    public void subscribe(Router router) throws IOException {
        com.rabbitmq.client.Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body)
                    throws IOException {

                String message = new String(body, "UTF-8");
                System.out.println("[" + Thread.currentThread().getName() + "] Received '" + message + "'");
                router.routeMessage(body);
            }
        };
        channel.basicConsume(configuredQueue, true, consumer);
    }
}
