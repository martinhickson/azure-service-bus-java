package com.microsoft.azure.servicebus.jca;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnection;
import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;
import java.io.Serializable;
import java.util.Objects;
import java.util.Properties;

/**
 * Azure Service Bus Managed Connection Factory for JCA integration.
 * 
 * This factory creates managed connections to Azure Service Bus and provides
 * configuration properties for authentication and connection settings.
 */
@ConnectionDefinition(
    connectionFactory = AzureServiceBusConnectionFactory.class,
    connectionFactoryImpl = AzureServiceBusConnectionFactory.class,
    connection = AzureServiceBusConnection.class,
    connectionImpl = AzureServiceBusConnection.class
)
public class AzureServiceBusManagedConnectionFactory 
        implements ManagedConnectionFactory, ResourceAdapterAssociation, Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AzureServiceBusManagedConnectionFactory.class);
    
    // Configuration properties that can be set in WildFly admin console
    
    @ConfigProperty(description = "Azure Service Bus connection string")
    private String connectionString;
    
    @ConfigProperty(description = "Authentication type: SAS, AAD, MSI", defaultValue = "SAS")
    private String authType = "SAS";
    
    @ConfigProperty(description = "Azure AD Client ID (required for AAD authentication)")
    private String clientId;
    
    @ConfigProperty(description = "Azure AD Client Secret (required for AAD authentication)")
    private String clientSecret;
    
    @ConfigProperty(description = "Azure AD Tenant ID (required for AAD authentication)")
    private String tenantId;
    
    @ConfigProperty(description = "MSI Resource ID (optional for MSI authentication)")
    private String msiResourceId;
    
    // Resource adapter reference
    private ResourceAdapter resourceAdapter;
    
    /**
     * Creates a connection factory for managed environments (with ConnectionManager).
     * This is used by the application server for connection pooling.
     * 
     * @param cm the connection manager provided by the application server
     * @return a JCA-managed connection factory
     * @throws ResourceException if creation fails
     */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        logger.debug("Creating managed connection factory with ConnectionManager");
        
        if (cm == null) {
            throw new ResourceException("ConnectionManager cannot be null");
        }
        
        validateConfiguration();
        
        return new AzureServiceBusConnectionFactory(this, cm);
    }
    
    /**
     * Creates a connection factory for non-managed environments.
     * This returns a direct JMS ConnectionFactory without JCA pooling.
     * 
     * @return a JMS connection factory
     * @throws ResourceException if creation fails
     */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        logger.debug("Creating non-managed connection factory");
        
        try {
            validateConfiguration();
            
            // Return direct JMS ConnectionFactory for non-managed environments
            Properties props = createConnectionProperties();
            return new ServiceBusJmsConnectionFactory(props);
            
        } catch (Exception e) {
            logger.error("Failed to create non-managed connection factory", e);
            throw new ResourceException("Failed to create connection factory", e);
        }
    }
    
    /**
     * Creates a managed connection to Azure Service Bus.
     * 
     * @param subject the subject for authentication (not used in this implementation)
     * @param cri connection request info (not used in this implementation)
     * @return a managed connection wrapping a JMS connection
     * @throws ResourceException if connection creation fails
     */
    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) 
            throws ResourceException {
        logger.debug("Creating managed connection to Azure Service Bus");
        
        try {
            validateConfiguration();
            
            // Create JMS connection using our existing JMS layer
            Properties props = createConnectionProperties();
            ServiceBusJmsConnectionFactory jmsFactory = new ServiceBusJmsConnectionFactory(props);
            ServiceBusJmsConnection jmsConnection = (ServiceBusJmsConnection) jmsFactory.createConnection();
            
            // Wrap in JCA managed connection
            return new AzureServiceBusManagedConnection(jmsConnection, this);
            
        } catch (Exception e) {
            logger.error("Failed to create managed connection", e);
            throw new ResourceException("Failed to create managed connection to Azure Service Bus", e);
        }
    }
    
    /**
     * Matches an existing managed connection for reuse.
     * 
     * @param connectionSet set of existing connections to match against
     * @param subject the subject (not used)
     * @param cri connection request info (not used)
     * @return a matching managed connection, or null if none match
     * @throws ResourceException if matching fails
     */
    @Override
    public ManagedConnection matchManagedConnections(java.util.Set connectionSet, 
                                                    Subject subject, 
                                                    ConnectionRequestInfo cri) 
            throws ResourceException {
        logger.debug("Matching managed connections");
        
        if (connectionSet == null || connectionSet.isEmpty()) {
            return null;
        }
        
        // Look for a compatible managed connection
        for (Object obj : connectionSet) {
            if (obj instanceof AzureServiceBusManagedConnection) {
                AzureServiceBusManagedConnection mc = (AzureServiceBusManagedConnection) obj;
                if (mc.getManagedConnectionFactory().equals(this)) {
                    logger.debug("Found matching managed connection");
                    return mc;
                }
            }
        }
        
        logger.debug("No matching managed connection found");
        return null;
    }
    
    /**
     * Sets the log writer (not implemented - using SLF4J).
     */
    @Override
    public void setLogWriter(java.io.PrintWriter out) throws ResourceException {
        // Using SLF4J instead of PrintWriter
    }
    
    /**
     * Gets the log writer (not implemented - using SLF4J).
     */
    @Override
    public java.io.PrintWriter getLogWriter() throws ResourceException {
        // Using SLF4J instead of PrintWriter
        return null;
    }
    
    /**
     * Sets the resource adapter instance.
     */
    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.resourceAdapter = ra;
        logger.debug("Resource adapter set on ManagedConnectionFactory");
    }
    
    /**
     * Gets the resource adapter instance.
     */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }
    
    // Configuration property getters and setters
    
    public String getConnectionString() {
        return connectionString;
    }
    
    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }
    
    public String getAuthType() {
        return authType;
    }
    
    public void setAuthType(String authType) {
        this.authType = authType;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getMsiResourceId() {
        return msiResourceId;
    }
    
    public void setMsiResourceId(String msiResourceId) {
        this.msiResourceId = msiResourceId;
    }
    
    // Helper methods
    
    /**
     * Creates properties for JMS connection factory.
     */
    private Properties createConnectionProperties() {
        Properties props = new Properties();
        props.setProperty("connectionString", connectionString);
        props.setProperty("authType", authType);
        
        if (clientId != null) {
            props.setProperty("clientId", clientId);
        }
        if (clientSecret != null) {
            props.setProperty("clientSecret", clientSecret);
        }
        if (tenantId != null) {
            props.setProperty("tenantId", tenantId);
        }
        if (msiResourceId != null) {
            props.setProperty("msiResourceId", msiResourceId);
        }
        
        return props;
    }
    
    /**
     * Validates the configuration before creating connections.
     */
    private void validateConfiguration() throws ResourceException {
        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new ResourceException("Connection string is required");
        }
        
        if (authType == null) {
            authType = "SAS";
        }
        
        // Validate authentication-specific requirements
        switch (authType.toUpperCase()) {
            case "AAD":
                if (clientId == null || tenantId == null) {
                    throw new ResourceException("AAD authentication requires clientId and tenantId");
                }
                if (clientSecret == null) {
                    throw new ResourceException("AAD authentication requires clientSecret");
                }
                break;
            case "MSI":
                // MSI doesn't require additional parameters
                break;
            case "SAS":
            default:
                // SAS authentication uses connection string only
                break;
        }
    }
    
    // Object methods for proper connection matching
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AzureServiceBusManagedConnectionFactory that = (AzureServiceBusManagedConnectionFactory) obj;
        
        return Objects.equals(connectionString, that.connectionString) &&
               Objects.equals(authType, that.authType) &&
               Objects.equals(clientId, that.clientId) &&
               Objects.equals(clientSecret, that.clientSecret) &&
               Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(msiResourceId, that.msiResourceId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(connectionString, authType, clientId, clientSecret, tenantId, msiResourceId);
    }
    
    @Override
    public String toString() {
        return "AzureServiceBusManagedConnectionFactory{" +
               "authType='" + authType + '\'' +
               ", connectionString='***'" +
               '}';
    }
}