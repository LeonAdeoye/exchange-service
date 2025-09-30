package com.leon.service;

public interface MarketDataService
{
    double getBidPrice(String instrumentCode);
    double getAskPrice(String instrumentCode);
    void subscribe(String instrumentCode);
    void unsubscribe(String instrumentCode);
}
