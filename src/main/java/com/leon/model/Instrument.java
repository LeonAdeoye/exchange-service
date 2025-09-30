package com.leon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Instrument")
public record Instrument(@Id String id, String instrumentCode)
{
    public Instrument(String instrumentCode)
    {
        this(null, instrumentCode);
    }
}