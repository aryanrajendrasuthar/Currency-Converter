package com.currencyconverter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class HistoricalDataPoint {
    private LocalDate date;
    private BigDecimal rate;
}
