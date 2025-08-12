// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jca;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * Metadata for Azure Service Bus Managed Connections.
 * 
 * Provides information about the Enterprise Information System (EIS) 
 * and connection capabilities.
 */
public class AzureServiceBusManagedConnectionMetaData implements ManagedConnectionMetaData {
    
    private static final String EIS_PRODUCT_NAME = "Microsoft Azure Service Bus";
    private static final String EIS_PRODUCT_VERSION = "1.0.0";
    private static final String USER_NAME = "Azure Service Bus Connection";
    
    /**
     * Returns the product name of the Enterprise Information System.
     * 
     * @return the EIS product name
     * @throws ResourceException if the information is not available
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return EIS_PRODUCT_NAME;
    }
    
    /**
     * Returns the product version of the Enterprise Information System.
     * 
     * @return the EIS product version
     * @throws ResourceException if the information is not available
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return EIS_PRODUCT_VERSION;
    }
    
    /**
     * Returns the maximum number of active connections that this
     * ManagedConnectionFactory can support across client processes.
     * 
     * @return the maximum number of connections (0 indicates no limit)
     * @throws ResourceException if the information is not available
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        // No specific limit from Azure Service Bus perspective
        // Actual limit depends on Azure Service Bus quotas and application server configuration
        return 0; // 0 indicates no specific limit
    }
    
    /**
     * Returns the user name associated with the ManagedConnection instance.
     * 
     * @return the user name
     * @throws ResourceException if the information is not available
     */
    @Override
    public String getUserName() throws ResourceException {
        return USER_NAME;
    }
}