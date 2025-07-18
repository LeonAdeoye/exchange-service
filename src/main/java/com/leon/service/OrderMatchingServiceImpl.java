package com.leon.service;

import com.leon.messaging.AmpsMessageOutboundProcessor;
import com.leon.model.Order;
import com.leon.model.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class OrderMatchingServiceImpl implements OrderMatchingService
{
    private static final Logger logger = LoggerFactory.getLogger(OrderMatchingServiceImpl.class);
    @Autowired
    private AmpsMessageOutboundProcessor ampsMessageOutboundProcessor;

    private Comparator<Order> buyOrderComparator = Comparator
            .comparingDouble(Order::getPrice).reversed()
            .thenComparing(Order::getArrivalTime);
    private Comparator<Order> sellOrderComparator = Comparator
            .comparingDouble(Order::getPrice)
            .thenComparing(Order::getArrivalTime);

    private Map<String, PriorityQueue<Order>> buyOrderBooks = new HashMap<>();
    private Map<String, PriorityQueue<Order>> sellOrderBooks = new HashMap<>();

    public void placeOrder(Order order)
    {
        String instrument = order.getInstrumentCode();
        buyOrderBooks.putIfAbsent(instrument, new PriorityQueue<>(buyOrderComparator));
        sellOrderBooks.putIfAbsent(instrument, new PriorityQueue<>(sellOrderComparator));

        if (order.getSide() == Side.BUY)
            matchOrder(order, sellOrderBooks.get(instrument), buyOrderBooks.get(instrument), true);
        else
            matchOrder(order, buyOrderBooks.get(instrument), sellOrderBooks.get(instrument), false);
    }

    public void doneForDay(Order order)
    {
        if( order.getSide() == Side.BUY)
            buyOrderBooks.values().forEach(queue -> queue.removeIf(o -> o.getOrderId().equals(order.getOrderId())));
        else
            sellOrderBooks.values().forEach(queue -> queue.removeIf(o -> o.getOrderId().equals(order.getOrderId())));
    }

    private void matchOrder(Order incomingOrder, PriorityQueue<Order> oppositeQueue,
                            PriorityQueue<Order> sameQueue, boolean isBuy)
    {
        while (!oppositeQueue.isEmpty() && incomingOrder.getPending() > 0)
        {
            Order matchingOrder = oppositeQueue.peek();

            boolean canTrade = isBuy ? incomingOrder.getPrice() >= matchingOrder.getPrice() : incomingOrder.getPrice() <= matchingOrder.getPrice();

            if (canTrade)
            {
                int tradeQuantity = Math.min(incomingOrder.getPending(), matchingOrder.getPending());

                incomingOrder.setPending(incomingOrder.getPending() - tradeQuantity);
                incomingOrder.setExecuted(incomingOrder.getExecuted() + tradeQuantity);
                matchingOrder.setPending(matchingOrder.getPending() - tradeQuantity);
                matchingOrder.setExecuted(matchingOrder.getExecuted() + tradeQuantity);

                ampsMessageOutboundProcessor.sendExecutionToOMS(incomingOrder, tradeQuantity);
                ampsMessageOutboundProcessor.sendExecutionToOMS(matchingOrder, tradeQuantity);

                logger.info("Order matched: Incoming ID={} matched ID={}, incoming order executed: {}, incoming order pending: {}, matching order pending: {}",
                    incomingOrder.getOrderId(), matchingOrder.getOrderId(), tradeQuantity, incomingOrder.getPending(), matchingOrder.getPending());

                if (Order.isFullyFilled(matchingOrder))
                    oppositeQueue.poll();
            }
            else
                break;
        }

        if (incomingOrder.getPending() > 0)
            sameQueue.add(incomingOrder);
    }
}
