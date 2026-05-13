package com.currencyconverter.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "historical_rates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"base_currency", "rate_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate date;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "historical_rate_values",
                     joinColumns = @JoinColumn(name = "historical_rate_id"))
    @MapKeyColumn(name = "currency_code")
    @Column(name = "rate", precision = 19, scale = 8)
    @Builder.Default
    private Map<String, java.math.BigDecimal> rates = new HashMap<>();
}
