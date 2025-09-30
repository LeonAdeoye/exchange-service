package com.leon.service;

import com.leon.model.SubscriptionRequest;
import com.leon.model.SubscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.Map;

@Component
public class MarketServiceClient
{
    private static final Logger logger = LoggerFactory.getLogger(MarketServiceClient.class);
    private final WebClient webClient;
    @Value("${market-service.base-url:http://localhost:20019}")
    private String baseUrl;
    public MarketServiceClient(WebClient.Builder webClientBuilder)
    {
        this.webClient = webClientBuilder.build();
    }
    
    public SubscriptionResponse subscribe(SubscriptionRequest request)
    {
        try
        {
            SubscriptionResponse response = webClient.post()
                    .uri(baseUrl + "/subscribe")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SubscriptionResponse.class)
                    .block();
            
            return response;
            
        }
        catch (WebClientResponseException e)
        {
            logger.error("Failed to subscribe to market data: {}", e.getMessage());
            return new SubscriptionResponse(false, "Failed to subscribe: " + e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("Unexpected error during subscription", e);
            return new SubscriptionResponse(false, "Unexpected error: " + e.getMessage());
        }
    }
    
    public Map<String, Object> unsubscribe(String ric)
    {
        try
        {
            logger.info("Unsubscribing from market data for RIC: {}", ric);
            Map<String, Object> response = webClient.delete()
                    .uri(baseUrl + "/unsubscribe/{ric}", ric)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            logger.info("Unsubscription response: {}", response);
            return response;
            
        }
        catch (WebClientResponseException e)
        {
            logger.error("Failed to unsubscribe from market data: {}", e.getMessage());
            return Map.of("success", false, "message", "Failed to unsubscribe: " + e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("Unexpected error during unsubscription", e);
            return Map.of("success", false, "message", "Unexpected error: " + e.getMessage());
        }
    }
}
