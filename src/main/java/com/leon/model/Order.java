package com.leon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(collection = "Orders")
public class Order
{
    private static final Logger log = LoggerFactory.getLogger(Order.class);
    @Id
    private String orderId;
    private String parentOrderId;
    @JsonProperty("isFirmAccount")
    private boolean isFirmAccount;
    @JsonProperty("isRiskAccount")
    private boolean isRiskAccount;
    private String instrumentCode;
    private String instrumentDescription;
    private String assetType;
    private String blgCode;
    private String ric;
    private String settlementCurrency;
    private String settlementType;
    private String exchangeAcronym;
    private Side side;
    private int quantity;
    private String priceType;
    private double price;
    private String tif;
    private String traderInstruction;
    private String qualifier;
    private String destination;
    private String accountMnemonic;
    private String accountName;
    private String legalEntity;
    private String customFlags;
    private String brokerAcronym;
    private String brokerDescription;
    private String handlingInstruction;
    private String algoType;
    private boolean facilConsent;
    private String facilConsentDetails;
    private String facilInstructions;
    private int lotSize;
    private String clientCode;
    private String clientDescription;
    private String ownerId;
    private OrderStates state;
    private LocalDate tradeDate;
    private LocalTime arrivalTime;
    private double arrivalPrice;
    private double averagePrice;
    private double adv20;
    private String executionTrigger;
    private int pending;
    private int executed;
    private double executedNotionalValueInUSD;
    private double orderNotionalValueInUSD;
    private double orderNotionalValueInLocal;
    private double residualNotionalValueInUSD;
    private double residualNotionalValueInLocal;
    private double ivwap;
    private double performanceVsArrival;
    private double performanceVsArrivalBPS;
    private double performanceVsIVWAP;
    private double performanceVsIVWAPBPS;
    private OrderStateEvents actionEvent;
    private double percentageOfParentOrder;

    public static boolean isParentOrder(Order order)
    {
        return order.getParentOrderId().equals(order.getOrderId());
    }

    public String toJSON()
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH);
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
            javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            mapper.registerModule(javaTimeModule);

            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e)
        {
            log.error("Failed to convert Order to JSON: {}", this, e);
            throw new RuntimeException("Failed to convert Order to JSON", e);
        }
    }
}