package com.leon.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubscriptionResponse
{
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    public SubscriptionResponse() {}
    
    public SubscriptionResponse(Boolean success, String message)
    {
        this.success = success;
        this.message = message;
    }
    
    public Boolean getSuccess()
    {
        return success;
    }
    
    public void setSuccess(Boolean success)
    {
        this.success = success;
    }
    
    public String getMessage()
    {
        return message;
    }
    
    public void setMessage(String message)
    {
        this.message = message;
    }
    
    @Override
    public String toString()
    {
        return "SubscriptionResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
