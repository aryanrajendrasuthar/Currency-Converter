package com.currencyconverter.scheduler;

import com.currencyconverter.entity.Alert;
import com.currencyconverter.repository.AlertRepository;
import com.currencyconverter.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateScheduler {

    private final ExchangeRateService exchangeRateService;
    private final AlertRepository alertRepository;
    private final JavaMailSender mailSender;

    private static final List<String> MAJOR_CURRENCIES =
            List.of("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CHF", "CNY");

    // Daily at midnight: store historical rates
    @Scheduled(cron = "0 0 0 * * *")
    public void storeHistoricalRates() {
        log.info("Storing daily historical rates...");
        for (String currency : MAJOR_CURRENCIES) {
            try {
                exchangeRateService.fetchAndStoreHistoricalRates(currency);
            } catch (Exception e) {
                log.error("Failed to store rates for {}: {}", currency, e.getMessage());
            }
        }
    }

    // Every 15 minutes: check alerts
    @Scheduled(fixedDelay = 900_000)
    public void checkAlerts() {
        List<Alert> activeAlerts = alertRepository.findByActiveTrue();
        if (activeAlerts.isEmpty()) return;

        log.debug("Checking {} active alerts", activeAlerts.size());

        for (Alert alert : activeAlerts) {
            try {
                BigDecimal currentRate = exchangeRateService.getRate(
                        alert.getFromCurrency(), alert.getToCurrency());

                boolean triggered = switch (alert.getDirection()) {
                    case ABOVE -> currentRate.compareTo(alert.getTargetRate()) >= 0;
                    case BELOW -> currentRate.compareTo(alert.getTargetRate()) <= 0;
                };

                if (triggered) {
                    alert.setTriggered(true);
                    alert.setActive(false);
                    alert.setTriggeredAt(LocalDateTime.now());
                    alertRepository.save(alert);
                    sendAlertEmail(alert, currentRate);
                    log.info("Alert {} triggered for user {}", alert.getId(), alert.getUser().getEmail());
                }
            } catch (Exception e) {
                log.error("Error checking alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    private void sendAlertEmail(Alert alert, BigDecimal currentRate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(alert.getUser().getEmail());
            message.setSubject("Rate Alert Triggered: " + alert.getFromCurrency() + "/" + alert.getToCurrency());
            message.setText(String.format(
                    "Hi %s,\n\nYour rate alert has been triggered!\n\n" +
                    "Pair: %s/%s\n" +
                    "Target: %s (%s)\n" +
                    "Current Rate: %s\n\n" +
                    "Visit the app to view your conversions.",
                    alert.getUser().getName(),
                    alert.getFromCurrency(), alert.getToCurrency(),
                    alert.getTargetRate(), alert.getDirection(),
                    currentRate));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Could not send alert email: {}", e.getMessage());
        }
    }
}
