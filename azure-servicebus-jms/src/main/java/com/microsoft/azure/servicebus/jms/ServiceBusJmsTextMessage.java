package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageBodyType;
import com.microsoft.azure.servicebus.Utils;

import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import javax.jms.TextMessage;
import java.nio.charset.StandardCharsets;

/**
 * Azure Service Bus JMS TextMessage implementation.
 */
public class ServiceBusJmsTextMessage extends ServiceBusJmsMessage implements TextMessage {
    
    private String text;
    
    public ServiceBusJmsTextMessage(ServiceBusJmsSession session) {
        super(session);
        this.text = null;
    }
    
    public ServiceBusJmsTextMessage(ServiceBusJmsSession session, IMessage serviceBusMessage) {
        super(session, serviceBusMessage);
        this.text = extractTextFromServiceBusMessage(serviceBusMessage);
    }
    
    public ServiceBusJmsTextMessage(ServiceBusJmsSession session, String text) {
        super(session);
        this.text = text;
        updateServiceBusMessage();
    }
    
    @Override
    public void setText(String text) throws JMSException {
        this.text = text;
        updateServiceBusMessage();
    }
    
    @Override
    public String getText() throws JMSException {
        return text;
    }
    
    @Override
    public void clearBody() throws JMSException {
        this.text = null;
        serviceBusMessage.setMessageBody(Utils.fromBinary(new byte[0]));
    }
    
    // JMS 2.0 method
    @Override
    public <T> T getBody(Class<T> c) throws JMSException {
        if (c == null) {
            throw new MessageFormatException("Class cannot be null");
        }
        if (String.class.isAssignableFrom(c)) {
            return c.cast(getText());
        }
        throw new MessageFormatException("TextMessage body cannot be assigned to " + c.getName());
    }
    
    @Override
    public boolean isBodyAssignableTo(Class c) throws JMSException {
        if (c == null) {
            return false;
        }
        return String.class.isAssignableFrom(c);
    }
    
    private void updateServiceBusMessage() {
        if (text == null) {
            serviceBusMessage.setMessageBody(Utils.fromBinary(new byte[0]));
        } else {
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            serviceBusMessage.setMessageBody(Utils.fromBinary(textBytes));
        }
    }
    
    private String extractTextFromServiceBusMessage(IMessage serviceBusMessage) {
        try {
            MessageBody body = serviceBusMessage.getMessageBody();
            if (body == null) {
                return null;
            }
            
            if (body.getBodyType() == MessageBodyType.BINARY) {
                byte[] data = body.getBinaryData().get(0);
                return new String(data, StandardCharsets.UTF_8);
            }
            
            throw new MessageFormatException("Message body is not text compatible");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from Service Bus message", e);
        }
    }
}