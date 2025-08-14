package com.microsoft.azure.servicebus.jca;

import org.junit.Test;

import javax.jms.JMSException;
import javax.resource.ResourceException;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for metadata classes.
 */
public class MetaDataTest {

    @Test
    public void testAzureServiceBusManagedConnectionMetaData() throws Exception {
        // Given
        AzureServiceBusManagedConnectionMetaData metaData = new AzureServiceBusManagedConnectionMetaData();

        // When & Then
        assertThat(metaData.getEISProductName()).isEqualTo("Microsoft Azure Service Bus");
        assertThat(metaData.getEISProductVersion()).isEqualTo("1.0.0");
        assertThat(metaData.getMaxConnections()).isEqualTo(0); // 0 indicates no limit
        assertThat(metaData.getUserName()).isEqualTo("Azure Service Bus Connection");
    }

    @Test
    public void testAzureServiceBusConnectionMetaData() throws Exception {
        // Given
        AzureServiceBusConnectionMetaData metaData = new AzureServiceBusConnectionMetaData();

        // When & Then
        assertThat(metaData.getJMSVersion()).isEqualTo("2.0");
        assertThat(metaData.getJMSMajorVersion()).isEqualTo(2);
        assertThat(metaData.getJMSMinorVersion()).isEqualTo(0);
        assertThat(metaData.getJMSProviderName()).isEqualTo("Microsoft Azure Service Bus JCA Adapter");
        assertThat(metaData.getProviderVersion()).isEqualTo("1.0.0");
        assertThat(metaData.getProviderMajorVersion()).isEqualTo(1);
        assertThat(metaData.getProviderMinorVersion()).isEqualTo(0);
    }

    @Test
    public void testAzureServiceBusConnectionMetaData_JMSXProperties() throws Exception {
        // Given
        AzureServiceBusConnectionMetaData metaData = new AzureServiceBusConnectionMetaData();

        // When
        Enumeration jmsxProperties = metaData.getJMSXPropertyNames();

        // Then
        assertThat(jmsxProperties).isNotNull();
        assertThat(jmsxProperties.hasMoreElements()).isFalse();
    }
}