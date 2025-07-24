package com.leon.messaging;

import com.crankuptheamps.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.leon.model.MessageData;
import com.leon.service.ExchangeServiceImpl;
import com.leon.validation.MessageDataValidator;
import com.leon.validation.ValidationResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AmpsMessageInboundProcessor implements MessageHandler
{
    private static final Logger log = LoggerFactory.getLogger(AmpsMessageInboundProcessor.class);
    @Value("${amps.server.url}")
    private String ampsServerUrl;
    @Value("${amps.client.name}")
    private String ampsClientName;
    @Value("${amps.topic.inbound.exchange}")
    private String inboundExchangeTopic;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private final MessageDataValidator messageValidator;
    @Autowired
    private final ExchangeServiceImpl exchangeService;
    private Client ampsClient;
    private CommandId inboundExchangeTopicId;


    @PostConstruct
    public void initialize() throws Exception
    {
        try
        {
            ampsClient = new Client(ampsClientName);
            ampsClient.connect(ampsServerUrl);
            ampsClient.logon();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH);
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
            javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
            objectMapper.registerModule(javaTimeModule);
            inboundExchangeTopicId = ampsClient.executeAsync(new Command("subscribe").setTopic(inboundExchangeTopic), this);
        }
        catch (Exception e)
        {
            log.error("ERR-007: Failed to initialize AMPS client", e);
            throw e;
        }
    }

    @PreDestroy
    public void shutdown()
    {
        try
        {
            if (ampsClient != null)
            {
                ampsClient.unsubscribe(inboundExchangeTopicId);
                ampsClient.disconnect();
                log.info("Unsubscribed from AMPS topics and disconnected.");
            }
        }
        catch (Exception e)
        {
            log.error("ERR-010: Failed to unsubscribe or disconnect from AMPS", e);
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

            MessageData messageData = objectMapper.readValue(message.getData(), MessageData.class);
            log.info("Received valid message data: {}", messageData);
            exchangeService.processOrder(messageData);

        }
        catch (Exception e)
        {
            log.error("ERR-009: Failed to process message data", e);
        }
    }
} 