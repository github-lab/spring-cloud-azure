/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.cloud.autoconfigure.eventhub;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.eventhub.EventHubAuthorizationKey;
import com.microsoft.azure.management.eventhub.EventHubNamespace;
import com.microsoft.azure.management.eventhub.EventHubNamespaceAuthorizationRule;
import com.microsoft.azure.spring.cloud.autoconfigure.context.AzureContextAutoConfiguration;
import com.microsoft.azure.spring.cloud.context.core.AzureAdmin;
import com.microsoft.rest.RestException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureEventHubAutoConfigurationTest {
    private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(AzureContextAutoConfiguration.class, AzureEventHubAutoConfiguration.class))
                                                                                   .withUserConfiguration(
                                                                                           TestConfiguration.class);

    @Test
    public void testWithoutAzureEventHubProperties() {
        this.contextRunner.run(context -> assertThat(context).doesNotHaveBean(AzureEventHubProperties.class));
    }

    @Ignore("org.apache.kafka.common.serialization.StringSerializer required on classpath")
    @Test
    public void testAzureEventHubPropertiesConfigured() {
        this.contextRunner.withPropertyValues("spring.cloud.azure.eventhub.namespace=ns1").run(context -> {
            assertThat(context).hasSingleBean(AzureEventHubProperties.class);
            assertThat(context.getBean(AzureEventHubProperties.class).getNamespace()).isEqualTo("ns1");
            assertThat(context).hasSingleBean(KafkaProperties.class);
            assertThat(context.getBean(KafkaProperties.class).getBootstrapServers().get(0)).isEqualTo("localhost:9093");
        });
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        AzureAdmin azureAdmin() {

            AzureAdmin azureAdmin = mock(AzureAdmin.class);
            EventHubNamespace namespace = mock(EventHubNamespace.class);
            EventHubAuthorizationKey key = mock(EventHubAuthorizationKey.class);
            when(key.primaryConnectionString()).thenReturn("connectionString1");
            EventHubNamespaceAuthorizationRule rule = mock(EventHubNamespaceAuthorizationRule.class);
            when(rule.getKeys()).thenReturn(key);
            PagedList<EventHubNamespaceAuthorizationRule> rules = new PagedList<EventHubNamespaceAuthorizationRule>() {
                @Override
                public Page<EventHubNamespaceAuthorizationRule> nextPage(String nextPageLink)
                        throws RestException, IOException {
                    return null;
                }
            };
            rules.add(rule);
            when(namespace.listAuthorizationRules()).thenReturn(rules);
            when(namespace.serviceBusEndpoint()).thenReturn("localhost");
            return azureAdmin;
        }

    }
}
