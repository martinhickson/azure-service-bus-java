package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageBodyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Azure Service Bus JMS QueueReceiver implementation.
 * Wraps Azure Service Bus IMessageReceiver.
 */
public class ServiceBusJmsQueueReceiver implements QueueReceiver {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceBusJmsQueueReceiver.class);
    
    private final ServiceBusJmsSession session;
    private final Queue queue;
    private final IMessageReceiver messageReceiver;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private MessageListener messageListener;
    private String messageSelector;
    
    public ServiceBusJmsQueueReceiver(ServiceBusJmsSession session, Queue queue, IMessageReceiver messageReceiver) {
        this.session = session;
        this.queue = queue;
        this.messageReceiver = messageReceiver;
    }
    
    @Override
    public Queue getQueue() throws JMSException {
        validateNotClosed();
        return queue;
    }
    
    @Override
    public String getMessageSelector() throws JMSException {
        validateNotClosed();
        return messageSelector;
    }
    
    @Override
    public MessageListener getMessageListener() throws JMSException {
        validateNotClosed();
        return messageListener;
    }
    
    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        validateNotClosed();
        this.messageListener = listener;
        if (listener != null) {
            logger.warn("Message listeners not fully implemented - messages will not be delivered automatically");
        }
    }
    
    @Override
    public Message receive() throws JMSException {
        validateNotClosed();
        
        try {
            IMessage serviceBusMessage = messageReceiver.receive();
            
            if (serviceBusMessage == null) {
                return null;
            }
            
            return convertToJmsMessage(serviceBusMessage);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JMSException("Failed to receive message: " + e.getMessage());
        } catch (Exception e) {
            throw new JMSException("Failed to receive message: " + e.getMessage());
        }
    }
    
    @Override
    public Message receive(long timeout) throws JMSException {
        validateNotClosed();
        
        try {
            IMessage serviceBusMessage;
            if (timeout == 0) {
                // Zero timeout means immediate return (non-blocking)
                serviceBusMessage = messageReceiver.receive(Duration.ZERO);
            } else if (timeout < 0) {
                throw new JMSException("Invalid timeout: " + timeout);
            } else {
                serviceBusMessage = messageReceiver.receive(Duration.ofMillis(timeout));
            }
            
            if (serviceBusMessage == null) {
                return null;
            }
            
            return convertToJmsMessage(serviceBusMessage);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JMSException("Failed to receive message: " + e.getMessage());
        } catch (Exception e) {
            throw new JMSException("Failed to receive message: " + e.getMessage());
        }
    }
    
    @Override
    public Message receiveNoWait() throws JMSException {
        validateNotClosed();
        
        try {
            IMessage serviceBusMessage = messageReceiver.receive(Duration.ofMillis(1));
            
            if (serviceBusMessage == null) {
                return null;
            }
            
            return convertToJmsMessage(serviceBusMessage);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JMSException("Failed to receive message: " + e.getMessage());
        } catch (Exception e) {
            throw new JMSException("Failed to receive message: " + e.getMessage());
        }
    }
    
    @Override
    public void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            logger.debug("Closing queue receiver");
            try {
                messageReceiver.close();
            } catch (Exception e) {
                logger.warn("Error closing message receiver", e);
                throw new JMSException("Failed to close receiver: " + e.getMessage());
            }
        }
    }
    
    Message convertToJmsMessage(IMessage serviceBusMessage) throws JMSException { // Package private for testing
        try {
            MessageBody body = serviceBusMessage.getMessageBody();
            
            if (body == null) {
                return new ServiceBusJmsMessage(session, serviceBusMessage);
            }
            
            if (body.getBodyType() == MessageBodyType.BINARY) {
                byte[] data = body.getBinaryData().get(0);
                
                // Try to determine if it's text or binary
                if (isTextContent(data)) {
                    return new ServiceBusJmsTextMessage(session, serviceBusMessage);
                } else {
                    return new ServiceBusJmsBytesMessage(session, serviceBusMessage);
                }
            }
            
            // Default to generic message
            return new ServiceBusJmsMessage(session, serviceBusMessage);
            
        } catch (Exception e) {
            throw new JMSException("Failed to convert Service Bus message to JMS message: " + e.getMessage());
        }
    }
    
    private boolean isTextContent(byte[] data) {
        if (data == null || data.length == 0) {
            return true; // Empty content can be considered text
        }
        
        // Simple heuristic: try to decode as UTF-8 and check for printable characters
        try {
            String text = new String(data, StandardCharsets.UTF_8);
            // Check if all characters are printable or whitespace
            for (char c : text.toCharArray()) {
                if (c < 32 && c != '\t' && c != '\n' && c != '\r') {
                    return false; // Contains non-printable control characters
                }
            }
            return true;
        } catch (Exception e) {
            return false; // Not valid UTF-8
        }
    }
    
    /**
     * Returns true if this receiver is closed.
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    private void validateNotClosed() throws JMSException {
        if (closed.get()) {
            throw new JMSException("QueueReceiver is closed");
        }
        if (session.isClosed()) {
            throw new JMSException("Session is closed");
        }
    }
}