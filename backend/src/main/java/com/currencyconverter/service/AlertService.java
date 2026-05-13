package com.currencyconverter.service;

import com.currencyconverter.dto.AlertRequest;
import com.currencyconverter.dto.AlertResponse;
import com.currencyconverter.entity.Alert;
import com.currencyconverter.entity.User;
import com.currencyconverter.repository.AlertRepository;
import com.currencyconverter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertResponse createAlert(AlertRequest request, String userEmail) {
        User user = getUser(userEmail);
        Alert alert = Alert.builder()
                .user(user)
                .fromCurrency(request.getFromCurrency().toUpperCase())
                .toCurrency(request.getToCurrency().toUpperCase())
                .targetRate(request.getTargetRate())
                .direction(request.getDirection())
                .build();
        return AlertResponse.from(alertRepository.save(alert));
    }

    public List<AlertResponse> getUserAlerts(String userEmail) {
        User user = getUser(userEmail);
        return alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(AlertResponse::from).collect(Collectors.toList());
    }

    public void deleteAlert(Long alertId, String userEmail) {
        User user = getUser(userEmail);
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        if (!alert.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to delete this alert");
        }
        alertRepository.delete(alert);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
