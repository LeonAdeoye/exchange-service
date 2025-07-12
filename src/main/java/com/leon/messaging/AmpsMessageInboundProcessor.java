package com.leon.messaging;

import com.crankuptheamps.client.Client;
import com.crankuptheamps.client.Message;
import com.crankuptheamps.client.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.model.Order;
import com.leon.service.ExchangeServiceImpl;
import com.leon.validation.OrderMessageValidator;
import com.leon.validation.ValidationResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AmpsMessageInboundProcessor implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(AmpsMessageInboundProcessor.class);
    @Value("${amps.server.url}")
    private String ampsServerUrl;
    @Value("${amps.client.name}")
    private String ampsClientName;
    @Value("${amps.topic.order.exchange.inbound}")
    private String exchangeInboundTopic;
    @Autowired
    private final ObjectMapper objectMapper;
    @Autowired
    private final OrderMessageValidator messageValidator;
    @Autowired
    private final ExchangeServiceImpl exchangeService;
    private Client ampsClient;


    @PostConstruct
    public void initialize() throws Exception
    {
        try
        {
            ampsClient = new Client(ampsClientName);
            ampsClient.connect(ampsServerUrl);
            ampsClient.logon();
            for(Message message : (ampsClient.subscribe(exchangeInboundTopic)))
            {
                invoke(message);
            }
        }
        catch (Exception e)
        {
            log.error("ERR-007: Failed to initialize AMPS client", e);
            throw e;
        }
    }

    @Override
    public void invoke(Message message)
    {
        try
        {
            ValidationResult validationResult = messageValidator.validateMessage(message.getData());

            if (!validationResult.valid())
            {
                log.error("ERR-008: Invalid message received: {}", validationResult.errorMessage());
                return;
            }

            Order order = objectMapper.readValue(message.getData(), Order.class);
            log.info("Received valid order message: {}", order);
            exchangeService.processOrder(order);

        }
        catch (Exception e)
        {
            log.error("ERR-009: Failed to process message", e);
        }
    }
} 