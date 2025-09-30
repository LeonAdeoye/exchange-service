package com.leon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class MarketData
{
    @JsonProperty("ric")
    private String ric;
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    public MarketData() {}
    
    public MarketData(String ric, Double price, LocalDateTime timestamp)
    {
        this.ric = ric;
        this.price = price;
        this.timestamp = timestamp;
    }
    
    public String getRic()
    {
        return ric;
    }
    
    public void setRic(String ric)
    {
        this.ric = ric;
    }
    
    public Double getPrice()
    {
        return price;
    }
    
    public void setPrice(Double price)
    {
        this.price = price;
    }
    
    public LocalDateTime getTimestamp()
    {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp)
    {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString()
    {
        return "MarketData{" +
                "ric='" + ric + '\'' +
                ", price=" + price +
                ", timestamp=" + timestamp +
                '}';
    }
}
