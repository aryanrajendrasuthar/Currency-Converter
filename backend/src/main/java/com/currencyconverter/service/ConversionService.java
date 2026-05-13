package com.currencyconverter.service;

import com.currencyconverter.dto.ConversionResponse;
import com.currencyconverter.dto.HistoricalDataPoint;
import com.currencyconverter.entity.ConversionHistory;
import com.currencyconverter.entity.HistoricalRate;
import com.currencyconverter.entity.User;
import com.currencyconverter.repository.ConversionHistoryRepository;
import com.currencyconverter.repository.HistoricalRateRepository;
import com.currencyconverter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ExchangeRateService exchangeRateService;
    private final ConversionHistoryRepository historyRepository;
    private final HistoricalRateRepository historicalRateRepository;
    private final UserRepository userRepository;

    public ConversionResponse convert(String from, String to, BigDecimal amount, String userEmail) {
        BigDecimal rate = exchangeRateService.getRate(from, to);
        BigDecimal converted = amount.multiply(rate).setScale(6, RoundingMode.HALF_UP);

        ConversionResponse response = ConversionResponse.builder()
                .fromCurrency(from.toUpperCase())
                .toCurrency(to.toUpperCase())
                .amount(amount)
                .convertedAmount(converted)
                .rate(rate)
                .build();

        if (userEmail != null) {
            userRepository.findByEmail(userEmail).ifPresent(user -> {
                ConversionHistory history = ConversionHistory.builder()
                        .user(user)
                        .fromCurrency(from.toUpperCase())
                        .toCurrency(to.toUpperCase())
                        .amount(amount)
                        .convertedAmount(converted)
                        .rate(rate)
                        .build();
                historyRepository.save(history);
            });
        }
        return response;
    }

    public Page<ConversionHistory> getHistory(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return historyRepository.findByUserIdOrderByTimestampDesc(user.getId(), pageable);
    }

    public List<HistoricalDataPoint> getHistoricalRates(String from, String to, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<HistoricalRate> rates = historicalRateRepository
                .findByBaseCurrencyAndDateBetweenOrderByDateAsc(from.toUpperCase(), startDate, endDate);

        return rates.stream()
                .filter(r -> r.getRates().containsKey(to.toUpperCase()))
                .map(r -> new HistoricalDataPoint(r.getDate(), r.getRates().get(to.toUpperCase())))
                .collect(Collectors.toList());
    }
}
