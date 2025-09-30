package com.leon.service;

import com.leon.messaging.AmpsMessageOutboundProcessor;
import com.leon.model.MessageData;
import com.leon.model.PriceTypeEnum;
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
    @Autowired
    private MarketDataService marketDataService;

    private Comparator<MessageData> buyOrderComparator = Comparator
            .comparingDouble(MessageData::getPrice).reversed()
            .thenComparing(MessageData::getArrivalTime);
    private Comparator<MessageData> sellOrderComparator = Comparator
            .comparingDouble(MessageData::getPrice)
            .thenComparing(MessageData::getArrivalTime);

    private Map<String, PriorityQueue<MessageData>> buyOrderBooks = new HashMap<>();
    private Map<String, PriorityQueue<MessageData>> sellOrderBooks = new HashMap<>();

    public void placeOrder(MessageData messageData)
    {
        String instrument = messageData.getInstrumentCode();
        buyOrderBooks.putIfAbsent(instrument, new PriorityQueue<>(buyOrderComparator));
        sellOrderBooks.putIfAbsent(instrument, new PriorityQueue<>(sellOrderComparator));

        if (messageData.getSide() == Side.BUY)
            matchOrder(messageData, sellOrderBooks.get(instrument), buyOrderBooks.get(instrument), true);
        else
            matchOrder(messageData, buyOrderBooks.get(instrument), sellOrderBooks.get(instrument), false);
    }

    public void doneForDay(MessageData messageData)
    {
        String instrumentCode = messageData.getInstrumentCode();
        if( messageData.getSide() == Side.BUY)
            buyOrderBooks.get(instrumentCode).removeIf(o -> o.getOrderId().equals(messageData.getOrderId()));
        else
            sellOrderBooks.get(instrumentCode).removeIf(o -> o.getOrderId().equals(messageData.getOrderId()));
    }

    private double handleMarketOrderPricing(MessageData incomingOrder, boolean isBuy)
    {
        String instrumentCode = incomingOrder.getInstrumentCode();
        return isBuy ? marketDataService.getAskPrice(instrumentCode) : marketDataService.getBidPrice(instrumentCode);
    }

    private void matchOrder(MessageData incomingMessageData, PriorityQueue<MessageData> oppositeQueue, PriorityQueue<MessageData> sameQueue, boolean isBuy)
    {
        while (!oppositeQueue.isEmpty() && incomingMessageData.getPending() > 0)
        {
            MessageData matchingMessageData = oppositeQueue.peek();

            if(incomingMessageData.getPriceType().equals(PriceTypeEnum.MARKET_ORDER) && !matchingMessageData.getPriceType().equals(PriceTypeEnum.MARKET_ORDER))
                incomingMessageData.setPrice(matchingMessageData.getPrice());
            else if(incomingMessageData.getPriceType().equals(PriceTypeEnum.MARKET_ORDER) && matchingMessageData.getPriceType().equals(PriceTypeEnum.MARKET_ORDER))
                incomingMessageData.setPrice(handleMarketOrderPricing(incomingMessageData, isBuy));

            double incomingPrice = incomingMessageData.getPrice();
            double matchingPrice = matchingMessageData.getPrice();

            if (matchingMessageData.getPriceType().equals(PriceTypeEnum.MARKET_ORDER) && matchingMessageData.getPrice() == 0.0)
                matchingPrice = incomingPrice;

            boolean canTrade = isBuy ? incomingPrice >= matchingPrice : incomingPrice <= matchingPrice;

            if (canTrade)
            {
                int tradeQuantity = Math.min(incomingMessageData.getPending(), matchingMessageData.getPending());

                incomingMessageData.setPending(incomingMessageData.getPending() - tradeQuantity);
                incomingMessageData.setExecuted(incomingMessageData.getExecuted() + tradeQuantity);
                matchingMessageData.setPending(matchingMessageData.getPending() - tradeQuantity);
                matchingMessageData.setExecuted(matchingMessageData.getExecuted() + tradeQuantity);

                ampsMessageOutboundProcessor.sendExecutionToOMS(incomingMessageData, tradeQuantity);
                ampsMessageOutboundProcessor.sendExecutionToOMS(matchingMessageData, tradeQuantity);

                logger.info("Order matched: Incoming ID={} matched ID={}, incoming order executed: {}, incoming order pending: {}, matching order pending: {}",
                    incomingMessageData.getOrderId(), matchingMessageData.getOrderId(), tradeQuantity, incomingMessageData.getPending(), matchingMessageData.getPending());

                if (MessageData.isFullyFilled(matchingMessageData))
                    oppositeQueue.poll();
            }
            else
                break;
        }

        if (incomingMessageData.getPending() > 0)
            sameQueue.add(incomingMessageData);
    }
}
