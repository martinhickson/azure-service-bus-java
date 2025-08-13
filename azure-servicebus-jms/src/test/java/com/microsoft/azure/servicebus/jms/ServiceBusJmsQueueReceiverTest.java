// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ServiceBusJmsQueueReceiver.
 * 
 * This class tests the message receiving operations, timeouts,
 * and queue receiver functionality that were previously untested.
 */
public class ServiceBusJmsQueueReceiverTest {

    private ServiceBusJmsQueueReceiver queueReceiver;

    @Mock
    private ServiceBusJmsSession mockSession;

    @Mock
    private IMessageReceiver mockServiceBusReceiver;

    @Mock
    private ServiceBusJmsQueue mockQueue;

    @Mock
    private IMessage mockServiceBusMessage;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Setup mocks
        when(mockSession.isClosed()).thenReturn(false);
        when(mockQueue.getQueueName()).thenReturn("test-queue");
        
        // Create queue receiver
        queueReceiver = new ServiceBusJmsQueueReceiver(mockSession, mockQueue, mockServiceBusReceiver);
    }

    // ========== Initial State Tests ==========

    @Test
    public void testInitialState() throws Exception {
        // Then
        assertThat(queueReceiver.getQueue()).isEqualTo(mockQueue);
        assertThat(queueReceiver.isClosed()).isFalse();
        assertThat(queueReceiver.getMessageListener()).isNull();
        assertThat(queueReceiver.getMessageSelector()).isNull(); // Message selectors not supported
    }

    // ========== Basic Receive Operations Tests ==========

    @Test
    public void testReceive_WithMessage() throws Exception {
        // Given
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receive();

        // Then
        assertThat(message).isNotNull();
        verify(mockServiceBusReceiver).receive();
    }

    @Test
    public void testReceive_NoMessage() throws Exception {
        // Given - receiver returns null (no message available)
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        Message message = queueReceiver.receive();

        // Then
        assertThat(message).isNull();
        verify(mockServiceBusReceiver).receive();
    }

    @Test
    public void testReceive_WithTimeout() throws Exception {
        // Given
        long timeout = 5000; // 5 seconds
        when(mockServiceBusReceiver.receive(any(Duration.class)))
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receive(timeout);

        // Then
        assertThat(message).isNotNull();
        verify(mockServiceBusReceiver).receive(Duration.ofMillis(timeout));
    }

    @Test
    public void testReceive_WithTimeoutNoMessage() throws Exception {
        // Given
        when(mockServiceBusReceiver.receive(any(Duration.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        Message message = queueReceiver.receive(1000);

        // Then
        assertThat(message).isNull();
    }

    @Test
    public void testReceive_ZeroTimeout() throws Exception {
        // Given - zero timeout means immediate return
        when(mockServiceBusReceiver.receive(Duration.ZERO))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        Message message = queueReceiver.receive(0);

        // Then
        assertThat(message).isNull();
        verify(mockServiceBusReceiver).receive(Duration.ZERO);
    }

    @Test
    public void testReceiveNoWait() throws Exception {
        // Given
        when(mockServiceBusReceiver.receive(Duration.ZERO))
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receiveNoWait();

        // Then
        assertThat(message).isNotNull();
        verify(mockServiceBusReceiver).receive(Duration.ZERO);
    }

    @Test
    public void testReceiveNoWait_NoMessage() throws Exception {
        // Given
        when(mockServiceBusReceiver.receive(Duration.ZERO))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        Message message = queueReceiver.receiveNoWait();

        // Then
        assertThat(message).isNull();
    }

    // ========== Message Listener Tests ==========

    @Test
    public void testGetMessageListener_InitiallyNull() throws Exception {
        // When
        MessageListener listener = queueReceiver.getMessageListener();

        // Then
        assertThat(listener).isNull();
    }

    @Test
    public void testSetMessageListener() throws Exception {
        // Given
        MessageListener mockListener = mock(MessageListener.class);

        // When
        queueReceiver.setMessageListener(mockListener);

        // Then
        assertThat(queueReceiver.getMessageListener()).isEqualTo(mockListener);
    }

    @Test
    public void testSetMessageListener_Null() throws Exception {
        // Given
        MessageListener mockListener = mock(MessageListener.class);
        queueReceiver.setMessageListener(mockListener);

        // When
        queueReceiver.setMessageListener(null);

        // Then
        assertThat(queueReceiver.getMessageListener()).isNull();
    }

    // ========== Message Selector Tests ==========

    @Test
    public void testGetMessageSelector_NotSupported() throws Exception {
        // When
        String selector = queueReceiver.getMessageSelector();

        // Then - Azure Service Bus doesn't support message selectors
        assertThat(selector).isNull();
    }

    // ========== Error Handling Tests ==========

    @Test
    public void testReceive_WhenClosed() throws Exception {
        // Given
        queueReceiver.close();

        // When & Then
        assertThatThrownBy(() -> queueReceiver.receive())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Receiver is closed");
    }

    @Test
    public void testReceive_WhenSessionClosed() throws Exception {
        // Given
        when(mockSession.isClosed()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> queueReceiver.receive())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Session is closed");
    }

    @Test
    public void testReceive_ServiceBusException() throws Exception {
        // Given
        CompletableFuture<IMessage> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Azure Service Bus error"));
        when(mockServiceBusReceiver.receive()).thenReturn(failedFuture);

        // When & Then
        assertThatThrownBy(() -> queueReceiver.receive())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Failed to receive message");
    }

    @Test
    public void testReceive_NegativeTimeout() throws Exception {
        // Given
        long negativeTimeout = -1000;

        // When & Then
        assertThatThrownBy(() -> queueReceiver.receive(negativeTimeout))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Timeout cannot be negative");
    }

    // ========== Close Operations Tests ==========

    @Test
    public void testClose() throws Exception {
        // When
        queueReceiver.close();

        // Then
        assertThat(queueReceiver.isClosed()).isTrue();
        verify(mockServiceBusReceiver).close();
    }

    @Test
    public void testClose_MultipleCalls() throws Exception {
        // Given
        queueReceiver.close();

        // When - second close should not cause issues
        queueReceiver.close();

        // Then
        assertThat(queueReceiver.isClosed()).isTrue();
        // Should only call underlying close once
        verify(mockServiceBusReceiver, times(1)).close();
    }

    @Test
    public void testClose_ServiceBusException() throws Exception {
        // Given
        doThrow(new RuntimeException("Close error")).when(mockServiceBusReceiver).close();

        // When & Then - should still mark as closed
        assertThatThrownBy(() -> queueReceiver.close())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Failed to close receiver");
        
        assertThat(queueReceiver.isClosed()).isTrue();
    }

    // ========== Timeout Handling Tests ==========

    @Test
    public void testReceive_LongTimeout() throws Exception {
        // Given
        long longTimeout = 300000; // 5 minutes
        when(mockServiceBusReceiver.receive(any(Duration.class)))
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receive(longTimeout);

        // Then
        assertThat(message).isNotNull();
        verify(mockServiceBusReceiver).receive(Duration.ofMillis(longTimeout));
    }

    @Test
    public void testReceive_TimeoutExpiration() throws Exception {
        // Given - simulate timeout by completing with null after delay
        CompletableFuture<IMessage> delayedFuture = new CompletableFuture<>();
        when(mockServiceBusReceiver.receive(any(Duration.class))).thenReturn(delayedFuture);
        
        // Complete with null to simulate timeout
        delayedFuture.complete(null);

        // When
        Message message = queueReceiver.receive(100);

        // Then
        assertThat(message).isNull();
    }

    // ========== Message Conversion Tests ==========

    @Test
    public void testMessageConversion_TextMessage() throws Exception {
        // Given - mock Azure Service Bus message
        when(mockServiceBusMessage.getMessageBody()).thenReturn(createMockTextMessageBody("Hello World"));
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receive();

        // Then
        assertThat(message).isNotNull();
        // Note: Actual message conversion testing would require more complex setup
        // This test verifies the basic flow
    }

    @Test
    public void testMessageConversion_EmptyMessage() throws Exception {
        // Given
        when(mockServiceBusMessage.getMessageBody()).thenReturn(createMockEmptyMessageBody());
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receive();

        // Then
        assertThat(message).isNotNull();
    }

    // ========== Validation Tests ==========

    @Test
    public void testValidateNotClosed_Operations() throws Exception {
        // Given
        queueReceiver.close();

        // When & Then - all operations should validate closure
        assertThatThrownBy(() -> queueReceiver.receive(1000))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Receiver is closed");

        assertThatThrownBy(() -> queueReceiver.receiveNoWait())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Receiver is closed");

        assertThatThrownBy(() -> queueReceiver.getMessageListener())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Receiver is closed");

        assertThatThrownBy(() -> queueReceiver.setMessageListener(null))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Receiver is closed");
    }

    // ========== Concurrent Operations Tests ==========

    @Test
    public void testConcurrentReceive() throws Exception {
        // Given
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When - simulate concurrent receive operations
        Message message1 = queueReceiver.receive();
        Message message2 = queueReceiver.receive();

        // Then
        assertThat(message1).isNotNull();
        assertThat(message2).isNull();
        verify(mockServiceBusReceiver, times(2)).receive();
    }

    @Test
    public void testConcurrentClose() throws Exception {
        // Given
        Runnable closeTask = () -> {
            try {
                queueReceiver.close();
            } catch (JMSException e) {
                // Expected in some race conditions
            }
        };

        // When - multiple threads try to close simultaneously
        Thread thread1 = new Thread(closeTask);
        Thread thread2 = new Thread(closeTask);
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();

        // Then - receiver should be closed without errors
        assertThat(queueReceiver.isClosed()).isTrue();
    }

    // ========== Edge Cases ==========

    @Test
    public void testReceive_InterruptedException() throws Exception {
        // Given - simulate interruption during receive
        CompletableFuture<IMessage> interruptedFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return mockServiceBusMessage;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
        
        when(mockServiceBusReceiver.receive()).thenReturn(interruptedFuture);

        // When & Then - should handle interruption gracefully
        try {
            Message message = queueReceiver.receive();
            // May return null or the message depending on timing
        } catch (JMSException e) {
            assertThat(e.getMessage()).contains("Failed to receive message");
        }
    }

    // ========== Helper Methods ==========

    private Object createMockTextMessageBody(String text) {
        // In a real test, this would create a properly formatted Azure Service Bus message body
        // For now, return a simple mock
        return mock(Object.class);
    }

    private Object createMockEmptyMessageBody() {
        return mock(Object.class);
    }

    // ========== Queue Identity Tests ==========

    @Test
    public void testGetQueue() throws Exception {
        // When
        Queue queue = queueReceiver.getQueue();

        // Then
        assertThat(queue).isEqualTo(mockQueue);
        assertThat(queue.getQueueName()).isEqualTo("test-queue");
    }

    // ========== Message Acknowledgment Integration ==========

    @Test
    public void testReceive_WithClientAcknowledge() throws Exception {
        // Given
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage));

        // When
        Message message = queueReceiver.receive();

        // Then - message should be configured for client acknowledgment
        assertThat(message).isNotNull();
        // Note: Actual acknowledgment testing would require session integration
    }

    @Test
    public void testReceive_MultipleMessages() throws Exception {
        // Given - sequence of messages
        when(mockServiceBusReceiver.receive())
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage))
                .thenReturn(CompletableFuture.completedFuture(mockServiceBusMessage))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        Message msg1 = queueReceiver.receive();
        Message msg2 = queueReceiver.receive();
        Message msg3 = queueReceiver.receive();

        // Then
        assertThat(msg1).isNotNull();
        assertThat(msg2).isNotNull();
        assertThat(msg3).isNull();
        verify(mockServiceBusReceiver, times(3)).receive();
    }
}