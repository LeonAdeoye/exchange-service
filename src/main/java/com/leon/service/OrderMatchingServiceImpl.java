package com.leon.service;

import com.leon.messaging.AmpsMessageOutboundProcessor;
import com.leon.model.Order;
import com.leon.model.OrderStates;
import com.leon.model.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.PriorityQueue;

@Service
public class OrderMatchingServiceImpl implements OrderMatchingService
{
    private static final Logger logger = LoggerFactory.getLogger(OrderMatchingServiceImpl.class);
    @Autowired
    private AmpsMessageOutboundProcessor ampsMessageOutboundProcessor;

    Comparator<Order> buyOrderComparator = Comparator
            .comparingDouble(Order::getPrice).reversed()
            .thenComparing(Order::getArrivalTime);
    Comparator<Order> sellOrderComparator = Comparator
            .comparingDouble(Order::getPrice)
            .thenComparing(Order::getArrivalTime);

    private PriorityQueue<Order> buyOrders = new PriorityQueue<>(buyOrderComparator);

    private PriorityQueue<Order> sellOrders = new PriorityQueue<>(sellOrderComparator);

    public boolean placeOrder(Order order)
    {
        if (order.getSide() == Side.BUY)
            return matchOrder(order, sellOrders, buyOrders, true);
        else
            return matchOrder(order, buyOrders, sellOrders, false);
    }

    private boolean matchOrder(Order incoming, PriorityQueue<Order> oppositeQueue, PriorityQueue<Order> sameQueue, boolean isBuy)
    {
        boolean orderMatched = false;

        while(!oppositeQueue.isEmpty() && incoming.getPending() > 0)
        {
            Order match = oppositeQueue.peek();
            boolean canTrade = isBuy ? incoming.getPrice() >= match.getPrice() : incoming.getPrice() <= match.getPrice();
            if(canTrade)
            {
                int tradeQty = Math.min(incoming.getPending(), match.getPending());
                incoming.setPending(incoming.getPending() - tradeQty);
                match.setPending(match.getPending() - tradeQty);
                incoming.setExecuted(incoming.getExecuted() + tradeQty);
                match.setExecuted(match.getExecuted() + tradeQty);
                if (match.getPending() == 0 && match.getQuantity() == match.getExecuted())
                    oppositeQueue.poll();
                logger.info("Order matched: Incoming ID={} matched ID={}, executed: {}, pending: {}, original quantity: {}", incoming.getOrderId(), match.getOrderId(), incoming.getExecuted(), incoming.getPending(), incoming.getQuantity());
                orderMatched = true;
            }
            else
                break;
        }

        if(incoming.getPending() > 0)
            sameQueue.add(incoming);

        return orderMatched;
    }
}
