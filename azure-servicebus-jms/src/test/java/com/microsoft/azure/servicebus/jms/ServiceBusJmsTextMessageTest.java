package com.microsoft.azure.servicebus.jms;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.JMSException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ServiceBusJmsTextMessageTest {
    
    @Mock
    private ServiceBusJmsSession mockSession;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testConstructorWithSession() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        assertNotNull(message);
        assertNull(message.getText());
        assertNotNull(message.getServiceBusMessage());
    }
    
    @Test
    public void testConstructorWithText() throws JMSException {
        String testText = "Hello, World!";
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession, testText);
        
        assertNotNull(message);
        assertEquals(testText, message.getText());
        assertNotNull(message.getServiceBusMessage());
    }
    
    @Test
    public void testSetAndGetText() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        String testText = "Test message content";
        message.setText(testText);
        
        assertEquals(testText, message.getText());
    }
    
    @Test
    public void testSetTextToNull() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession, "Initial text");
        
        message.setText(null);
        
        assertNull(message.getText());
    }
    
    @Test
    public void testSetTextToEmpty() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        message.setText("");
        
        assertEquals("", message.getText());
    }
    
    @Test
    public void testSetTextWithUnicodeCharacters() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        String unicodeText = "Hello 世界! Ñiño café 🌍";
        message.setText(unicodeText);
        
        assertEquals(unicodeText, message.getText());
    }
    
    @Test
    public void testClearBody() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession, "Some text");
        
        assertNotNull(message.getText());
        
        message.clearBody();
        
        assertNull(message.getText());
    }
    
    @Test
    public void testLargeText() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        // Create a large text string
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("This is line ").append(i).append(" of the large text message.\n");
        }
        
        String largeString = largeText.toString();
        message.setText(largeString);
        
        assertEquals(largeString, message.getText());
    }
    
    @Test
    public void testMessageProperties() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession, "Test text");
        
        // Test setting and getting properties
        message.setStringProperty("testProp", "testValue");
        assertEquals("testValue", message.getStringProperty("testProp"));
        
        message.setIntProperty("intProp", 42);
        assertEquals(42, message.getIntProperty("intProp"));
        
        message.setBooleanProperty("boolProp", true);
        assertTrue(message.getBooleanProperty("boolProp"));
    }
    
    @Test
    public void testJMSMessageID() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        // Should have a default message ID
        assertNotNull(message.getJMSMessageID());
        assertTrue(message.getJMSMessageID().startsWith("ID:"));
        
        // Test setting custom message ID
        String customId = "ID:custom-message-123";
        message.setJMSMessageID(customId);
        assertEquals(customId, message.getJMSMessageID());
    }
    
    @Test
    public void testJMSTimestamp() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        // Should have a timestamp set
        assertTrue(message.getJMSTimestamp() > 0);
        assertTrue(message.getJMSTimestamp() <= System.currentTimeMillis());
        
        // Test setting custom timestamp
        long customTimestamp = 1234567890L;
        message.setJMSTimestamp(customTimestamp);
        assertEquals(customTimestamp, message.getJMSTimestamp());
    }
    
    @Test
    public void testJMSCorrelationID() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        // Initially should be null
        assertNull(message.getJMSCorrelationID());
        
        // Test setting correlation ID
        String correlationId = "correlation-123";
        message.setJMSCorrelationID(correlationId);
        assertEquals(correlationId, message.getJMSCorrelationID());
        
        // Test setting to null
        message.setJMSCorrelationID(null);
        assertNull(message.getJMSCorrelationID());
    }
    
    @Test
    public void testJMSExpiration() throws JMSException {
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession);
        
        // Initially should be 0 (no expiration)
        assertEquals(0, message.getJMSExpiration());
        
        // Test setting expiration
        long expiration = System.currentTimeMillis() + 60000; // 1 minute from now
        message.setJMSExpiration(expiration);
        assertEquals(expiration, message.getJMSExpiration());
    }
}