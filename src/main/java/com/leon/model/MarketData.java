package com.leon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record MarketData(
    @JsonProperty("ric") String ric,
    @JsonProperty("price") Double price,
    @JsonProperty("timestamp") LocalDateTime timestamp
) {}
