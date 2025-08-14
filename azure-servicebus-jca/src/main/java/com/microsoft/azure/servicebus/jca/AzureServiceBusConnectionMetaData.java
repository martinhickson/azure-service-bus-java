package com.microsoft.azure.servicebus.jca;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Metadata for Azure Service Bus JCA connections.
 * 
 * Provides information about the JMS provider and supported features.
 */
public class AzureServiceBusConnectionMetaData implements ConnectionMetaData {
    
    private static final String JMS_VERSION = "2.0";
    private static final int JMS_MAJOR_VERSION = 2;
    private static final int JMS_MINOR_VERSION = 0;
    private static final String JMS_PROVIDER_NAME = "Microsoft Azure Service Bus JCA Adapter";
    private static final String PROVIDER_VERSION = "1.0.0";
    private static final int PROVIDER_MAJOR_VERSION = 1;
    private static final int PROVIDER_MINOR_VERSION = 0;
    
    /**
     * Gets the JMS API version.
     * 
     * @return the JMS version
     * @throws JMSException if the information is not available
     */
    @Override
    public String getJMSVersion() throws JMSException {
        return JMS_VERSION;
    }
    
    /**
     * Gets the JMS API major version number.
     * 
     * @return the JMS major version
     * @throws JMSException if the information is not available
     */
    @Override
    public int getJMSMajorVersion() throws JMSException {
        return JMS_MAJOR_VERSION;
    }
    
    /**
     * Gets the JMS API minor version number.
     * 
     * @return the JMS minor version
     * @throws JMSException if the information is not available
     */
    @Override
    public int getJMSMinorVersion() throws JMSException {
        return JMS_MINOR_VERSION;
    }
    
    /**
     * Gets the JMS provider name.
     * 
     * @return the provider name
     * @throws JMSException if the information is not available
     */
    @Override
    public String getJMSProviderName() throws JMSException {
        return JMS_PROVIDER_NAME;
    }
    
    /**
     * Gets the JMS provider version.
     * 
     * @return the provider version
     * @throws JMSException if the information is not available
     */
    @Override
    public String getProviderVersion() throws JMSException {
        return PROVIDER_VERSION;
    }
    
    /**
     * Gets the JMS provider major version number.
     * 
     * @return the provider major version
     * @throws JMSException if the information is not available
     */
    @Override
    public int getProviderMajorVersion() throws JMSException {
        return PROVIDER_MAJOR_VERSION;
    }
    
    /**
     * Gets the JMS provider minor version number.
     * 
     * @return the provider minor version
     * @throws JMSException if the information is not available
     */
    @Override
    public int getProviderMinorVersion() throws JMSException {
        return PROVIDER_MINOR_VERSION;
    }
    
    /**
     * Gets an enumeration of the JMSX property names.
     * 
     * @return an empty enumeration (JMSX properties not supported)
     * @throws JMSException if the information is not available
     */
    @Override
    public Enumeration getJMSXPropertyNames() throws JMSException {
        // JMSX properties are not supported by this implementation
        return Collections.emptyEnumeration();
    }
}