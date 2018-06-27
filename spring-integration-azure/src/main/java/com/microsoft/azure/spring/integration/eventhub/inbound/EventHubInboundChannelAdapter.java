/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.integration.eventhub.inbound;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.spring.integration.eventhub.EventHubHeaders;
import com.microsoft.azure.spring.integration.eventhub.EventHubOperation;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class EventHubInboundChannelAdapter extends MessageProducerSupport {
    private static final String DEFAULT_CONSUMER_GROUP = "$Default";
    private final String eventHubName;
    private final EventHubOperation eventHubOperation;
    private CheckpointMode checkpointMode = CheckpointMode.RECORD;
    private ListenerMode listenerMode = ListenerMode.RECORD;
    private MessageConverter messageConverter;
    private Map<String, Object> commonHeaders = new HashMap<>();
    private String consumerGroup = DEFAULT_CONSUMER_GROUP;

    public EventHubInboundChannelAdapter(String eventHubName, EventHubOperation eventHubOperation,
            String consumerGroup) {
        Assert.hasText(eventHubName, "eventHubName can't be null or empty");
        Assert.notNull(eventHubOperation, "EventHubOperation can't be null");
        this.eventHubName = eventHubName;
        this.eventHubOperation = eventHubOperation;
        if (StringUtils.hasText(consumerGroup)) {
            this.consumerGroup = consumerGroup;
        }
    }

    @Override
    protected void doStart() {
        super.doStart();

        this.eventHubOperation.subscribe(this.eventHubName, this::receiveMessage, this.consumerGroup);

        if (this.checkpointMode == CheckpointMode.MANUAL) {
            // Send the checkpointer downstream so user decides on when to checkpoint.
            this.commonHeaders.put(EventHubHeaders.CHECKPOINTER,
                    eventHubOperation.getCheckpointer(this.eventHubName, this.consumerGroup));
        }
    }

    public void receiveMessage(Iterable<EventData> events) {

        if (this.listenerMode == ListenerMode.BATCH) {
            sendMessage(toMessage(events));
        } else /* ListenerMode.RECORD */ {
            StreamSupport.stream(events.spliterator(), false).forEach((e) -> {
                sendMessage(toMessage(e.getBytes()));
                if (this.checkpointMode == CheckpointMode.RECORD) {
                    this.eventHubOperation.getCheckpointer(eventHubName, consumerGroup).checkpoint(e);
                }
            });
        }

        if (this.checkpointMode == CheckpointMode.BATCH) {
            this.eventHubOperation.getCheckpointer(eventHubName, consumerGroup).checkpoint();
        }

    }

    private Message<?> toMessage(Object payload) {
        if (this.messageConverter == null) {
            return MessageBuilder.withPayload(payload).copyHeaders(commonHeaders).build();
        }
        return this.messageConverter.toMessage(payload, new MessageHeaders(commonHeaders));
    }

    @Override
    protected void doStop() {
        this.eventHubOperation.unsubscribe(eventHubName, this::receiveMessage, this.consumerGroup);

        super.doStop();
    }

    public CheckpointMode getCheckpointMode() {
        return checkpointMode;
    }

    public void setCheckpointMode(CheckpointMode checkpointMode) {
        this.checkpointMode = checkpointMode;
    }

    public ListenerMode getListenerMode() {
        return listenerMode;
    }

    public void setListenerMode(ListenerMode listenerMode) {
        this.listenerMode = listenerMode;
    }

    public MessageConverter getMessageConverter() {
        return this.messageConverter;
    }

    /**
     * Sets the {@link MessageConverter} to convert the payload of the incoming message from Event hub.
     * If {@code messageConverter} is null, payload is {@code EventData} or
     * {@code Iterable<EventData>} and returned in that form.
     *
     * @param messageConverter converts the payload of the incoming message
     */
    public void setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }
}
