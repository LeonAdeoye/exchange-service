package com.leon.service;

import com.leon.model.MessageData;

public interface OrderMatchingService
{
    void placeOrder(MessageData messageData);
    void doneForDay(MessageData messageData);
}
