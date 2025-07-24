package com.leon.service;

import com.leon.model.MessageData;
import com.leon.model.MessageEvent;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisruptorEventProducer
{
    private static final Logger logger = LoggerFactory.getLogger(DisruptorEventProducer.class);
    private final RingBuffer<MessageEvent> ringBuffer;
    public DisruptorEventProducer(RingBuffer<MessageEvent> ringBuffer)
    {
        this.ringBuffer = ringBuffer;
    }
    public void onData(MessageData messageData)
    {
        long sequence  = ringBuffer.next();
        try
        {
            MessageEvent event = ringBuffer.get(sequence);
            event.setMessageData(messageData);
        }
        finally
        {
            ringBuffer.publish(sequence);
        }

    }
}