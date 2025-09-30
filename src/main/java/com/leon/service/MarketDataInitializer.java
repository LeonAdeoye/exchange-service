package com.leon.service;

import com.leon.model.Instrument;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketDataInitializer
{
    private static final Logger logger = LoggerFactory.getLogger(MarketDataInitializer.class);
    @Autowired
    private MarketDataService marketDataService;
    @Autowired
    private InstrumentRepository instrumentRepository;

    @PostConstruct
    public void subscribeToAllInstruments()
    {
        try
        {
            logger.info("Starting market data subscription for all instruments...");
            List<Instrument> instruments = instrumentRepository.findAll();
            logger.info("Loaded {} instruments from MongoDB", instruments.size());
            
            if (instruments.isEmpty())
            {
                logger.warn("No instruments found in MongoDB. Initializing sample instruments...");
                initializeSampleInstruments();
                instruments = instrumentRepository.findAll();
                logger.info("After initialization: {} instruments loaded", instruments.size());
            }
            int successCount = 0;
            int failureCount = 0;

            for (Instrument instrument : instruments)
            {
                try
                {
                    marketDataService.subscribe(instrument.instrumentCode());
                    successCount++;
                    Thread.sleep(50);
                }
                catch (Exception e)
                {
                    logger.error("Failed to subscribe to instrument: {}", instrument.instrumentCode(), e);
                    failureCount++;
                }
            }
            logger.info("Market data subscription completed: {} successful, {} failed", successCount, failureCount);
        }
        catch (Exception e)
        {
            logger.error("Error during market data initialization", e);
        }
    }
    
    private void initializeSampleInstruments()
    {
        try
        {
            // Create some sample instruments
            Instrument[] sampleInstruments = {
                new Instrument("AAPL"),
                new Instrument("MSFT"),
                new Instrument("GOOGL"),
                new Instrument("TSLA"),
                new Instrument("AMZN")
            };
            
            for (Instrument instrument : sampleInstruments)
            {
                instrumentRepository.save(instrument);
            }
            
            logger.info("Initialized {} sample instruments", sampleInstruments.length);
        }
        catch (Exception e)
        {
            logger.error("Failed to initialize sample instruments", e);
        }
    }
}