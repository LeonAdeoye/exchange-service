package com.leon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SubscriptionRequest
{
    @JsonProperty("rics")
    private List<String> rics;
    
    public SubscriptionRequest() {}
    
    public SubscriptionRequest(List<String> rics)
    {
        this.rics = rics;
    }
    
    public List<String> getRics()
    {
        return rics;
    }
    
    public void setRics(List<String> rics)
    {
        this.rics = rics;
    }
    
    @Override
    public String toString()
    {
        return "SubscriptionRequest{" +
                "rics=" + rics +
                '}';
    }
}
