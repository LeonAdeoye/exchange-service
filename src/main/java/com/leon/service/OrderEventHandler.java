package com.leon.service;

import com.leon.messaging.AmpsMessageOutboundProcessor;
import com.leon.model.Order;
import com.leon.model.OrderEvent;
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
public class OrderEventHandler implements EventHandler<OrderEvent>
{
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    @Autowired
    private OrderMatchingService orderMatchingService;
    @Autowired
    private AmpsMessageOutboundProcessor ampsMessageOutboundProcessor;


    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch)
    {
        try
        {
            processOrder(event.getOrder());
        }
        catch(Exception e)
        {
            log.error("Error processing order event: {}", event, e);
        }
    }

    private void processOrder(Order order)
    {
        transitionToNewState(order, OrderStateEvents.EXCH_APPROVE);
        ampsMessageOutboundProcessor.sendOrderToOMS(order);

        if(orderMatchingService.placeOrder(order))
            ampsMessageOutboundProcessor.sendOrderToOMS(order);
    }

    public void transitionToNewState(Order order, OrderStateEvents actionEvent)
    {
        OrderStates currentState = order.getState();
        Optional<OrderStates> nextStateOpt = OrderStateMachine.getNextState(currentState, actionEvent);

        if (nextStateOpt.isPresent())
        {
            OrderStates newState = nextStateOpt.get();
            order.setState(newState);
            log.info("Order {} transitioned from {} to {} due to event {}", order.getOrderId(), currentState, newState, actionEvent);
        }
        else
            log.warn("No valid transition for order {} from state {} with event {}", order.getOrderId(), currentState, actionEvent);
    }
} 