package com.currencyconverter.service;

import com.currencyconverter.entity.HistoricalRate;
import com.currencyconverter.repository.HistoricalRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final WebClient.Builder webClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HistoricalRateRepository historicalRateRepository;

    @Value("${app.exchange-rate.api-key}")
    private String apiKey;

    @Value("${app.exchange-rate.base-url}")
    private String baseUrl;

    private static final long CACHE_TTL_HOURS = 1;

    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> getRates(String baseCurrency) {
        String cacheKey = "rates:" + baseCurrency.toUpperCase();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return convertToRateMap((Map<String, Object>) cached);
        }

        Map<String, BigDecimal> rates = fetchRatesFromApi(baseCurrency);
        redisTemplate.opsForValue().set(cacheKey, rates, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return rates;
    }

    public BigDecimal getRate(String from, String to) {
        Map<String, BigDecimal> rates = getRates(from.toUpperCase());
        BigDecimal rate = rates.get(to.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Currency not supported: " + to);
        }
        return rate;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> fetchRatesFromApi(String baseCurrency) {
        try {
            String url = baseUrl + "/" + apiKey + "/latest/" + baseCurrency.toUpperCase();
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (response != null && "success".equals(response.get("result"))) {
                Map<String, Object> conversionRates = (Map<String, Object>) response.get("conversion_rates");
                return convertToRateMap(conversionRates);
            }
        } catch (Exception e) {
            log.error("Failed to fetch rates for {}: {}", baseCurrency, e.getMessage());
        }
        return getFallbackRates();
    }

    private Map<String, BigDecimal> convertToRateMap(Map<String, Object> raw) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (raw == null) return result;
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            try {
                result.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public void fetchAndStoreHistoricalRates(String baseCurrency) {
        LocalDate today = LocalDate.now();
        if (historicalRateRepository.findByBaseCurrencyAndDate(baseCurrency, today).isPresent()) {
            return;
        }
        Map<String, BigDecimal> rates = getRates(baseCurrency);
        HistoricalRate historicalRate = HistoricalRate.builder()
                .baseCurrency(baseCurrency.toUpperCase())
                .date(today)
                .rates(rates)
                .build();
        historicalRateRepository.save(historicalRate);
        log.info("Stored historical rates for {} on {}", baseCurrency, today);
    }

    private Map<String, BigDecimal> getFallbackRates() {
        Map<String, BigDecimal> fallback = new HashMap<>();
        fallback.put("USD", BigDecimal.ONE);
        fallback.put("EUR", new BigDecimal("0.92"));
        fallback.put("GBP", new BigDecimal("0.79"));
        fallback.put("JPY", new BigDecimal("149.50"));
        fallback.put("INR", new BigDecimal("83.12"));
        fallback.put("CAD", new BigDecimal("1.36"));
        fallback.put("AUD", new BigDecimal("1.53"));
        fallback.put("CHF", new BigDecimal("0.90"));
        fallback.put("CNY", new BigDecimal("7.24"));
        fallback.put("MXN", new BigDecimal("17.15"));
        return fallback;
    }
}
