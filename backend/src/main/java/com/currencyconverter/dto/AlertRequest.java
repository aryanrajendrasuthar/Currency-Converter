package com.currencyconverter.dto;

import com.currencyconverter.entity.Alert.Direction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRequest {
    @NotBlank
    private String fromCurrency;

    @NotBlank
    private String toCurrency;

    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal targetRate;

    @NotNull
    private Direction direction;
}
