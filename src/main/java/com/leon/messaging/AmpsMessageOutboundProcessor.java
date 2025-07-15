package com.leon.messaging;

import com.crankuptheamps.client.Client;
import com.leon.model.MessageType;
import com.leon.model.Order;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Component
public class AmpsMessageOutboundProcessor
{
    private static final Logger log = LoggerFactory.getLogger(AmpsMessageOutboundProcessor.class);
    private Client ampsClient;
    @Value("${amps.server.url}")
    private String ampsServerUrl;
    @Value("${amps.client.name}")
    private String ampsClientName;
    @Value("${amps.topic.outbound.exchange}")
    private String outboundExchangeTopic;

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

    public void sendExecutionToOMS(Order order)
    {
        Order execution = new Order();
        try
        {
            execution.setMessageType(MessageType.EXECUTION_REPORT);
            execution.setParentOrderId(order.getOrderId());
            execution.setPrice(order.getPrice());
            execution.setInstrumentCode(order.getInstrumentCode());
            execution.setQuantity(order.getExecuted());
            execution.setSide(order.getSide());
            execution.setOrderId(UUID.randomUUID().toString());
            execution.setExecutedTime(LocalTime.now());
            execution.setTradeDate(LocalDate.now());
            execution.setCurrentSource("EXCHANGE_SERVICE");
            execution.setTargetSource("ORDER_MANAGEMENT_SERVICE");
            ampsClient.publish(outboundExchangeTopic, execution.toJSON());
            log.info("Published execution message: {}", order);
        }
        catch (Exception e)
        {
            log.error("ERR-902: Failed to publish execution message for order: {}", order, e);
        }
    }

    public void sendOrderToOMS(Order order)
    {
        try
        {
            order.setCurrentSource("EXCHANGE_SERVICE");
            order.setTargetSource("ORDER_MANAGEMENT_SERVICE");
            ampsClient.publish(outboundExchangeTopic, order.toJSON());
            log.info("Published order message: {}", order);
        }
        catch (Exception e)
        {
            log.error("ERR-902: Failed to publish order message for order: {}", order, e);
        }
    }
}
