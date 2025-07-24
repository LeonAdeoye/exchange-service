package com.leon.service;

import com.leon.model.MessageEvent;
import com.lmax.disruptor.EventFactory;

public class MessageEventFactory implements EventFactory<MessageEvent>
{
    @Override
    public MessageEvent newInstance()
    {
        return new MessageEvent();
    }
} 