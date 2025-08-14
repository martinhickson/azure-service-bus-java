package com.microsoft.azure.servicebus.jca;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AzureServiceBusConnectionFactory.
 */
public class AzureServiceBusConnectionFactoryTest {

    private AzureServiceBusConnectionFactory connectionFactory;

    @Mock
    private AzureServiceBusManagedConnectionFactory mockMcf;

    @Mock
    private ConnectionManager mockConnectionManager;

    @Mock
    private AzureServiceBusConnection mockConnection;

    @Mock
    private Reference mockReference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connectionFactory = new AzureServiceBusConnectionFactory(mockMcf, mockConnectionManager);
    }

    @Test
    public void testConstructor() {
        // Then
        assertThat(connectionFactory.getManagedConnectionFactory()).isEqualTo(mockMcf);
        assertThat(connectionFactory.getConnectionManager()).isEqualTo(mockConnectionManager);
    }

    @Test
    public void testCreateConnection_Success() throws Exception {
        // Given
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenReturn(mockConnection);

        // When
        Connection connection = connectionFactory.createConnection();

        // Then
        assertThat(connection).isEqualTo(mockConnection);
        verify(mockConnectionManager).allocateConnection(eq(mockMcf), isNull());
    }

    @Test
    public void testCreateConnection_WithCredentials() throws Exception {
        // Given
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenReturn(mockConnection);

        // When
        Connection connection = connectionFactory.createConnection("username", "password");

        // Then
        assertThat(connection).isEqualTo(mockConnection);
        verify(mockConnectionManager).allocateConnection(eq(mockMcf), isNull());
        // Note: username and password are ignored as documented
    }

    @Test
    public void testCreateConnection_ResourceException() throws Exception {
        // Given
        ResourceException resourceException = new ResourceException("Connection failed");
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenThrow(resourceException);

        // When & Then
        assertThatThrownBy(() -> connectionFactory.createConnection())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Failed to create connection");
    }

    @Test
    public void testCreateConnection_InvalidType() throws Exception {
        // Given
        String invalidConnection = "not a connection";
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenReturn(invalidConnection);

        // When & Then
        assertThatThrownBy(() -> connectionFactory.createConnection())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Invalid connection type");
    }

    @Test
    public void testCreateQueueConnection_Success() throws Exception {
        // Given
        AzureServiceBusConnection queueConnection = mock(AzureServiceBusConnection.class);
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenReturn(queueConnection);

        // When
        QueueConnection connection = connectionFactory.createQueueConnection();

        // Then
        assertThat(connection).isEqualTo(queueConnection);
    }

    @Test
    public void testCreateQueueConnection_WithCredentials() throws Exception {
        // Given
        AzureServiceBusConnection queueConnection = mock(AzureServiceBusConnection.class);
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenReturn(queueConnection);

        // When
        QueueConnection connection = connectionFactory.createQueueConnection("username", "password");

        // Then
        assertThat(connection).isEqualTo(queueConnection);
        // Note: username and password are ignored as documented
    }

    @Test
    public void testCreateQueueConnection_NotQueueConnection() throws Exception {
        // Given - return a connection that doesn't implement QueueConnection
        Connection regularConnection = mock(Connection.class);
        when(mockConnectionManager.allocateConnection(eq(mockMcf), isNull())).thenReturn(regularConnection);

        // When & Then
        assertThatThrownBy(() -> connectionFactory.createQueueConnection())
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Invalid connection type returned by ConnectionManager");
    }

    @Test
    public void testJNDIReference() throws Exception {
        // When
        connectionFactory.setReference(mockReference);

        // Then
        assertThat(connectionFactory.getReference()).isEqualTo(mockReference);
    }

    @Test
    public void testJNDIReference_Initially_Null() throws Exception {
        // Given - fresh instance
        AzureServiceBusConnectionFactory freshFactory = new AzureServiceBusConnectionFactory(mockMcf, mockConnectionManager);

        // When & Then
        assertThat(freshFactory.getReference()).isNull();
    }

    @Test
    public void testToString() {
        // When
        String result = connectionFactory.toString();

        // Then
        assertThat(result).contains("AzureServiceBusConnectionFactory");
        assertThat(result).contains("mcf=");
        assertThat(result).contains("connectionManager=");
    }

    @Test
    public void testGetManagedConnectionFactory() {
        // When & Then
        assertThat(connectionFactory.getManagedConnectionFactory()).isEqualTo(mockMcf);
    }

    @Test
    public void testGetConnectionManager() {
        // When & Then
        assertThat(connectionFactory.getConnectionManager()).isEqualTo(mockConnectionManager);
    }
}