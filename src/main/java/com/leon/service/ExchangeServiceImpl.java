package com.leon.service;

import com.leon.model.MessageData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeServiceImpl
{
    private static final Logger logger = LoggerFactory.getLogger(ExchangeServiceImpl.class);
    private static int countOfOrders = 0;
    @Autowired
    private final MessageEventHandler messageEventHandler;
    @Autowired
    private DisruptorService disruptorService;
    @PostConstruct
    public void initialize() {
        disruptorService.start("ExchangeService", messageEventHandler);
    }
    @PreDestroy
    public void shutdown()
    {
        logger.info("Shutting down ExchangeService. Total orders processed: {}", countOfOrders);
        disruptorService.stop();
    }

    public void processOrder(MessageData messageData)
    {
        try
        {
            countOfOrders++;
            if(!isValidOrder(messageData)) {
                logger.error("Invalid order: {}", messageData);
                return;
            }
            disruptorService.push(messageData);
        }
        catch (Exception e)
        {
            logger.error("Error processing order: {}", messageData, e);
        }
    }

    private static boolean isValidOrder(MessageData messageData)
    {
        if (messageData.getQuantity() <= 0)
            return false;
        if (messageData.getPrice() < 0)
            return false;
        return true;
    }
}
