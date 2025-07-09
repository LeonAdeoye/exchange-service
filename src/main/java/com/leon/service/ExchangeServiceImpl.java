package com.leon.service;

import com.leon.model.Order;
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
    private final OrderEventHandler orderEventHandler;
    @Autowired
    private DisruptorService disruptorService;
    @PostConstruct
    public void initialize() {
        disruptorService.start("ExchangeService", orderEventHandler);
    }
    @PreDestroy
    public void shutdown()
    {
        logger.info("Shutting down ExchangeService. Total orders processed: {}", countOfOrders);
        disruptorService.stop();
    }

    public void processOrder(Order order)
    {
        try
        {
            countOfOrders++;
            if(!isValidOrder(order)) {
                logger.error("Invalid order: {}", order);
                return;
            }
            disruptorService.push(order);
        }
        catch (Exception e)
        {
            logger.error("Error processing order: {}", order, e);
        }
    }

    private static boolean isValidOrder(Order order)
    {
        if (order.getQuantity() <= 0)
            return false;
        if (order.getPrice() <= 0)
            return false;
        return true;
    }
}
