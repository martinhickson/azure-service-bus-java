// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jca;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AzureServiceBusResourceAdapter.
 */
public class AzureServiceBusResourceAdapterTest {

    private AzureServiceBusResourceAdapter resourceAdapter;

    @Mock
    private BootstrapContext mockBootstrapContext;

    @Mock
    private MessageEndpointFactory mockEndpointFactory;

    @Mock
    private ActivationSpec mockActivationSpec;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resourceAdapter = new AzureServiceBusResourceAdapter();
    }

    @Test
    public void testStart_Successful() throws Exception {
        // When
        resourceAdapter.start(mockBootstrapContext);

        // Then
        assertThat(resourceAdapter.isStarted()).isTrue();
        assertThat(resourceAdapter.getBootstrapContext()).isEqualTo(mockBootstrapContext);
    }

    @Test
    public void testStart_NullBootstrapContext() {
        // When & Then
        assertThatThrownBy(() -> resourceAdapter.start(null))
                .isInstanceOf(ResourceAdapterInternalException.class)
                .hasMessageContaining("BootstrapContext cannot be null");

        assertThat(resourceAdapter.isStarted()).isFalse();
        assertThat(resourceAdapter.getBootstrapContext()).isNull();
    }

    @Test
    public void testStop_AfterStart() throws Exception {
        // Given
        resourceAdapter.start(mockBootstrapContext);
        assertThat(resourceAdapter.isStarted()).isTrue();

        // When
        resourceAdapter.stop();

        // Then
        assertThat(resourceAdapter.isStarted()).isFalse();
        assertThat(resourceAdapter.getBootstrapContext()).isNull();
    }

    @Test
    public void testStop_WithoutStart() {
        // Given - not started
        assertThat(resourceAdapter.isStarted()).isFalse();

        // When
        resourceAdapter.stop();

        // Then - should not throw exception
        assertThat(resourceAdapter.isStarted()).isFalse();
    }

    @Test
    public void testEndpointActivation_NotSupported() throws Exception {
        // Given
        resourceAdapter.start(mockBootstrapContext);

        // When & Then
        assertThatThrownBy(() -> resourceAdapter.endpointActivation(mockEndpointFactory, mockActivationSpec))
                .isInstanceOf(NotSupportedException.class)
                .hasMessageContaining("Inbound messaging not supported");
    }

    @Test
    public void testEndpointDeactivation() throws Exception {
        // Given
        resourceAdapter.start(mockBootstrapContext);

        // When - should not throw exception
        resourceAdapter.endpointDeactivation(mockEndpointFactory, mockActivationSpec);

        // Then - verify no exceptions thrown
        assertThat(resourceAdapter.isStarted()).isTrue();
    }

    @Test
    public void testGetXAResources_ReturnsEmptyArray() throws Exception {
        // Given
        resourceAdapter.start(mockBootstrapContext);
        ActivationSpec[] specs = {mockActivationSpec};

        // When
        javax.transaction.xa.XAResource[] xaResources = resourceAdapter.getXAResources(specs);

        // Then
        assertThat(xaResources).isNotNull();
        assertThat(xaResources).hasSize(0);
    }

    @Test
    public void testGetXAResources_WithNullSpecs() throws Exception {
        // Given
        resourceAdapter.start(mockBootstrapContext);

        // When
        javax.transaction.xa.XAResource[] xaResources = resourceAdapter.getXAResources(null);

        // Then
        assertThat(xaResources).isNotNull();
        assertThat(xaResources).hasSize(0);
    }

    @Test
    public void testInitialState() {
        // Then
        assertThat(resourceAdapter.isStarted()).isFalse();
        assertThat(resourceAdapter.getBootstrapContext()).isNull();
    }

    @Test
    public void testStartStop_Multiple() throws Exception {
        // First start
        resourceAdapter.start(mockBootstrapContext);
        assertThat(resourceAdapter.isStarted()).isTrue();

        // Stop
        resourceAdapter.stop();
        assertThat(resourceAdapter.isStarted()).isFalse();

        // Second start
        resourceAdapter.start(mockBootstrapContext);
        assertThat(resourceAdapter.isStarted()).isTrue();

        // Final stop
        resourceAdapter.stop();
        assertThat(resourceAdapter.isStarted()).isFalse();
    }
}