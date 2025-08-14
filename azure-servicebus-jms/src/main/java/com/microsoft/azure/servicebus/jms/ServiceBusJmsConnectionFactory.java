package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.security.AzureActiveDirectoryTokenProvider;
import com.microsoft.azure.servicebus.security.ManagedServiceIdentityTokenProvider;
import com.microsoft.azure.servicebus.security.SharedAccessSignatureTokenProvider;
import com.microsoft.azure.servicebus.security.TokenProvider;
import com.microsoft.azure.servicebus.ClientSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import java.net.URI;
import java.util.Properties;

/**
 * Azure Service Bus JMS ConnectionFactory implementation.
 * Creates JMS connections that wrap Azure Service Bus clients.
 */
public class ServiceBusJmsConnectionFactory implements ConnectionFactory, QueueConnectionFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceBusJmsConnectionFactory.class);
    
    // Authentication properties
    public static final String PROP_CONNECTION_STRING = "connectionString";
    public static final String PROP_AUTH_TYPE = "authType"; // SAS, AAD, MSI
    public static final String PROP_CLIENT_ID = "clientId";
    public static final String PROP_CLIENT_SECRET = "clientSecret";
    public static final String PROP_TENANT_ID = "tenantId";
    public static final String PROP_MSI_RESOURCE_ID = "msiResourceId";
    
    private final Properties connectionProperties;
    private final URI namespaceUri;
    private final String connectionString;
    
    public ServiceBusJmsConnectionFactory(Properties properties) throws JMSException {
        if (properties == null) {
            throw new JMSException("Connection properties cannot be null");
        }
        
        this.connectionProperties = new Properties();
        this.connectionProperties.putAll(properties);
        
        this.connectionString = properties.getProperty(PROP_CONNECTION_STRING);
        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new JMSException("Connection string is required");
        }
        
        try {
            ConnectionStringBuilder csb = new ConnectionStringBuilder(connectionString);
            this.namespaceUri = csb.getEndpoint();
        } catch (Exception e) {
            throw new JMSException("Invalid connection string: " + e.getMessage());
        }
    }
    
    public ServiceBusJmsConnectionFactory(String connectionString) throws JMSException {
        Properties props = new Properties();
        props.setProperty(PROP_CONNECTION_STRING, connectionString);
        props.setProperty(PROP_AUTH_TYPE, "SAS");
        
        this.connectionProperties = props;
        this.connectionString = connectionString;
        
        try {
            ConnectionStringBuilder csb = new ConnectionStringBuilder(connectionString);
            this.namespaceUri = csb.getEndpoint();
        } catch (Exception e) {
            throw new JMSException("Invalid connection string: " + e.getMessage());
        }
    }
    
    @Override
    public Connection createConnection() throws JMSException {
        return createQueueConnection();
    }
    
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        // Azure Service Bus uses token-based authentication, not username/password
        logger.warn("Username/password authentication not supported, using configured authentication");
        return createConnection();
    }
    
    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        try {
            ClientSettings clientSettings = createClientSettings();
            return new ServiceBusJmsConnection(namespaceUri, clientSettings, connectionProperties);
        } catch (Exception e) {
            throw new JMSException("Failed to create connection: " + e.getMessage());
        }
    }
    
    @Override
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        logger.warn("Username/password authentication not supported, using configured authentication");
        return createQueueConnection();
    }
    
    // JMS 2.0 methods - not supported in queue-only implementation
    @Override
    public javax.jms.JMSContext createContext() {
        throw new UnsupportedOperationException("JMSContext not supported in queue-only implementation");
    }
    
    @Override
    public javax.jms.JMSContext createContext(String userName, String password) {
        throw new UnsupportedOperationException("JMSContext not supported in queue-only implementation");
    }
    
    @Override
    public javax.jms.JMSContext createContext(String userName, String password, int sessionMode) {
        throw new UnsupportedOperationException("JMSContext not supported in queue-only implementation");
    }
    
    @Override
    public javax.jms.JMSContext createContext(int sessionMode) {
        throw new UnsupportedOperationException("JMSContext not supported in queue-only implementation");
    }
    
    private ClientSettings createClientSettings() throws Exception {
        TokenProvider tokenProvider = createTokenProvider();
        return new ClientSettings(tokenProvider);
    }
    
    private TokenProvider createTokenProvider() throws Exception {
        String authType = connectionProperties.getProperty(PROP_AUTH_TYPE, "SAS");
        
        switch (authType.toUpperCase()) {
            case "SAS":
                return createSASTokenProvider();
            case "AAD":
                return createAADTokenProvider();
            case "MSI":
                return createMSITokenProvider();
            default:
                throw new IllegalArgumentException("Unsupported authentication type: " + authType);
        }
    }
    
    private TokenProvider createSASTokenProvider() throws Exception {
        // For SAS authentication, use the static factory method
        ConnectionStringBuilder csb = new ConnectionStringBuilder(connectionString);
        return TokenProvider.createSharedAccessSignatureTokenProvider(
            csb.getSasKeyName(), 
            csb.getSasKey()
        );
    }
    
    private TokenProvider createAADTokenProvider() throws Exception {
        String clientId = connectionProperties.getProperty(PROP_CLIENT_ID);
        String clientSecret = connectionProperties.getProperty(PROP_CLIENT_SECRET);
        String tenantId = connectionProperties.getProperty(PROP_TENANT_ID);
        
        if (clientId == null || clientSecret == null || tenantId == null) {
            throw new IllegalArgumentException("Azure AD authentication requires clientId, clientSecret, and tenantId");
        }
        
        // Create authority URL for tenant
        String authorityUrl = "https://login.microsoftonline.com/" + tenantId;
        return TokenProvider.createAzureActiveDirectoryTokenProvider(authorityUrl, clientId, clientSecret);
    }
    
    private TokenProvider createMSITokenProvider() throws Exception {
        // MSI token provider doesn't accept parameters in the constructor
        return TokenProvider.createManagedServiceIdentityTokenProvider();
    }
    
    // Getters for configuration
    public URI getNamespaceUri() {
        return namespaceUri;
    }
    
    public String getConnectionString() {
        return connectionString;
    }
    
    public Properties getConnectionProperties() {
        return new Properties(connectionProperties);
    }
}