package com.leon.service;

import com.leon.model.OrderEvent;
import com.lmax.disruptor.EventFactory;

public class OrderEventFactory implements EventFactory<OrderEvent>
{
    @Override
    public OrderEvent newInstance()
    {
        return new OrderEvent();
    }
} 