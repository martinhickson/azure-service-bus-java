// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Azure Service Bus JMS Connection Metadata implementation.
 */
public class ServiceBusJmsConnectionMetaData implements ConnectionMetaData {
    
    private static final String JMS_VERSION = "2.0";
    private static final int JMS_MAJOR_VERSION = 2;
    private static final int JMS_MINOR_VERSION = 0;
    private static final String PROVIDER_NAME = "Microsoft Azure Service Bus JMS Implementation";
    private static final String PROVIDER_VERSION = "1.0.0";
    private static final int PROVIDER_MAJOR_VERSION = 1;
    private static final int PROVIDER_MINOR_VERSION = 0;
    
    @Override
    public String getJMSVersion() throws JMSException {
        return JMS_VERSION;
    }
    
    @Override
    public int getJMSMajorVersion() throws JMSException {
        return JMS_MAJOR_VERSION;
    }
    
    @Override
    public int getJMSMinorVersion() throws JMSException {
        return JMS_MINOR_VERSION;
    }
    
    @Override
    public String getJMSProviderName() throws JMSException {
        return PROVIDER_NAME;
    }
    
    @Override
    public String getProviderVersion() throws JMSException {
        return PROVIDER_VERSION;
    }
    
    @Override
    public int getProviderMajorVersion() throws JMSException {
        return PROVIDER_MAJOR_VERSION;
    }
    
    @Override
    public int getProviderMinorVersion() throws JMSException {
        return PROVIDER_MINOR_VERSION;
    }
    
    @Override
    public Enumeration getJMSXPropertyNames() throws JMSException {
        // Return supported JMSX properties
        return Collections.enumeration(Collections.singletonList("JMSXDeliveryCount"));
    }
}