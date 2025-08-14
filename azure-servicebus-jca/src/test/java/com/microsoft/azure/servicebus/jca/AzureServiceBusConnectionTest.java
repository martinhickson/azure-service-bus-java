package com.microsoft.azure.servicebus.jca;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.ConnectionMetaData;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.Session;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AzureServiceBusConnection.
 */
public class AzureServiceBusConnectionTest {

    private AzureServiceBusConnection connection;

    @Mock
    private AzureServiceBusManagedConnection mockManagedConnection;

    @Mock
    private Session mockSession;

    @Mock
    private QueueSession mockQueueSession;

    @Mock
    private ExceptionListener mockExceptionListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connection = new AzureServiceBusConnection(mockManagedConnection);
    }

    @Test
    public void testInitialState() {
        // Then
        assertThat(connection.getManagedConnection()).isEqualTo(mockManagedConnection);
        assertThat(connection.isValid()).isTrue();
        assertThat(connection.isClosed()).isFalse();
    }

    @Test
    public void testCreateSession_Success() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);
        when(mockManagedConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);

        // When
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Then
        assertThat(session).isEqualTo(mockSession);
        verify(mockManagedConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Test
    public void testCreateSession_Transacted() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> connection.createSession(true, Session.AUTO_ACKNOWLEDGE))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Transacted sessions are not supported");
    }

    @Test
    public void testCreateSession_WithMode() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);
        when(mockManagedConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE)).thenReturn(mockSession);

        // When
        Session session = connection.createSession(Session.CLIENT_ACKNOWLEDGE);

        // Then
        assertThat(session).isEqualTo(mockSession);
        verify(mockManagedConnection).createSession(false, Session.CLIENT_ACKNOWLEDGE);
    }

    @Test
    public void testCreateSession_DefaultMode() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);
        when(mockManagedConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);

        // When
        Session session = connection.createSession();

        // Then
        assertThat(session).isEqualTo(mockSession);
        verify(mockManagedConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Test
    public void testCreateSession_AfterClose() throws Exception {
        // Given
        connection.close();

        // When & Then
        assertThatThrownBy(() -> connection.createSession(false, Session.AUTO_ACKNOWLEDGE))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection has been closed");
    }

    @Test
    public void testCreateSession_InvalidConnection() throws Exception {
        // Given
        connection.invalidate();

        // When & Then
        assertThatThrownBy(() -> connection.createSession(false, Session.AUTO_ACKNOWLEDGE))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection handle has been invalidated");
    }

    @Test
    public void testCreateSession_DestroyedManagedConnection() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> connection.createSession(false, Session.AUTO_ACKNOWLEDGE))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Underlying managed connection has been destroyed");
    }

    @Test
    public void testCreateQueueSession_Success() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);
        when(mockManagedConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockQueueSession);

        // When
        QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        // Then
        assertThat(session).isEqualTo(mockQueueSession);
    }

    @Test
    public void testCreateQueueSession_NotQueueSession() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);
        when(mockManagedConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);

        // When & Then
        assertThatThrownBy(() -> connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Created session does not support queue operations");
    }

    @Test
    public void testGetClientID() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When
        String clientId = connection.getClientID();

        // Then
        assertThat(clientId).isNull();
    }

    @Test
    public void testSetClientID_NotSupported() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> connection.setClientID("test-client-id"))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Client identifiers are not supported");
    }

    @Test
    public void testGetMetaData() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When
        ConnectionMetaData metaData = connection.getMetaData();

        // Then
        assertThat(metaData).isInstanceOf(AzureServiceBusConnectionMetaData.class);
    }

    @Test
    public void testGetExceptionListener() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When
        ExceptionListener listener = connection.getExceptionListener();

        // Then
        assertThat(listener).isNull();
    }

    @Test
    public void testSetExceptionListener() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When & Then - should not throw exception
        connection.setExceptionListener(mockExceptionListener);
    }

    @Test
    public void testStart() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When & Then - should not throw exception
        connection.start();
    }

    @Test
    public void testStop() throws Exception {
        // Given
        when(mockManagedConnection.isDestroyed()).thenReturn(false);

        // When & Then - should not throw exception
        connection.stop();
    }

    @Test
    public void testClose_Success() throws Exception {
        // When
        connection.close();

        // Then
        assertThat(connection.isClosed()).isTrue();
        verify(mockManagedConnection).removeConnectionHandle(connection);
    }

    @Test
    public void testClose_AlreadyClosed() throws Exception {
        // Given
        connection.close();
        reset(mockManagedConnection);

        // When
        connection.close();

        // Then - should not interact with managed connection again
        verifyNoMoreInteractions(mockManagedConnection);
    }

    @Test
    public void testInvalidate() {
        // When
        connection.invalidate();

        // Then
        assertThat(connection.isValid()).isFalse();
        assertThat(connection.isClosed()).isTrue();
        assertThat(connection.getManagedConnection()).isNull();
    }

    @Test
    public void testSetManagedConnection() {
        // Given
        AzureServiceBusManagedConnection newManagedConnection = mock(AzureServiceBusManagedConnection.class);

        // When
        connection.setManagedConnection(newManagedConnection);

        // Then
        assertThat(connection.getManagedConnection()).isEqualTo(newManagedConnection);
    }

    // Test unsupported operations
    
    @Test
    public void testCreateConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createConnectionConsumer(null, null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection consumers are not supported");
    }

    @Test
    public void testCreateSharedConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createSharedConnectionConsumer(null, "test", null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Topics and shared consumers are not supported");
    }

    @Test
    public void testCreateSharedDurableConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createSharedDurableConnectionConsumer(null, "test", null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Topics and durable consumers are not supported");
    }

    @Test
    public void testCreateDurableConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createDurableConnectionConsumer(null, "test", null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Topics and durable consumers are not supported");
    }

    private void validateConnectionThrowsException() throws Exception {
        // Test various validation scenarios
        connection.invalidate();
        assertThatThrownBy(() -> connection.start())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection handle has been invalidated");
    }
}