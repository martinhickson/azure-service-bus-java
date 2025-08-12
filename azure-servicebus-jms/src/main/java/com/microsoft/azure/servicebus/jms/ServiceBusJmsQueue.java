// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import javax.jms.JMSException;
import javax.jms.Queue;

/**
 * Azure Service Bus JMS Queue implementation.
 */
public class ServiceBusJmsQueue implements Queue {
    
    private final String queueName;
    
    public ServiceBusJmsQueue(String queueName) throws JMSException {
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new JMSException("Queue name cannot be null or empty");
        }
        this.queueName = queueName.trim();
    }
    
    @Override
    public String getQueueName() throws JMSException {
        return queueName;
    }
    
    @Override
    public String toString() {
        return "ServiceBusQueue[" + queueName + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Queue)) return false;
        try {
            return queueName.equals(((Queue) obj).getQueueName());
        } catch (JMSException e) {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return queueName.hashCode();
    }
}