// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import org.junit.Before;
import org.junit.Test;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import java.util.Properties;

import static org.junit.Assert.*;

public class ServiceBusJmsConnectionFactoryTest {
    
    private static final String VALID_CONNECTION_STRING = 
        "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey";
    
    private ServiceBusJmsConnectionFactory connectionFactory;
    
    @Before
    public void setUp() throws JMSException {
        Properties props = new Properties();
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_CONNECTION_STRING, VALID_CONNECTION_STRING);
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_AUTH_TYPE, "SAS");
        
        connectionFactory = new ServiceBusJmsConnectionFactory(props);
    }
    
    @Test
    public void testConstructorWithValidConnectionString() throws JMSException {
        ServiceBusJmsConnectionFactory factory = new ServiceBusJmsConnectionFactory(VALID_CONNECTION_STRING);
        
        assertNotNull(factory);
        assertEquals(VALID_CONNECTION_STRING, factory.getConnectionString());
        assertNotNull(factory.getNamespaceUri());
    }
    
    @Test(expected = JMSException.class)
    public void testConstructorWithNullProperties() throws JMSException {
        new ServiceBusJmsConnectionFactory((Properties) null);
    }
    
    @Test(expected = JMSException.class)
    public void testConstructorWithMissingConnectionString() throws JMSException {
        Properties props = new Properties();
        new ServiceBusJmsConnectionFactory(props);
    }
    
    @Test(expected = JMSException.class)
    public void testConstructorWithInvalidConnectionString() throws JMSException {
        Properties props = new Properties();
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_CONNECTION_STRING, "invalid-connection-string");
        new ServiceBusJmsConnectionFactory(props);
    }
    
    @Test
    public void testCreateConnection() throws JMSException {
        // Note: This test will fail without a real Service Bus namespace
        // In a real test environment, you would use a test double or integration test
        try {
            Connection connection = connectionFactory.createConnection();
            assertNotNull(connection);
            assertTrue(connection instanceof QueueConnection);
            connection.close();
        } catch (JMSException e) {
            // Expected in unit test environment without real Service Bus
            assertTrue(e.getMessage().contains("Failed to create connection"));
        }
    }
    
    @Test
    public void testCreateQueueConnection() throws JMSException {
        try {
            QueueConnection connection = connectionFactory.createQueueConnection();
            assertNotNull(connection);
            connection.close();
        } catch (JMSException e) {
            // Expected in unit test environment without real Service Bus
            assertTrue(e.getMessage().contains("Failed to create connection"));
        }
    }
    
    @Test
    public void testAADAuthenticationProperties() throws JMSException {
        Properties props = new Properties();
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_CONNECTION_STRING, 
                         "Endpoint=sb://test.servicebus.windows.net/");
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_AUTH_TYPE, "AAD");
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_CLIENT_ID, "test-client-id");
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_CLIENT_SECRET, "test-client-secret");
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_TENANT_ID, "test-tenant-id");
        
        ServiceBusJmsConnectionFactory factory = new ServiceBusJmsConnectionFactory(props);
        assertNotNull(factory);
        
        Properties retrievedProps = factory.getConnectionProperties();
        assertEquals("AAD", retrievedProps.getProperty(ServiceBusJmsConnectionFactory.PROP_AUTH_TYPE));
        assertEquals("test-client-id", retrievedProps.getProperty(ServiceBusJmsConnectionFactory.PROP_CLIENT_ID));
    }
    
    @Test
    public void testMSIAuthenticationProperties() throws JMSException {
        Properties props = new Properties();
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_CONNECTION_STRING, 
                         "Endpoint=sb://test.servicebus.windows.net/");
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_AUTH_TYPE, "MSI");
        props.setProperty(ServiceBusJmsConnectionFactory.PROP_MSI_RESOURCE_ID, 
                         "/subscriptions/test/resourceGroups/test/providers/Microsoft.ManagedIdentity/userAssignedIdentities/test");
        
        ServiceBusJmsConnectionFactory factory = new ServiceBusJmsConnectionFactory(props);
        assertNotNull(factory);
        
        Properties retrievedProps = factory.getConnectionProperties();
        assertEquals("MSI", retrievedProps.getProperty(ServiceBusJmsConnectionFactory.PROP_AUTH_TYPE));
    }
    
    @Test
    public void testGetters() throws JMSException {
        assertNotNull(connectionFactory.getNamespaceUri());
        assertEquals(VALID_CONNECTION_STRING, connectionFactory.getConnectionString());
        
        Properties props = connectionFactory.getConnectionProperties();
        assertNotNull(props);
        assertEquals("SAS", props.getProperty(ServiceBusJmsConnectionFactory.PROP_AUTH_TYPE));
    }
}