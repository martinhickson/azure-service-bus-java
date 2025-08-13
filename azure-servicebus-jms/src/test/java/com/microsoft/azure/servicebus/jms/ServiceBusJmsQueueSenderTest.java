// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessageSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ServiceBusJmsQueueSender.
 * 
 * This class tests the message sending operations, delivery modes,
 * and queue sender functionality that were previously untested.
 */
public class ServiceBusJmsQueueSenderTest {

    private ServiceBusJmsQueueSender queueSender;

    @Mock
    private ServiceBusJmsSession mockSession;

    @Mock
    private IMessageSender mockServiceBusSender;

    @Mock
    private ServiceBusJmsQueue mockQueue;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Setup mocks
        when(mockSession.isClosed()).thenReturn(false);
        when(mockQueue.getQueueName()).thenReturn("test-queue");
        
        // Mock successful send operations  
        when(mockServiceBusSender.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        
        // Create queue sender
        queueSender = new ServiceBusJmsQueueSender(mockSession, mockQueue, mockServiceBusSender);
    }

    // ========== Initial State Tests ==========

    @Test
    public void testInitialState() throws Exception {
        // Then
        assertThat(queueSender.getQueue()).isEqualTo(mockQueue);
        assertThat(queueSender.getDestination()).isEqualTo(mockQueue);
        assertThat(queueSender.isClosed()).isFalse();
        assertThat(queueSender.getDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
        assertThat(queueSender.getPriority()).isEqualTo(Message.DEFAULT_PRIORITY);
        assertThat(queueSender.getTimeToLive()).isEqualTo(Message.DEFAULT_TIME_TO_LIVE);
    }

    // ========== Basic Send Operations Tests ==========

    @Test
    public void testSend_WithMessage() throws Exception {
        // Given
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "Hello World");

        // When
        queueSender.send(message);

        // Then
        verify(mockServiceBusSender).send(any());
        assertThat(message.getJMSDestination()).isEqualTo(mockQueue);
        assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
        assertThat(message.getJMSPriority()).isEqualTo(Message.DEFAULT_PRIORITY);
    }

    @Test
    public void testSend_WithDestinationAndMessage() throws Exception {
        // Given
        ServiceBusJmsQueue targetQueue = new ServiceBusJmsQueue("target-queue");
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "Hello World");

        // When
        queueSender.send(targetQueue, message);

        // Then
        verify(mockServiceBusSender).send(any());
        assertThat(message.getJMSDestination()).isEqualTo(targetQueue);
    }

    @Test
    public void testSend_WithFullParameters() throws Exception {
        // Given
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "Hello World");
        int deliveryMode = DeliveryMode.NON_PERSISTENT;
        int priority = 7;
        long timeToLive = 60000; // 1 minute

        // When
        queueSender.send(message, deliveryMode, priority, timeToLive);

        // Then
        verify(mockServiceBusSender).send(any());
        assertThat(message.getJMSDeliveryMode()).isEqualTo(deliveryMode);
        assertThat(message.getJMSPriority()).isEqualTo(priority);
    }

    @Test
    public void testSend_WithDestinationAndFullParameters() throws Exception {
        // Given
        ServiceBusJmsQueue targetQueue = new ServiceBusJmsQueue("target-queue");
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "Hello World");

        // When
        queueSender.send(targetQueue, message, DeliveryMode.NON_PERSISTENT, 5, 30000);

        // Then
        verify(mockServiceBusSender).send(any());
        assertThat(message.getJMSDestination()).isEqualTo(targetQueue);
        assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.NON_PERSISTENT);
        assertThat(message.getJMSPriority()).isEqualTo(5);
    }

    // ========== Property Configuration Tests ==========

    @Test
    public void testSetDeliveryMode() throws Exception {
        // When
        queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        // Then
        assertThat(queueSender.getDeliveryMode()).isEqualTo(DeliveryMode.NON_PERSISTENT);
    }

    @Test
    public void testSetPriority() throws Exception {
        // When
        queueSender.setPriority(8);

        // Then
        assertThat(queueSender.getPriority()).isEqualTo(8);
    }

    @Test
    public void testSetPriority_InvalidValue() throws Exception {
        // When & Then - invalid priority values should be accepted but may be normalized
        queueSender.setPriority(-1);
        // JMS spec says priority should be 0-9, but implementation may handle invalid values
        
        queueSender.setPriority(15);
        // Similarly for values above 9
    }

    @Test
    public void testSetTimeToLive() throws Exception {
        // When
        queueSender.setTimeToLive(120000); // 2 minutes

        // Then
        assertThat(queueSender.getTimeToLive()).isEqualTo(120000);
    }

    @Test
    public void testSetTimeToLive_Zero() throws Exception {
        // When
        queueSender.setTimeToLive(0); // No expiration

        // Then
        assertThat(queueSender.getTimeToLive()).isEqualTo(0);
    }

    // ========== JMS 2.0 Delivery Delay Tests ==========

    @Test
    public void testGetDeliveryDelay() throws Exception {
        // When
        long delay = queueSender.getDeliveryDelay();

        // Then - Azure Service Bus doesn't support delivery delay
        assertThat(delay).isEqualTo(0);
    }

    @Test
    public void testSetDeliveryDelay_Zero() throws Exception {
        // When - should not throw exception
        queueSender.setDeliveryDelay(0);

        // Then
        assertThat(queueSender.getDeliveryDelay()).isEqualTo(0);
    }

    @Test
    public void testSetDeliveryDelay_NonZero_NotSupported() throws Exception {
        // When & Then - non-zero delivery delay should throw exception
        assertThatThrownBy(() -> queueSender.setDeliveryDelay(5000))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Delivery delay not supported");
    }

    // ========== Error Handling Tests ==========

    @Test
    public void testSend_WhenClosed() throws Exception {
        // Given
        queueSender.close();
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When & Then
        assertThatThrownBy(() -> queueSender.send(message))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Sender is closed");
    }

    @Test
    public void testSend_WhenSessionClosed() throws Exception {
        // Given
        when(mockSession.isClosed()).thenReturn(true);
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When & Then
        assertThatThrownBy(() -> queueSender.send(message))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Session is closed");
    }

    @Test
    public void testSend_WithNullMessage() throws Exception {
        // When & Then
        assertThatThrownBy(() -> queueSender.send(null))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Message cannot be null");
    }

    @Test
    public void testSend_WithInvalidDestination() throws Exception {
        // Given
        Destination invalidDestination = mock(Destination.class); // Not a Queue
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When & Then
        assertThatThrownBy(() -> queueSender.send(invalidDestination, message))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Only Queue destinations are supported");
    }

    @Test
    public void testSend_ServiceBusException() throws Exception {
        // Given
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Azure Service Bus error"));
        when(mockServiceBusSender.send(any())).thenReturn(failedFuture);
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When & Then
        assertThatThrownBy(() -> queueSender.send(message))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Failed to send message");
    }

    // ========== JMS 2.0 CompletionListener Tests (Unsupported) ==========

    @Test
    public void testSend_WithCompletionListener_NotSupported() throws Exception {
        // Given
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When & Then
        assertThatThrownBy(() -> queueSender.send(message, null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("CompletionListener not supported");
    }

    @Test
    public void testSend_WithDestinationAndCompletionListener_NotSupported() throws Exception {
        // Given
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When & Then
        assertThatThrownBy(() -> queueSender.send(mockQueue, message, null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("CompletionListener not supported");
    }

    // ========== Close Operations Tests ==========

    @Test
    public void testClose() throws Exception {
        // When
        queueSender.close();

        // Then
        assertThat(queueSender.isClosed()).isTrue();
        verify(mockServiceBusSender).close();
    }

    @Test
    public void testClose_MultipleCalls() throws Exception {
        // Given
        queueSender.close();

        // When - second close should not cause issues
        queueSender.close();

        // Then
        assertThat(queueSender.isClosed()).isTrue();
        // Should only call underlying close once
        verify(mockServiceBusSender, times(1)).close();
    }

    @Test
    public void testClose_ServiceBusException() throws Exception {
        // Given
        doThrow(new RuntimeException("Close error")).when(mockServiceBusSender).close();

        // When & Then - should still mark as closed
        assertThatThrownBy(() -> queueSender.close())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Failed to close sender");
        
        assertThat(queueSender.isClosed()).isTrue();
    }

    // ========== Message Property Setting Tests ==========

    @Test
    public void testSend_SetsMessageProperties() throws Exception {
        // Given
        queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        queueSender.setPriority(7);
        queueSender.setTimeToLive(30000);
        
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "Hello World");

        // When
        queueSender.send(message);

        // Then - message should have sender's default properties
        assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.NON_PERSISTENT);
        assertThat(message.getJMSPriority()).isEqualTo(7);
        assertThat(message.getJMSDestination()).isEqualTo(mockQueue);
    }

    @Test
    public void testSend_MessageTimestamp() throws Exception {
        // Given
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");
        long beforeSend = System.currentTimeMillis();

        // When
        queueSender.send(message);

        // Then - timestamp should be set
        long afterSend = System.currentTimeMillis();
        assertThat(message.getJMSTimestamp()).isBetween(beforeSend, afterSend);
    }

    @Test
    public void testSend_MessageExpiration_WithTTL() throws Exception {
        // Given
        long ttl = 60000; // 1 minute
        queueSender.setTimeToLive(ttl);
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");
        long beforeSend = System.currentTimeMillis();

        // When
        queueSender.send(message);

        // Then
        long expectedExpiration = beforeSend + ttl;
        long actualExpiration = message.getJMSExpiration();
        
        // Allow for some timing variance
        assertThat(actualExpiration).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
    }

    @Test
    public void testSend_MessageExpiration_NoTTL() throws Exception {
        // Given
        queueSender.setTimeToLive(0); // No expiration
        TextMessage message = new ServiceBusJmsTextMessage(mockSession, "test");

        // When
        queueSender.send(message);

        // Then - no expiration should be set
        assertThat(message.getJMSExpiration()).isEqualTo(0);
    }

    // ========== Edge Cases ==========

    @Test
    public void testSend_EmptyMessage() throws Exception {
        // Given
        TextMessage emptyMessage = new ServiceBusJmsTextMessage(mockSession);

        // When
        queueSender.send(emptyMessage);

        // Then - should send successfully
        verify(mockServiceBusSender).send(any());
        assertThat(emptyMessage.getJMSDestination()).isEqualTo(mockQueue);
    }

    @Test
    public void testSend_LargeMessage() throws Exception {
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is a large message content to test large message handling. ");
        }
        TextMessage largeMessage = new ServiceBusJmsTextMessage(mockSession, largeContent.toString());

        // When
        queueSender.send(largeMessage);

        // Then
        verify(mockServiceBusSender).send(any());
        assertThat(largeMessage.getText()).hasSize(largeContent.length());
    }

    // ========== Validation Tests ==========

    @Test
    public void testValidateNotClosed_Operations() throws Exception {
        // Given
        queueSender.close();

        // When & Then - all operations should validate closure
        assertThatThrownBy(() -> queueSender.getDeliveryMode())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Sender is closed");

        assertThatThrownBy(() -> queueSender.setDeliveryMode(DeliveryMode.PERSISTENT))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Sender is closed");

        assertThatThrownBy(() -> queueSender.getPriority())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Sender is closed");
    }
}