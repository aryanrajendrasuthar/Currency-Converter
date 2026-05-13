package com.currencyconverter.dto;

import com.currencyconverter.entity.Alert;
import com.currencyconverter.entity.Alert.Direction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AlertResponse {
    private Long id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal targetRate;
    private Direction direction;
    private boolean active;
    private boolean triggered;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;

    public static AlertResponse from(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .fromCurrency(alert.getFromCurrency())
                .toCurrency(alert.getToCurrency())
                .targetRate(alert.getTargetRate())
                .direction(alert.getDirection())
                .active(alert.isActive())
                .triggered(alert.isTriggered())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }
}
