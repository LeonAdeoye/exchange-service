package com.leon.model;

public enum PriceTypeEnum {
    MARKET_ORDER("1"),
    LIMIT_ORDER("2");
    PriceTypeEnum(String priceType)
    {
        this.priceType = priceType;
    }
    private final String priceType;
}
