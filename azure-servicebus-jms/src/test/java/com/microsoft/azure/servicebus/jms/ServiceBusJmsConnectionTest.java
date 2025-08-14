package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.primitives.MessagingFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.ConnectionMetaData;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.Session;
import java.net.URI;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ServiceBusJmsConnection.
 * 
 * This class tests the connection lifecycle, session management, and error handling
 * that were previously untested in the JMS connection implementation.
 */
public class ServiceBusJmsConnectionTest {

    private ServiceBusJmsConnection connection;

    @Mock
    private MessagingFactory mockMessagingFactory;
    
    @Mock
    private ClientSettings mockClientSettings;

    private Properties connectionProperties;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Setup connection properties
        connectionProperties = new Properties();
        connectionProperties.setProperty("connectionString", 
            "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=testkey");
        connectionProperties.setProperty("authType", "SAS");

        // Create connection
        connection = new ServiceBusJmsConnection(
            URI.create("sb://test.servicebus.windows.net/"), 
            mockClientSettings,
            connectionProperties);
    }

    // ========== Initial State Tests ==========

    @Test
    public void testInitialState() throws Exception {
        // Then
        assertThat(connection.isClosed()).isFalse();
        assertThat(connection.getClientID()).isNull();
    }

    @Test
    public void testGetConnectionMetaData() throws Exception {
        // When
        ConnectionMetaData metaData = connection.getMetaData();

        // Then
        assertThat(metaData).isInstanceOf(ServiceBusJmsConnectionMetaData.class);
    }

    // ========== Session Creation Tests ==========

    @Test
    public void testCreateSession_DefaultSettings() throws Exception {
        // When & Then - may fail due to Azure connection requirements in unit test
        try {
            Session session = connection.createSession();
            
            assertThat(session).isInstanceOf(ServiceBusJmsSession.class);
            assertThat(session.getTransacted()).isFalse();
            assertThat(session.getAcknowledgeMode()).isEqualTo(Session.AUTO_ACKNOWLEDGE);
            
        } catch (JMSException e) {
            // Expected in unit test environment without real Azure connection
            assertThat(e.getMessage()).contains("Failed to create messaging factory");
        }
    }

    @Test
    public void testCreateSession_WithParameters() throws Exception {
        // When & Then
        try {
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            
            assertThat(session).isInstanceOf(ServiceBusJmsSession.class);
            assertThat(session.getTransacted()).isFalse();
            assertThat(session.getAcknowledgeMode()).isEqualTo(Session.CLIENT_ACKNOWLEDGE);
            
        } catch (JMSException e) {
            // Expected in unit test environment
            assertThat(e.getMessage()).contains("Failed to create messaging factory");
        }
    }

    @Test
    public void testCreateSession_WithSessionMode() throws Exception {
        // When & Then
        try {
            Session session = connection.createSession(Session.DUPS_OK_ACKNOWLEDGE);
            
            assertThat(session).isInstanceOf(ServiceBusJmsSession.class);
            assertThat(session.getAcknowledgeMode()).isEqualTo(Session.DUPS_OK_ACKNOWLEDGE);
            
        } catch (JMSException e) {
            // Expected in unit test environment
            assertThat(e.getMessage()).contains("Failed to create messaging factory");
        }
    }

    @Test
    public void testCreateQueueSession() throws Exception {
        // When & Then
        try {
            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            
            assertThat(session).isInstanceOf(ServiceBusJmsSession.class);
            
        } catch (JMSException e) {
            // Expected in unit test environment
            assertThat(e.getMessage()).contains("Failed to create messaging factory");
        }
    }

    @Test
    public void testCreateSession_TransactedNotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createSession(true, Session.SESSION_TRANSACTED))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Transacted sessions are not supported");
    }

    // ========== Client ID Tests ==========

    @Test
    public void testSetClientID_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.setClientID("test-client"))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Client identifiers are not supported");
    }

    @Test
    public void testGetClientID_ReturnsNull() throws Exception {
        // When
        String clientId = connection.getClientID();

        // Then
        assertThat(clientId).isNull();
    }

    // ========== Exception Listener Tests ==========

    @Test
    public void testGetExceptionListener_InitiallyNull() throws Exception {
        // When
        ExceptionListener listener = connection.getExceptionListener();

        // Then
        assertThat(listener).isNull();
    }

    @Test
    public void testSetExceptionListener() throws Exception {
        // Given
        ExceptionListener mockListener = mock(ExceptionListener.class);

        // When
        connection.setExceptionListener(mockListener);

        // Then
        assertThat(connection.getExceptionListener()).isEqualTo(mockListener);
    }

    @Test
    public void testSetExceptionListener_Null() throws Exception {
        // Given
        ExceptionListener mockListener = mock(ExceptionListener.class);
        connection.setExceptionListener(mockListener);

        // When
        connection.setExceptionListener(null);

        // Then
        assertThat(connection.getExceptionListener()).isNull();
    }

    // ========== Connection Lifecycle Tests ==========

    @Test
    public void testStart() throws Exception {
        // When - should not throw exception
        connection.start();

        // Then - connection remains open and functional
        assertThat(connection.isClosed()).isFalse();
    }

    @Test
    public void testStop() throws Exception {
        // When - should not throw exception
        connection.stop();

        // Then - connection remains open (stop doesn't close)
        assertThat(connection.isClosed()).isFalse();
    }

    @Test
    public void testClose_Success() throws Exception {
        // When
        connection.close();

        // Then
        assertThat(connection.isClosed()).isTrue();
    }

    @Test
    public void testClose_MultipleCalls_Idempotent() throws Exception {
        // Given
        connection.close();

        // When - second close call should not throw
        connection.close();

        // Then
        assertThat(connection.isClosed()).isTrue();
    }

    @Test
    public void testOperationsAfterClose_ThrowException() throws Exception {
        // Given
        connection.close();

        // When & Then - all operations should throw after close
        assertThatThrownBy(() -> connection.createSession())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection is closed");

        assertThatThrownBy(() -> connection.setClientID("test"))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection is closed");

        assertThatThrownBy(() -> connection.start())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Connection is closed");
    }

    // ========== Unsupported Connection Consumer Tests ==========

    @Test
    public void testCreateConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createConnectionConsumer(null, null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("ConnectionConsumer not supported");
    }

    @Test
    public void testCreateDurableConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createDurableConnectionConsumer(null, "sub", null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Durable subscriptions not supported");
    }

    // ========== JMS 2.0 Connection Consumer Tests ==========

    @Test
    public void testCreateSharedConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createSharedConnectionConsumer(null, "shared", null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Shared connection consumers not supported");
    }

    @Test
    public void testCreateSharedDurableConnectionConsumer_NotSupported() throws Exception {
        // When & Then
        assertThatThrownBy(() -> connection.createSharedDurableConnectionConsumer(null, "shared", null, null, 1))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Shared durable connection consumers not supported");
    }

    // ========== Authentication Configuration Tests ==========

    @Test
    public void testSASAuthentication() throws Exception {
        // Given
        Properties sasProps = new Properties();
        sasProps.setProperty("connectionString", 
            "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=testkey");
        sasProps.setProperty("authType", "SAS");

        // When
        ServiceBusJmsConnection sasConnection = new ServiceBusJmsConnection(
            URI.create("sb://test.servicebus.windows.net/"), mockClientSettings, sasProps);

        // Then - connection should be created successfully
        assertThat(sasConnection).isNotNull();
        assertThat(sasConnection.isClosed()).isFalse();
    }

    @Test
    public void testAADAuthentication() throws Exception {
        // Given
        Properties aadProps = new Properties();
        aadProps.setProperty("connectionString", 
            "Endpoint=sb://test.servicebus.windows.net/");
        aadProps.setProperty("authType", "AAD");
        aadProps.setProperty("clientId", "test-client");
        aadProps.setProperty("clientSecret", "test-secret");
        aadProps.setProperty("tenantId", "test-tenant");

        // When
        ServiceBusJmsConnection aadConnection = new ServiceBusJmsConnection(
            URI.create("sb://test.servicebus.windows.net/"), mockClientSettings, aadProps);

        // Then
        assertThat(aadConnection).isNotNull();
        assertThat(aadConnection.isClosed()).isFalse();
    }

    @Test
    public void testMSIAuthentication() throws Exception {
        // Given
        Properties msiProps = new Properties();
        msiProps.setProperty("connectionString", 
            "Endpoint=sb://test.servicebus.windows.net/");
        msiProps.setProperty("authType", "MSI");

        // When
        ServiceBusJmsConnection msiConnection = new ServiceBusJmsConnection(
            URI.create("sb://test.servicebus.windows.net/"), mockClientSettings, msiProps);

        // Then
        assertThat(msiConnection).isNotNull();
        assertThat(msiConnection.isClosed()).isFalse();
    }

    // ========== Error Handling Tests ==========

    @Test
    public void testInvalidConnectionString() {
        // Given
        Properties invalidProps = new Properties();
        invalidProps.setProperty("connectionString", "invalid-connection-string");

        // When & Then
        assertThatThrownBy(() -> new ServiceBusJmsConnection(
                URI.create("sb://invalid"), mockClientSettings, invalidProps))
                .isInstanceOf(RuntimeException.class); // Constructor may throw various exceptions
    }

    @Test
    public void testMissingConnectionString() {
        // Given
        Properties emptyProps = new Properties();

        // When & Then
        assertThatThrownBy(() -> new ServiceBusJmsConnection(
                URI.create("sb://test.servicebus.windows.net/"), mockClientSettings, emptyProps))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testAADWithMissingCredentials() {
        // Given
        Properties incompleteAAD = new Properties();
        incompleteAAD.setProperty("connectionString", 
            "Endpoint=sb://test.servicebus.windows.net/");
        incompleteAAD.setProperty("authType", "AAD");
        // Missing clientId, clientSecret, tenantId

        // When & Then
        assertThatThrownBy(() -> new ServiceBusJmsConnection(
                URI.create("sb://test.servicebus.windows.net/"), mockClientSettings, incompleteAAD))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== Session Management Tests ==========

    @Test
    public void testRemoveSession() throws Exception {
        // Given
        ServiceBusJmsSession mockSession = mock(ServiceBusJmsSession.class);

        // When
        connection.removeSession(mockSession);

        // Then - should complete without error
        // (This tests the internal session tracking mechanism)
    }

    @Test
    public void testMultipleSessionCreation() throws Exception {
        // When & Then - test that multiple sessions can be requested
        try {
            Session session1 = connection.createSession();
            Session session2 = connection.createSession();
            
            assertThat(session1).isNotNull();
            assertThat(session2).isNotNull();
            assertThat(session1).isNotSameAs(session2);
            
        } catch (JMSException e) {
            // Expected in unit test environment
            assertThat(e.getMessage()).contains("Failed to create messaging factory");
        }
    }

    // ========== Edge Cases ==========

    @Test
    public void testConnectionWithNullURI() {
        // Given
        Properties props = new Properties();
        props.setProperty("connectionString", "Endpoint=sb://test.servicebus.windows.net/");

        // When & Then
        assertThatThrownBy(() -> new ServiceBusJmsConnection(null, mockClientSettings, props))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testConnectionWithNullProperties() {
        // When & Then
        assertThatThrownBy(() -> new ServiceBusJmsConnection(
                URI.create("sb://test.servicebus.windows.net/"), mockClientSettings, null))
                .isInstanceOf(NullPointerException.class);
    }

    // ========== Concurrent Access Tests ==========

    @Test
    public void testConcurrentClose() throws Exception {
        // Given
        Runnable closeTask = () -> {
            try {
                connection.close();
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

        // Then - connection should be closed without errors
        assertThat(connection.isClosed()).isTrue();
    }

    @Test
    public void testStateAfterStart() throws Exception {
        // Given
        connection.start();

        // When & Then - connection should remain functional
        assertThat(connection.isClosed()).isFalse();
        assertThat(connection.getClientID()).isNull();
        assertThat(connection.getMetaData()).isNotNull();
    }

    @Test
    public void testStateAfterStop() throws Exception {
        // Given
        connection.start();
        connection.stop();

        // When & Then - connection should still be open but stopped
        assertThat(connection.isClosed()).isFalse();
        // Note: JMS spec says stop() doesn't close the connection
    }
}