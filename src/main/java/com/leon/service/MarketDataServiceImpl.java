package com.leon.service;

import com.leon.model.MarketData;
import com.leon.model.SubscriptionRequest;
import com.leon.model.SubscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MarketDataServiceImpl implements MarketDataService
{
    private static final Logger logger = LoggerFactory.getLogger(MarketDataServiceImpl.class);
    private final Map<String, Double> bidPrices = new ConcurrentHashMap<>();
    private final Map<String, Double> askPrices = new ConcurrentHashMap<>();
    private final Map<String, Boolean> subscriptions = new ConcurrentHashMap<>();
    @Autowired
    private MarketServiceClient marketServiceClient;
    @Value("${market-data.default-spread:0.01}")
    private double defaultSpread;
    private final ExecutorService subscriptionExecutor = Executors.newCachedThreadPool();
    
    @Override
    public double getBidPrice(String instrumentCode)
    {
        return bidPrices.getOrDefault(instrumentCode, 0.0);
    }

    @Override
    public double getAskPrice(String instrumentCode)
    {
        return askPrices.getOrDefault(instrumentCode, 0.0);
    }

    @Override
    public void subscribe(String instrumentCode)
    {
        logger.info("Subscribing to market data for instrument: {}", instrumentCode);
        if (subscriptions.getOrDefault(instrumentCode, false))
        {
            logger.warn("Already subscribed to instrument: {}", instrumentCode);
            return;
        }

        SubscriptionRequest request = new SubscriptionRequest(Arrays.asList(instrumentCode));
        SubscriptionResponse response = marketServiceClient.subscribe(request);
        
        if (response.getSuccess())
        {
            subscriptions.put(instrumentCode, true);
            logger.info("Successfully subscribed to instrument: {}", instrumentCode);
            subscriptionExecutor.submit(() -> 
            {
                try
                {
                    logger.info("Started AMPS listening for instrument: {}", instrumentCode);
                }
                catch (Exception e)
                {
                    logger.error("Error in AMPS listening for instrument: {}", instrumentCode, e);
                    subscriptions.put(instrumentCode, false);
                }
            });
        }
        else
        {
            logger.error("Failed to subscribe to instrument: {}, reason: {}", 
                instrumentCode, response.getMessage());
        }
    }

    @Override
    public void unsubscribe(String instrumentCode)
    {
        logger.info("Unsubscribing from market data for instrument: {}", instrumentCode);
        if (!subscriptions.getOrDefault(instrumentCode, false))
        {
            logger.warn("Not subscribed to instrument: {}", instrumentCode);
            return;
        }

        Map<String, Object> response = marketServiceClient.unsubscribe(instrumentCode);
        Boolean success = (Boolean) response.get("success");
        if (success != null && success)
        {
            subscriptions.put(instrumentCode, false);
            bidPrices.remove(instrumentCode);
            askPrices.remove(instrumentCode);
            logger.info("Successfully unsubscribed from instrument: {}", instrumentCode);
        }
        else
        {
            logger.error("Failed to unsubscribe from instrument: {}, reason: {}", 
                instrumentCode, response.get("message"));
        }
    }

    public void updatePricesFromMarketData(MarketData marketData)
    {
        String ric = marketData.getRic();
        Double marketPrice = marketData.getPrice();
        
        if (marketPrice == null || marketPrice <= 0)
        {
            logger.warn("Invalid market price {} for instrument {}", marketPrice, ric);
            return;
        }

        Double spread = defaultSpread;
        Double bidPrice = marketPrice - (spread / 2);
        Double askPrice = marketPrice + (spread / 2);
        bidPrices.put(ric, bidPrice);
        askPrices.put(ric, askPrice);
        logger.info("Updated prices for {}: Market={}, Bid={}, Ask={}, Spread={}", ric, marketPrice, bidPrice, askPrice, spread);
    }
    
    public boolean isSubscribed(String instrumentCode)
    {
        return subscriptions.getOrDefault(instrumentCode, false);
    }
    public Map<String, Boolean> getSubscriptions()
    {
        return Map.copyOf(subscriptions);
    }
}
