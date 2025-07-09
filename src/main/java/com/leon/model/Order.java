package com.leon.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(collection = "Orders")
public class Order
{
    public enum Side { BUY, SELL }
    private static final Logger log = LoggerFactory.getLogger(Order.class);

    private double price;
    private LocalDateTime arrivalTime;
    private int quantity;
    private Side side;
    public String toJSON()
    {

        try
        {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e)
        {
            log.error("Failed to convert Order to JSON: {}", this, e);
            throw new RuntimeException("Failed to convert Order to JSON", e);
        }
    }
}
