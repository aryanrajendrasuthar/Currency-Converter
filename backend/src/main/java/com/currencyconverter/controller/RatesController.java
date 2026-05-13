package com.currencyconverter.controller;

import com.currencyconverter.dto.ConversionResponse;
import com.currencyconverter.dto.HistoricalDataPoint;
import com.currencyconverter.service.ConversionService;
import com.currencyconverter.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class RatesController {

    private final ExchangeRateService exchangeRateService;
    private final ConversionService conversionService;

    @GetMapping("/current")
    public ResponseEntity<Map<String, BigDecimal>> getCurrentRates(
            @RequestParam(defaultValue = "USD") String base) {
        return ResponseEntity.ok(exchangeRateService.getRates(base));
    }

    @GetMapping("/convert")
    public ResponseEntity<ConversionResponse> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(conversionService.convert(from, to, amount, email));
    }

    @GetMapping("/historical")
    public ResponseEntity<List<HistoricalDataPoint>> getHistorical(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "7d") String period) {
        int days = parsePeriod(period);
        return ResponseEntity.ok(conversionService.getHistoricalRates(from, to, days));
    }

    private int parsePeriod(String period) {
        return switch (period) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            case "1y" -> 365;
            default -> 7;
        };
    }
}
