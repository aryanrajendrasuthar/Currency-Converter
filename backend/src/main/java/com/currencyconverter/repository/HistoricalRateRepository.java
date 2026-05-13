package com.currencyconverter.repository;

import com.currencyconverter.entity.HistoricalRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HistoricalRateRepository extends JpaRepository<HistoricalRate, Long> {
    Optional<HistoricalRate> findByBaseCurrencyAndDate(String baseCurrency, LocalDate date);
    List<HistoricalRate> findByBaseCurrencyAndDateBetweenOrderByDateAsc(
            String baseCurrency, LocalDate from, LocalDate to);
}
