package com.leon.messaging;

import com.crankuptheamps.client.Client;
import com.leon.model.MessageType;
import com.leon.model.MessageData;
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

    public void sendExecutionToOMS(MessageData messageData, int tradedQuantity)
    {
        MessageData execution = new MessageData();
        try
        {
            execution.setMessageType(MessageType.EXECUTION_REPORT);
            execution.setVersion(1);
            execution.setParentOrderId(messageData.getOrderId());
            execution.setPrice(messageData.getPrice());
            execution.setInstrumentCode(messageData.getInstrumentCode());
            execution.setQuantity(tradedQuantity);
            execution.setExecuted(tradedQuantity);
            execution.setSide(messageData.getSide());
            execution.setOrderId(UUID.randomUUID().toString());
            execution.setExecutedTime(LocalTime.now());
            execution.setTradeDate(LocalDate.now());
            execution.setCurrentSource("EXCHANGE_SERVICE");
            execution.setTargetSource("ORDER_MANAGEMENT_SERVICE");
            ampsClient.publish(outboundExchangeTopic, execution.toJSON());
            log.info("Published execution message: {}", messageData);
        }
        catch (Exception e)
        {
            log.error("ERR-902: Failed to publish execution message for order: {}", messageData, e);
        }
    }

    public void sendOrderToOMS(MessageData messageData)
    {
        try
        {
            messageData.setCurrentSource("EXCHANGE_SERVICE");
            messageData.setTargetSource("ORDER_MANAGEMENT_SERVICE");
            ampsClient.publish(outboundExchangeTopic, messageData.toJSON());
            log.info("Published message data: {}", messageData);
        }
        catch (Exception e)
        {
            log.error("ERR-902: Failed to publish message data: {} due to exception: {}", messageData, e);
        }
    }
}
