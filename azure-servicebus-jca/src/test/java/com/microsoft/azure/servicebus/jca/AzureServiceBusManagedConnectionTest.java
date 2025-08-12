// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jca;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.jms.Session;
import javax.jms.JMSException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AzureServiceBusManagedConnection.
 */
public class AzureServiceBusManagedConnectionTest {

    private AzureServiceBusManagedConnection managedConnection;

    @Mock
    private ServiceBusJmsConnection mockJmsConnection;

    @Mock
    private AzureServiceBusManagedConnectionFactory mockMcf;

    // Note: Subject is final and cannot be mocked, we'll use null in tests

    @Mock
    private ConnectionRequestInfo mockConnectionRequestInfo;

    @Mock
    private ConnectionEventListener mockListener;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        managedConnection = new AzureServiceBusManagedConnection(mockJmsConnection, mockMcf);
    }

    @Test
    public void testGetConnection_Success() throws Exception {
        // When
        Object connection = managedConnection.getConnection(null, mockConnectionRequestInfo);

        // Then
        assertThat(connection).isInstanceOf(AzureServiceBusConnection.class);
        AzureServiceBusConnection handle = (AzureServiceBusConnection) connection;
        assertThat(handle.getManagedConnection()).isEqualTo(managedConnection);
        assertThat(handle.isValid()).isTrue();
        assertThat(handle.isClosed()).isFalse();
    }

    @Test
    public void testGetConnection_AfterDestroy() throws Exception {
        // Given
        managedConnection.destroy();

        // When & Then
        assertThatThrownBy(() -> managedConnection.getConnection(null, mockConnectionRequestInfo))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Managed connection has been destroyed");
    }

    @Test
    public void testDestroy_Success() throws Exception {
        // Given
        AzureServiceBusConnection handle = (AzureServiceBusConnection) managedConnection.getConnection(null, mockConnectionRequestInfo);

        // When
        managedConnection.destroy();

        // Then
        assertThat(managedConnection.isDestroyed()).isTrue();
        assertThat(handle.isValid()).isFalse();
        verify(mockJmsConnection).close();
    }

    @Test
    public void testDestroy_AlreadyDestroyed() throws Exception {
        // Given
        managedConnection.destroy();
        reset(mockJmsConnection);

        // When
        managedConnection.destroy();

        // Then - should not call close again
        verifyNoMoreInteractions(mockJmsConnection);
    }

    @Test
    public void testDestroy_JmsException() throws Exception {
        // Given
        doThrow(new JMSException("Test exception")).when(mockJmsConnection).close();

        // When & Then
        assertThatThrownBy(() -> managedConnection.destroy())
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Failed to destroy managed connection");
    }

    @Test
    public void testCleanup_Success() throws Exception {
        // Given
        AzureServiceBusConnection handle1 = (AzureServiceBusConnection) managedConnection.getConnection(null, mockConnectionRequestInfo);
        AzureServiceBusConnection handle2 = (AzureServiceBusConnection) managedConnection.getConnection(null, mockConnectionRequestInfo);

        // When
        managedConnection.cleanup();

        // Then
        assertThat(handle1.isValid()).isFalse();
        assertThat(handle2.isValid()).isFalse();
        assertThat(managedConnection.isDestroyed()).isFalse(); // cleanup doesn't destroy the connection
    }

    @Test
    public void testAssociateConnection_Success() throws Exception {
        // Given
        AzureServiceBusConnection handle = new AzureServiceBusConnection(null);

        // When
        managedConnection.associateConnection(handle);

        // Then
        assertThat(handle.getManagedConnection()).isEqualTo(managedConnection);
    }

    @Test
    public void testAssociateConnection_AfterDestroy() throws Exception {
        // Given
        managedConnection.destroy();
        AzureServiceBusConnection handle = new AzureServiceBusConnection(null);

        // When & Then
        assertThatThrownBy(() -> managedConnection.associateConnection(handle))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Managed connection has been destroyed");
    }

    @Test
    public void testAssociateConnection_InvalidType() throws Exception {
        // Given
        String invalidConnection = "not a connection";

        // When & Then
        assertThatThrownBy(() -> managedConnection.associateConnection(invalidConnection))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Invalid connection type");
    }

    @Test
    public void testConnectionEventListeners() {
        // When
        managedConnection.addConnectionEventListener(mockListener);

        // Then
        List<ConnectionEventListener> listeners = managedConnection.getListeners();
        assertThat(listeners).contains(mockListener);

        // When
        managedConnection.removeConnectionEventListener(mockListener);

        // Then
        listeners = managedConnection.getListeners();
        assertThat(listeners).doesNotContain(mockListener);
    }

    @Test
    public void testAddConnectionEventListener_Null() {
        // When
        managedConnection.addConnectionEventListener(null);

        // Then
        List<ConnectionEventListener> listeners = managedConnection.getListeners();
        assertThat(listeners).isEmpty();
    }

    @Test
    public void testAddConnectionEventListener_Duplicate() {
        // Given
        managedConnection.addConnectionEventListener(mockListener);

        // When
        managedConnection.addConnectionEventListener(mockListener);

        // Then
        List<ConnectionEventListener> listeners = managedConnection.getListeners();
        assertThat(listeners).hasSize(1);
        assertThat(listeners).contains(mockListener);
    }

    @Test
    public void testRemoveConnectionEventListener_NotPresent() {
        // When
        managedConnection.removeConnectionEventListener(mockListener);

        // Then - should not throw exception
        List<ConnectionEventListener> listeners = managedConnection.getListeners();
        assertThat(listeners).isEmpty();
    }

    @Test
    public void testGetXAResource_NotSupported() {
        // When & Then
        assertThatThrownBy(() -> managedConnection.getXAResource())
                .isInstanceOf(NotSupportedException.class)
                .hasMessageContaining("XA transactions are not supported");
    }

    @Test
    public void testGetLocalTransaction_NotSupported() {
        // When & Then
        assertThatThrownBy(() -> managedConnection.getLocalTransaction())
                .isInstanceOf(NotSupportedException.class)
                .hasMessageContaining("Local transactions are not supported");
    }

    @Test
    public void testGetMetaData() throws Exception {
        // When
        ManagedConnectionMetaData metaData = managedConnection.getMetaData();

        // Then
        assertThat(metaData).isInstanceOf(AzureServiceBusManagedConnectionMetaData.class);
    }

    @Test
    public void testLogWriter_Operations() throws Exception {
        // When & Then - should not throw exceptions
        managedConnection.setLogWriter(null);
        assertThat(managedConnection.getLogWriter()).isNull();
    }

    @Test
    public void testCreateSession_Success() throws Exception {
        // Given
        when(mockJmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);

        // When
        Session session = managedConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Then
        assertThat(session).isEqualTo(mockSession);
        verify(mockJmsConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Test
    public void testCreateSession_AfterDestroy() throws Exception {
        // Given
        managedConnection.destroy();

        // When & Then
        assertThatThrownBy(() -> managedConnection.createSession(false, Session.AUTO_ACKNOWLEDGE))
                .isInstanceOf(JMSException.class)
                .hasMessageContaining("Managed connection has been destroyed");
    }

    @Test
    public void testRemoveConnectionHandle() throws Exception {
        // Given
        AzureServiceBusConnection handle = (AzureServiceBusConnection) managedConnection.getConnection(null, mockConnectionRequestInfo);

        // When
        managedConnection.removeConnectionHandle(handle);

        // Then - should not throw exception
        // The handle should no longer be tracked by the managed connection
    }

    @Test
    public void testGetManagedConnectionFactory() {
        // When & Then
        assertThat(managedConnection.getManagedConnectionFactory()).isEqualTo(mockMcf);
    }

    @Test
    public void testGetJmsConnection() {
        // When & Then
        assertThat(managedConnection.getJmsConnection()).isEqualTo(mockJmsConnection);
    }

    @Test
    public void testInitialState() {
        // Then
        assertThat(managedConnection.isDestroyed()).isFalse();
        assertThat(managedConnection.getListeners()).isEmpty();
        assertThat(managedConnection.getManagedConnectionFactory()).isEqualTo(mockMcf);
        assertThat(managedConnection.getJmsConnection()).isEqualTo(mockJmsConnection);
    }
}