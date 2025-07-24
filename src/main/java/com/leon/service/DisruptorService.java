package com.leon.service;

import com.leon.model.MessageData;
import com.leon.model.MessageEvent;
import com.lmax.disruptor.EventHandler;

public interface DisruptorService
{
    void start(String name, EventHandler<MessageEvent> actionEventHandler);
    void stop();
    void push(MessageData messageData);
} 