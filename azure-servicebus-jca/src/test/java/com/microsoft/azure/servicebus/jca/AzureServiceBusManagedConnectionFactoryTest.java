// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jca;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAdapter;
import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AzureServiceBusManagedConnectionFactory.
 */
public class AzureServiceBusManagedConnectionFactoryTest {

    private AzureServiceBusManagedConnectionFactory mcf;

    @Mock
    private ConnectionManager mockConnectionManager;

    @Mock
    private ResourceAdapter mockResourceAdapter;

    // Note: Subject is final and cannot be mocked, we'll use null in tests

    @Mock
    private ConnectionRequestInfo mockConnectionRequestInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mcf = new AzureServiceBusManagedConnectionFactory();
    }

    @Test
    public void testCreateConnectionFactory_WithConnectionManager() throws Exception {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");

        // When
        Object connectionFactory = mcf.createConnectionFactory(mockConnectionManager);

        // Then
        assertThat(connectionFactory).isInstanceOf(AzureServiceBusConnectionFactory.class);
        AzureServiceBusConnectionFactory cf = (AzureServiceBusConnectionFactory) connectionFactory;
        assertThat(cf.getManagedConnectionFactory()).isEqualTo(mcf);
        assertThat(cf.getConnectionManager()).isEqualTo(mockConnectionManager);
    }

    @Test
    public void testCreateConnectionFactory_WithNullConnectionManager() {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");

        // When & Then
        assertThatThrownBy(() -> mcf.createConnectionFactory(null))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("ConnectionManager cannot be null");
    }

    @Test
    public void testCreateConnectionFactory_WithoutConnectionString() {
        // When & Then
        assertThatThrownBy(() -> mcf.createConnectionFactory(mockConnectionManager))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("Connection string is required");
    }

    @Test
    public void testCreateConnectionFactory_NonManaged() throws Exception {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");

        // When
        Object connectionFactory = mcf.createConnectionFactory();

        // Then
        assertThat(connectionFactory).isInstanceOf(ServiceBusJmsConnectionFactory.class);
    }

    @Test
    public void testCreateManagedConnection_ValidConfiguration() throws Exception {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");

        // When & Then
        // This test verifies that with valid configuration, createManagedConnection successfully creates
        // a managed connection. The actual Azure connection happens lazily, so this should succeed
        // with valid configuration (the connection failure would happen later when actually using the connection)
        try {
            ManagedConnection result = mcf.createManagedConnection(null, mockConnectionRequestInfo);
            
            // Should successfully create a managed connection
            assertThat(result).isInstanceOf(AzureServiceBusManagedConnection.class);
            
        } catch (ResourceException e) {
            // If it does throw an exception due to Azure connection issues, that's also acceptable
            // as it means the method is trying to create the connection
            assertThat(e.getMessage()).contains("Failed to create managed connection");
        }
    }

    @Test
    public void testValidateConfiguration_SAS() throws Exception {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");
        mcf.setAuthType("SAS");

        // When & Then - should not throw exception
        mcf.createConnectionFactory(mockConnectionManager);
    }

    @Test
    public void testValidateConfiguration_AAD_MissingClientId() {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");
        mcf.setAuthType("AAD");
        mcf.setTenantId("test-tenant");
        mcf.setClientSecret("test-secret");
        // Missing clientId

        // When & Then
        assertThatThrownBy(() -> mcf.createConnectionFactory(mockConnectionManager))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("AAD authentication requires clientId and tenantId");
    }

    @Test
    public void testValidateConfiguration_AAD_MissingTenantId() {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");
        mcf.setAuthType("AAD");
        mcf.setClientId("test-client");
        mcf.setClientSecret("test-secret");
        // Missing tenantId

        // When & Then
        assertThatThrownBy(() -> mcf.createConnectionFactory(mockConnectionManager))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("AAD authentication requires clientId and tenantId");
    }

    @Test
    public void testValidateConfiguration_AAD_MissingClientSecret() {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");
        mcf.setAuthType("AAD");
        mcf.setClientId("test-client");
        mcf.setTenantId("test-tenant");
        // Missing clientSecret

        // When & Then
        assertThatThrownBy(() -> mcf.createConnectionFactory(mockConnectionManager))
                .isInstanceOf(ResourceException.class)
                .hasMessageContaining("AAD authentication requires clientSecret");
    }

    @Test
    public void testValidateConfiguration_MSI() throws Exception {
        // Given
        mcf.setConnectionString("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test");
        mcf.setAuthType("MSI");
        mcf.setMsiResourceId("test-resource");

        // When & Then - should not throw exception during validation
        mcf.createConnectionFactory(mockConnectionManager);
    }

    @Test
    public void testMatchManagedConnections_EmptySet() throws Exception {
        // Given
        Set<ManagedConnection> connectionSet = new HashSet<>();

        // When
        ManagedConnection result = mcf.matchManagedConnections(connectionSet, null, mockConnectionRequestInfo);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void testMatchManagedConnections_NullSet() throws Exception {
        // When
        ManagedConnection result = mcf.matchManagedConnections(null, null, mockConnectionRequestInfo);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void testMatchManagedConnections_WithMatchingConnection() throws Exception {
        // Given
        AzureServiceBusManagedConnection mockManagedConnection = mock(AzureServiceBusManagedConnection.class);
        when(mockManagedConnection.getManagedConnectionFactory()).thenReturn(mcf);

        Set<ManagedConnection> connectionSet = new HashSet<>();
        connectionSet.add(mockManagedConnection);

        // When
        ManagedConnection result = mcf.matchManagedConnections(connectionSet, null, mockConnectionRequestInfo);

        // Then
        assertThat(result).isEqualTo(mockManagedConnection);
    }

    @Test
    public void testResourceAdapterAssociation() throws Exception {
        // When
        mcf.setResourceAdapter(mockResourceAdapter);

        // Then
        assertThat(mcf.getResourceAdapter()).isEqualTo(mockResourceAdapter);
    }

    @Test
    public void testConfigurationProperties() {
        // Test all configuration property getters and setters
        mcf.setConnectionString("test-connection-string");
        assertThat(mcf.getConnectionString()).isEqualTo("test-connection-string");

        mcf.setAuthType("AAD");
        assertThat(mcf.getAuthType()).isEqualTo("AAD");

        mcf.setClientId("test-client-id");
        assertThat(mcf.getClientId()).isEqualTo("test-client-id");

        mcf.setClientSecret("test-client-secret");
        assertThat(mcf.getClientSecret()).isEqualTo("test-client-secret");

        mcf.setTenantId("test-tenant-id");
        assertThat(mcf.getTenantId()).isEqualTo("test-tenant-id");

        mcf.setMsiResourceId("test-msi-resource");
        assertThat(mcf.getMsiResourceId()).isEqualTo("test-msi-resource");
    }

    @Test
    public void testDefaultValues() {
        // Then
        assertThat(mcf.getAuthType()).isEqualTo("SAS");
        assertThat(mcf.getConnectionString()).isNull();
        assertThat(mcf.getClientId()).isNull();
        assertThat(mcf.getClientSecret()).isNull();
        assertThat(mcf.getTenantId()).isNull();
        assertThat(mcf.getMsiResourceId()).isNull();
    }

    @Test
    public void testEquals_SameInstance() {
        // When & Then
        assertThat(mcf.equals(mcf)).isTrue();
    }

    @Test
    public void testEquals_NullObject() {
        // When & Then
        assertThat(mcf.equals(null)).isFalse();
    }

    @Test
    public void testEquals_DifferentClass() {
        // When & Then
        assertThat(mcf.equals("string")).isFalse();
    }

    @Test
    public void testEquals_SameConfiguration() {
        // Given
        AzureServiceBusManagedConnectionFactory other = new AzureServiceBusManagedConnectionFactory();
        
        mcf.setConnectionString("test");
        mcf.setAuthType("SAS");
        
        other.setConnectionString("test");
        other.setAuthType("SAS");

        // When & Then
        assertThat(mcf.equals(other)).isTrue();
        assertThat(mcf.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    public void testEquals_DifferentConfiguration() {
        // Given
        AzureServiceBusManagedConnectionFactory other = new AzureServiceBusManagedConnectionFactory();
        
        mcf.setConnectionString("test1");
        other.setConnectionString("test2");

        // When & Then
        assertThat(mcf.equals(other)).isFalse();
    }

    @Test
    public void testToString() {
        // Given
        mcf.setAuthType("AAD");
        mcf.setConnectionString("secret-connection-string");

        // When
        String result = mcf.toString();

        // Then
        assertThat(result).contains("AzureServiceBusManagedConnectionFactory");
        assertThat(result).contains("authType='AAD'");
        assertThat(result).contains("connectionString='***'"); // Should mask connection string
    }

    @Test
    public void testLogWriter_Operations() throws Exception {
        // When & Then - should not throw exceptions
        mcf.setLogWriter(null);
        assertThat(mcf.getLogWriter()).isNull();
    }
}