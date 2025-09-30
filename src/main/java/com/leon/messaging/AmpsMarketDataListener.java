package com.leon.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.model.MarketData;
import com.leon.service.MarketDataServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AmpsMarketDataListener
{
    private static final Logger logger = LoggerFactory.getLogger(AmpsMarketDataListener.class);
    
    @Autowired
    private MarketDataServiceImpl marketDataService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void onMarketDataMessage(MarketData marketData)
    {
        marketDataService.updatePricesFromMarketData(marketData);
    }

    public void onMarketDataMessage(String messageJson)
    {
        try
        {
            MarketData marketData = objectMapper.readValue(messageJson, MarketData.class);
            marketDataService.updatePricesFromMarketData(marketData);
        }
        catch (Exception e)
        {
            logger.error("Error processing market data JSON: {}", messageJson, e);
        }
    }
}
