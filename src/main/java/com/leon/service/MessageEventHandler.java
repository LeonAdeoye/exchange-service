package com.leon.service;

import com.leon.messaging.AmpsMessageOutboundProcessor;
import com.leon.model.MessageData;
import com.leon.model.MessageEvent;
import com.leon.model.OrderStateEvents;
import com.leon.model.OrderStates;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MessageEventHandler implements EventHandler<MessageEvent>
{
    private static final Logger log = LoggerFactory.getLogger(MessageEventHandler.class);
    @Autowired
    private OrderMatchingService orderMatchingService;
    @Autowired
    private AmpsMessageOutboundProcessor ampsMessageOutboundProcessor;


    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch)
    {
        try
        {
            processMessage(event.getMessageData());
        }
        catch(Exception e)
        {
            log.error("Error processing order event: {}", event, e);
        }
    }

    private void processMessage(MessageData messageData)
    {
        if(messageData.getState() == OrderStates.PENDING_EXCH)
        {
            transitionToNewState(messageData, OrderStateEvents.EXCH_APPROVE);
            ampsMessageOutboundProcessor.sendOrderToOMS(messageData);
            orderMatchingService.placeOrder(messageData);
            return;
        }
        if((messageData.getState() == OrderStates.FULLY_FILLED || messageData.getState() == OrderStates.PARTIALLY_FILLED) && messageData.getActionEvent() == OrderStateEvents.DESK_DONE)
        {
            transitionToNewState(messageData, OrderStateEvents.DESK_DONE);
            orderMatchingService.doneForDay(messageData);
        }
    }

    public void transitionToNewState(MessageData messageData, OrderStateEvents actionEvent)
    {
        OrderStates currentState = messageData.getState();
        Optional<OrderStates> nextStateOpt = OrderStateMachine.getNextState(currentState, actionEvent);

        if (nextStateOpt.isPresent())
        {
            OrderStates newState = nextStateOpt.get();
            messageData.setState(newState);
            log.info("Order {} transitioned from {} to {} due to event {}", messageData.getOrderId(), currentState, newState, actionEvent);
        }
        else
            log.warn("No valid transition for order {} from state {} with event {}", messageData.getOrderId(), currentState, actionEvent);
    }
} 