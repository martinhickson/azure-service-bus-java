// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSException;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ServiceBusJmsConnectionMetaData.
 * 
 * This class tests the JMS provider metadata functionality that was previously untested.
 */
public class ServiceBusJmsConnectionMetaDataTest {

    private ServiceBusJmsConnectionMetaData metaData;

    @Before
    public void setUp() {
        metaData = new ServiceBusJmsConnectionMetaData();
    }

    // ========== JMS Version Tests ==========

    @Test
    public void testGetJMSVersion() throws Exception {
        // When
        String version = metaData.getJMSVersion();

        // Then
        assertThat(version).isEqualTo("2.0");
    }

    @Test
    public void testGetJMSMajorVersion() throws Exception {
        // When
        int majorVersion = metaData.getJMSMajorVersion();

        // Then
        assertThat(majorVersion).isEqualTo(2);
    }

    @Test
    public void testGetJMSMinorVersion() throws Exception {
        // When
        int minorVersion = metaData.getJMSMinorVersion();

        // Then
        assertThat(minorVersion).isEqualTo(0);
    }

    // ========== Provider Information Tests ==========

    @Test
    public void testGetJMSProviderName() throws Exception {
        // When
        String providerName = metaData.getJMSProviderName();

        // Then
        assertThat(providerName).isEqualTo("Microsoft Azure Service Bus JMS Implementation");
    }

    @Test
    public void testGetProviderVersion() throws Exception {
        // When
        String providerVersion = metaData.getProviderVersion();

        // Then
        assertThat(providerVersion).isEqualTo("1.0.0");
    }

    @Test
    public void testGetProviderMajorVersion() throws Exception {
        // When
        int majorVersion = metaData.getProviderMajorVersion();

        // Then
        assertThat(majorVersion).isEqualTo(1);
    }

    @Test
    public void testGetProviderMinorVersion() throws Exception {
        // When
        int minorVersion = metaData.getProviderMinorVersion();

        // Then
        assertThat(minorVersion).isEqualTo(0);
    }

    // ========== JMSX Properties Tests ==========

    @Test
    public void testGetJMSXPropertyNames() throws Exception {
        // When
        Enumeration<String> propertyNames = metaData.getJMSXPropertyNames();

        // Then
        assertThat(propertyNames).isNotNull();
        
        // Should contain JMSXDeliveryCount as it is supported
        assertThat(propertyNames.hasMoreElements()).isTrue();
        
        // Should return JMSXDeliveryCount
        String firstProperty = propertyNames.nextElement();
        assertThat(firstProperty).isEqualTo("JMSXDeliveryCount");
        
        // Should not have more elements after JMSXDeliveryCount
        assertThat(propertyNames.hasMoreElements()).isFalse();
    }

    @Test
    public void testGetJMSXPropertyNames_MultipleCallsConsistent() throws Exception {
        // When
        Enumeration<String> enum1 = metaData.getJMSXPropertyNames();
        Enumeration<String> enum2 = metaData.getJMSXPropertyNames();

        // Then - both should have JMSXDeliveryCount and be consistent
        assertThat(enum1.hasMoreElements()).isTrue();
        assertThat(enum2.hasMoreElements()).isTrue();
        
        assertThat(enum1.nextElement()).isEqualTo("JMSXDeliveryCount");
        assertThat(enum2.nextElement()).isEqualTo("JMSXDeliveryCount");
        
        assertThat(enum1.hasMoreElements()).isFalse();
        assertThat(enum2.hasMoreElements()).isFalse();
    }

    // ========== Consistency Tests ==========

    @Test
    public void testVersionConsistency() throws Exception {
        // When
        String fullVersion = metaData.getJMSVersion();
        int majorVersion = metaData.getJMSMajorVersion();
        int minorVersion = metaData.getJMSMinorVersion();

        // Then - full version should match major.minor
        String expectedFullVersion = majorVersion + "." + minorVersion;
        assertThat(fullVersion).isEqualTo(expectedFullVersion);
    }

    @Test
    public void testProviderVersionConsistency() throws Exception {
        // When
        String fullProviderVersion = metaData.getProviderVersion();
        int majorVersion = metaData.getProviderMajorVersion();
        int minorVersion = metaData.getProviderMinorVersion();

        // Then - provider version should start with major.minor
        String expectedPrefix = majorVersion + "." + minorVersion;
        assertThat(fullProviderVersion).startsWith(expectedPrefix);
    }

    // ========== Immutability Tests ==========

    @Test
    public void testMetaDataImmutable() throws Exception {
        // Given - get initial values
        String initialJMSVersion = metaData.getJMSVersion();
        String initialProviderName = metaData.getJMSProviderName();
        String initialProviderVersion = metaData.getProviderVersion();
        int initialJMSMajor = metaData.getJMSMajorVersion();
        int initialJMSMinor = metaData.getJMSMinorVersion();
        int initialProviderMajor = metaData.getProviderMajorVersion();
        int initialProviderMinor = metaData.getProviderMinorVersion();

        // When - call methods multiple times (metadata should be immutable)
        String laterJMSVersion = metaData.getJMSVersion();
        String laterProviderName = metaData.getJMSProviderName();
        String laterProviderVersion = metaData.getProviderVersion();
        int laterJMSMajor = metaData.getJMSMajorVersion();
        int laterJMSMinor = metaData.getJMSMinorVersion();
        int laterProviderMajor = metaData.getProviderMajorVersion();
        int laterProviderMinor = metaData.getProviderMinorVersion();

        // Then - all values should be identical
        assertThat(laterJMSVersion).isEqualTo(initialJMSVersion);
        assertThat(laterProviderName).isEqualTo(initialProviderName);
        assertThat(laterProviderVersion).isEqualTo(initialProviderVersion);
        assertThat(laterJMSMajor).isEqualTo(initialJMSMajor);
        assertThat(laterJMSMinor).isEqualTo(initialJMSMinor);
        assertThat(laterProviderMajor).isEqualTo(initialProviderMajor);
        assertThat(laterProviderMinor).isEqualTo(initialProviderMinor);
    }

    // ========== Thread Safety Tests ==========

    @Test
    public void testConcurrentAccess() throws Exception {
        // Given
        final int numThreads = 10;
        final Thread[] threads = new Thread[numThreads];
        final String[] results = new String[numThreads];
        final Throwable[] exceptions = new Throwable[numThreads];

        // When - access metadata concurrently from multiple threads
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    // Access various metadata properties
                    String jmsVersion = metaData.getJMSVersion();
                    String providerName = metaData.getJMSProviderName();
                    int majorVersion = metaData.getJMSMajorVersion();
                    Enumeration<String> props = metaData.getJMSXPropertyNames();
                    
                    // Store result
                    results[threadIndex] = jmsVersion + "|" + providerName + "|" + majorVersion + "|" + props.hasMoreElements();
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - no exceptions should occur and all results should be identical
        for (int i = 0; i < numThreads; i++) {
            assertThat(exceptions[i]).withFailMessage("Thread " + i + " threw exception").isNull();
            assertThat(results[i]).isNotNull();
        }

        // All results should be identical
        String expectedResult = results[0];
        for (int i = 1; i < numThreads; i++) {
            assertThat(results[i]).isEqualTo(expectedResult);
        }
    }

    // ========== Specification Compliance Tests ==========

    @Test
    public void testJMSVersionFormat() throws Exception {
        // When
        String version = metaData.getJMSVersion();

        // Then - should follow major.minor format
        assertThat(version).matches("\\d+\\.\\d+");
    }

    @Test
    public void testProviderVersionFormat() throws Exception {
        // When
        String version = metaData.getProviderVersion();

        // Then - should follow semantic versioning format
        assertThat(version).matches("\\d+\\.\\d+\\.\\d+");
    }

    @Test
    public void testVersionNumbers_NonNegative() throws Exception {
        // When & Then - all version numbers should be non-negative
        assertThat(metaData.getJMSMajorVersion()).isNotNegative();
        assertThat(metaData.getJMSMinorVersion()).isNotNegative();
        assertThat(metaData.getProviderMajorVersion()).isNotNegative();
        assertThat(metaData.getProviderMinorVersion()).isNotNegative();
    }

    @Test
    public void testProviderName_NotEmpty() throws Exception {
        // When
        String providerName = metaData.getJMSProviderName();

        // Then
        assertThat(providerName).isNotNull();
        assertThat(providerName).isNotEmpty();
        assertThat(providerName.trim()).isNotEmpty();
    }

    @Test
    public void testProviderName_ContainsAzure() throws Exception {
        // When
        String providerName = metaData.getJMSProviderName();

        // Then - should identify as Azure Service Bus provider
        assertThat(providerName.toLowerCase()).contains("azure");
        assertThat(providerName.toLowerCase()).contains("service bus");
    }

    // ========== Multiple Instance Tests ==========

    @Test
    public void testMultipleInstances_SameValues() throws Exception {
        // Given
        ServiceBusJmsConnectionMetaData metaData2 = new ServiceBusJmsConnectionMetaData();

        // When & Then - both instances should return identical values
        assertThat(metaData2.getJMSVersion()).isEqualTo(metaData.getJMSVersion());
        assertThat(metaData2.getJMSProviderName()).isEqualTo(metaData.getJMSProviderName());
        assertThat(metaData2.getProviderVersion()).isEqualTo(metaData.getProviderVersion());
        assertThat(metaData2.getJMSMajorVersion()).isEqualTo(metaData.getJMSMajorVersion());
        assertThat(metaData2.getJMSMinorVersion()).isEqualTo(metaData.getJMSMinorVersion());
        assertThat(metaData2.getProviderMajorVersion()).isEqualTo(metaData.getProviderMajorVersion());
        assertThat(metaData2.getProviderMinorVersion()).isEqualTo(metaData.getProviderMinorVersion());
    }

    // ========== Error Conditions Tests ==========

    @Test
    public void testNoExceptionsThrown() throws Exception {
        // When & Then - all methods should execute without throwing exceptions
        assertThatNoException().isThrownBy(() -> {
            metaData.getJMSVersion();
            metaData.getJMSMajorVersion();
            metaData.getJMSMinorVersion();
            metaData.getJMSProviderName();
            metaData.getProviderVersion();
            metaData.getProviderMajorVersion();
            metaData.getProviderMinorVersion();
            metaData.getJMSXPropertyNames();
        });
    }

    // ========== JMS 2.0 Compliance Tests ==========

    @Test
    public void testJMS20Compliance() throws Exception {
        // When
        int majorVersion = metaData.getJMSMajorVersion();
        int minorVersion = metaData.getJMSMinorVersion();

        // Then - should indicate JMS 2.0 compliance
        assertThat(majorVersion).isEqualTo(2);
        assertThat(minorVersion).isEqualTo(0);
    }

    @Test
    public void testJMSXPropertiesNotSupported() throws Exception {
        // When
        Enumeration<String> properties = metaData.getJMSXPropertyNames();

        // Then - Azure Service Bus supports JMSXDeliveryCount
        assertThat(properties.hasMoreElements()).isTrue();
        assertThat(properties.nextElement()).isEqualTo("JMSXDeliveryCount");
        assertThat(properties.hasMoreElements()).isFalse();
        
        // Verify enumeration behavior
        int count = 0;
        while (properties.hasMoreElements()) {
            properties.nextElement();
            count++;
        }
        assertThat(count).isZero();
    }
}