package com.leon.messaging;

import com.crankuptheamps.client.Client;
import com.leon.model.Order;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.PriorityQueue;

@Component
public class AmpsMessageOutboundProcessor
{
    private static final Logger log = LoggerFactory.getLogger(AmpsMessageOutboundProcessor.class);
    private Client ampsClient;
    @Value("${amps.server.url}")
    private String ampsServerUrl;
    @Value("${amps.client.name}")
    private String ampsClientName;
    @Value("${amps.topic.order.exchange.outbound}")
    private String exchangeOutboundTopic;

    @PostConstruct
    public void initialize() throws Exception
    {
        try
        {
            ampsClient = new Client(ampsClientName);
            ampsClient.connect(ampsServerUrl);
            ampsClient.logon();
        }
        catch (Exception e)
        {
            log.error("ERR-901: Failed to initialize AMPS client for AmpsOutboundProcessor", e);
            throw e;
        }
    }

    public void sendOrderToOMS(Order order)
    {
        try
        {
            ampsClient.publish(exchangeOutboundTopic, order.toJSON());
            log.info("Published order message: {}", order);
        }
        catch (Exception e)
        {
            log.error("ERR-902: Failed to publish order message: {}", order, e);
        }
    }

    public void sendOrderBookState(PriorityQueue<Order> buyOrders, PriorityQueue<Order> sellOrders){
        try
        {
            buyOrders.forEach(this::sendOrderToOMS);
            sellOrders.forEach(this::sendOrderToOMS);
        }
        catch (Exception e)
        {
            log.error("ERR-903: Failed to publish order book state", e);
        }
    }
}
