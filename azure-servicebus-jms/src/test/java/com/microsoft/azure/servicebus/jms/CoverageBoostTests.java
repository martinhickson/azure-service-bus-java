// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageBodyType;
import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.primitives.MessagingFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 🎯 LASER-FOCUSED COVERAGE BOOSTER TESTS
 * 
 * Targeting specific branches and error paths to push coverage from 62% to 99%:
 * - Error handling branches in close() methods
 * - Exception conversion and propagation  
 * - Resource cleanup failure scenarios
 * - Message conversion edge cases
 * - State validation paths
 */
public class CoverageBoostTests {

    @Mock private ServiceBusJmsSession mockSession;
    @Mock private ServiceBusJmsConnection mockConnection;
    @Mock private MessagingFactory mockMessagingFactory;
    @Mock private ClientSettings mockClientSettings;
    @Mock private IMessageSender mockSender;
    @Mock private IMessageReceiver mockReceiver;
    @Mock private com.microsoft.azure.servicebus.Message mockMessage;
    @Mock private MessageBody mockMessageBody;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    // ========== Error Handling in Close() Methods - Critical Branch Coverage ==========

    @Test
    public void testQueueReceiver_CloseWithException() throws Exception {
        // Given - receiver that throws on close
        doThrow(new RuntimeException("Close failed")).when(mockReceiver).close();
        
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // When & Then - should handle exception gracefully (not rethrow)
        receiver.close();
        
        // Verify close was called and receiver is marked as closed
        verify(mockReceiver).close();
        assertThat(receiver.isClosed()).isTrue();
    }

    @Test
    public void testQueueSender_CloseWithException() throws Exception {
        // Given - sender that throws on close
        doThrow(new RuntimeException("Close failed")).when(mockSender).close();
        
        ServiceBusJmsQueueSender sender = new ServiceBusJmsQueueSender(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockSender);
        
        // When & Then - should handle exception gracefully
        sender.close();
        
        verify(mockSender).close();
        assertThat(sender.isClosed()).isTrue();
    }

    @Test
    public void testSession_CloseWithMultipleExceptions() throws Exception {
        // Given - session with senders/receivers that throw on close
        doThrow(new RuntimeException("Receiver close failed")).when(mockReceiver).close();
        doThrow(new RuntimeException("Sender close failed")).when(mockSender).close();
        
        ServiceBusJmsSession session = new ServiceBusJmsSession(mockConnection, mockMessagingFactory, Session.AUTO_ACKNOWLEDGE);
        
        // Manually add to internal maps to simulate active resources
        session.senders.put("test-queue", mockSender);
        session.receivers.put("test-queue", mockReceiver);
        
        // When & Then - should handle all exceptions and complete cleanup
        session.close();
        
        verify(mockReceiver).close();
        verify(mockSender).close();
        assertThat(session.isClosed()).isTrue();
        assertThat(session.senders).isEmpty();
        assertThat(session.receivers).isEmpty();
    }

    // ========== Message Conversion Edge Cases - Missing Branch Coverage ==========

    @Test
    public void testMessageConversion_NullMessageBody() throws Exception {
        // Given - message with null body
        when(mockMessage.getMessageBody()).thenReturn(null);
        
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // When - convert message
        Message result = receiver.convertToJmsMessage(mockMessage);
        
        // Then - should create generic JMS message
        assertThat(result).isInstanceOf(ServiceBusJmsMessage.class);
        assertThat(result).isNotInstanceOf(ServiceBusJmsTextMessage.class);
        assertThat(result).isNotInstanceOf(ServiceBusJmsBytesMessage.class);
    }

    @Test
    public void testMessageConversion_BinaryDataWithTextContent() throws Exception {
        // Given - binary message with text-like content
        byte[] textData = "Hello World".getBytes("UTF-8");
        when(mockMessage.getMessageBody()).thenReturn(mockMessageBody);
        when(mockMessageBody.getBodyType()).thenReturn(MessageBodyType.BINARY);
        when(mockMessageBody.getBinaryData()).thenReturn(Arrays.asList(textData));
        
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // When - convert message
        Message result = receiver.convertToJmsMessage(mockMessage);
        
        // Then - should detect text content and create TextMessage
        assertThat(result).isInstanceOf(ServiceBusJmsTextMessage.class);
    }

    @Test
    public void testMessageConversion_BinaryDataWithBinaryContent() throws Exception {
        // Given - binary message with binary content (non-text bytes)
        byte[] binaryData = {(byte) 0xFF, (byte) 0xFE, (byte) 0xFD, 0x00, 0x01};
        when(mockMessage.getMessageBody()).thenReturn(mockMessageBody);
        when(mockMessageBody.getBodyType()).thenReturn(MessageBodyType.BINARY);
        when(mockMessageBody.getBinaryData()).thenReturn(Arrays.asList(binaryData));
        
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // When - convert message  
        Message result = receiver.convertToJmsMessage(mockMessage);
        
        // Then - should create BytesMessage
        assertThat(result).isInstanceOf(ServiceBusJmsBytesMessage.class);
    }

    @Test
    public void testMessageConversion_NonBinaryBodyType() throws Exception {
        // Given - message with non-binary body type
        when(mockMessage.getMessageBody()).thenReturn(mockMessageBody);
        when(mockMessageBody.getBodyType()).thenReturn(MessageBodyType.VALUE); // Not BINARY
        
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // When - convert message
        Message result = receiver.convertToJmsMessage(mockMessage);
        
        // Then - should default to generic message
        assertThat(result).isInstanceOf(ServiceBusJmsMessage.class);
        assertThat(result).isNotInstanceOf(ServiceBusJmsTextMessage.class);
        assertThat(result).isNotInstanceOf(ServiceBusJmsBytesMessage.class);
    }

    @Test  
    public void testMessageConversion_ExceptionInConversion() throws Exception {
        // Given - message that throws during body access
        when(mockMessage.getMessageBody()).thenThrow(new RuntimeException("Body access failed"));
        
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // When & Then - should wrap in JMSException
        assertThatThrownBy(() -> receiver.convertToJmsMessage(mockMessage))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Failed to convert Service Bus message to JMS message");
    }

    // ========== State Validation Branches ==========

    @Test  
    public void testQueueReceiver_OperationsWhenClosed() throws Exception {
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // Close the receiver
        receiver.close();
        
        // All operations should throw
        assertThatThrownBy(() -> receiver.receive())
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Receiver is closed");
        
        assertThatThrownBy(() -> receiver.receive(1000))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Receiver is closed");
        
        assertThatThrownBy(() -> receiver.receiveNoWait())
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Receiver is closed");
    }

    @Test
    public void testQueueSender_OperationsWhenClosed() throws Exception {
        ServiceBusJmsQueueSender sender = new ServiceBusJmsQueueSender(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockSender);
        
        // Close the sender
        sender.close();
        
        // Operations should throw
        ServiceBusJmsTextMessage testMessage = new ServiceBusJmsTextMessage(mockSession, "test");
        
        assertThatThrownBy(() -> sender.send(testMessage))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Sender is closed");
    }

    @Test
    public void testSession_OperationsWhenClosed() throws Exception {
        ServiceBusJmsSession session = new ServiceBusJmsSession(mockConnection, mockMessagingFactory, Session.AUTO_ACKNOWLEDGE);
        
        // Close session
        session.close();
        
        // All operations should throw
        assertThatThrownBy(() -> session.createMessage())
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Session is closed");
        
        assertThatThrownBy(() -> session.createTextMessage())
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Session is closed");
        
        assertThatThrownBy(() -> session.createQueue("test"))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Session is closed");
    }

    // ========== Connection State Validation ==========

    @Test
    public void testConnection_OperationsWhenClosed() throws Exception {
        Properties props = new Properties();
        props.setProperty("connectionString", "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=key");
        
        ServiceBusJmsConnection connection = new ServiceBusJmsConnection(
            URI.create("sb://test.servicebus.windows.net/"), 
            mockClientSettings, 
            props);
        
        // Close connection
        connection.close();
        
        // Operations should throw
        assertThatThrownBy(() -> connection.createSession(false, Session.AUTO_ACKNOWLEDGE))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Connection is closed");
        
        assertThatThrownBy(() -> connection.start())
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Connection is closed");
        
        assertThatThrownBy(() -> connection.stop())
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Connection is closed");
    }

    // ========== Authentication Method Coverage ==========

    @Test
    public void testConnectionFactory_InvalidAuthType() throws Exception {
        Properties props = new Properties();
        props.setProperty("connectionString", "Endpoint=sb://test.servicebus.windows.net/");
        props.setProperty("authType", "INVALID_TYPE");
        
        ServiceBusJmsConnectionFactory factory = new ServiceBusJmsConnectionFactory(props);
        
        // Should throw for invalid auth type
        assertThatThrownBy(() -> factory.createConnection())
            .isInstanceOf(JMSException.class);
    }

    @Test
    public void testConnectionFactory_MissingConnectionString() throws Exception {
        Properties props = new Properties();
        // Missing connectionString property
        
        assertThatThrownBy(() -> new ServiceBusJmsConnectionFactory(props))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Connection string is required");
    }

    // ========== JMS 2.0 Method Coverage ==========

    @Test
    public void testConnectionFactory_CreateContext_NotSupported() throws Exception {
        Properties props = new Properties();
        props.setProperty("connectionString", "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=key");
        
        ServiceBusJmsConnectionFactory factory = new ServiceBusJmsConnectionFactory(props);
        
        // All createContext methods should throw UnsupportedOperationException
        assertThatThrownBy(() -> factory.createContext())
            .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> factory.createContext("user", "pass"))
            .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> factory.createContext(Session.AUTO_ACKNOWLEDGE))
            .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> factory.createContext("user", "pass", Session.AUTO_ACKNOWLEDGE))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========== Property Validation Coverage ==========

    @Test
    public void testMessage_InvalidPropertyNames() throws Exception {
        ServiceBusJmsMessage message = new ServiceBusJmsMessage(mockSession);
        
        // Test with null property name - should handle gracefully
        assertThatThrownBy(() -> message.setStringProperty(null, "value"))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test with empty property name
        assertThatThrownBy(() -> message.setStringProperty("", "value"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== Destination Validation ==========

    @Test
    public void testSession_CreateProducerWithNonQueue() throws Exception {
        ServiceBusJmsSession session = new ServiceBusJmsSession(mockConnection, mockMessagingFactory, Session.AUTO_ACKNOWLEDGE);
        
        // Create non-queue destination (mock Topic)
        Destination nonQueueDestination = mock(javax.jms.Topic.class);
        
        // Should throw for non-queue destinations  
        assertThatThrownBy(() -> session.createProducer(nonQueueDestination))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Only Queue destinations are supported");
    }

    @Test
    public void testSession_CreateConsumerWithNonQueue() throws Exception {
        ServiceBusJmsSession session = new ServiceBusJmsSession(mockConnection, mockMessagingFactory, Session.AUTO_ACKNOWLEDGE);
        
        // Create non-queue destination
        Destination nonQueueDestination = mock(javax.jms.Topic.class);
        
        // Should throw for non-queue destinations
        assertThatThrownBy(() -> session.createConsumer(nonQueueDestination))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Only Queue destinations are supported");
    }

    // ========== Timeout Validation ==========

    @Test
    public void testQueueReceiver_NegativeTimeout() throws Exception {
        ServiceBusJmsQueueReceiver receiver = new ServiceBusJmsQueueReceiver(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockReceiver);
        
        // Negative timeout should throw
        assertThatThrownBy(() -> receiver.receive(-1000))
            .isInstanceOf(JMSException.class)
            .hasMessageContaining("Invalid timeout");
    }

    // ========== Message Priority Validation ==========

    @Test 
    public void testQueueSender_InvalidPriority() throws Exception {
        ServiceBusJmsQueueSender sender = new ServiceBusJmsQueueSender(mockSession, 
            new ServiceBusJmsQueue("test-queue"), mockSender);
        
        ServiceBusJmsTextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");
        
        // Test priority out of range (should be 0-9)
        assertThatThrownBy(() -> sender.send(message, javax.jms.DeliveryMode.PERSISTENT, 15, 0))
            .isInstanceOf(JMSException.class);
        
        assertThatThrownBy(() -> sender.send(message, javax.jms.DeliveryMode.PERSISTENT, -1, 0))
            .isInstanceOf(JMSException.class);
    }
}