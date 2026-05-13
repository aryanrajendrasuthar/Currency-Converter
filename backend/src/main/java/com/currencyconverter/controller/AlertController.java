package com.currencyconverter.controller;

import com.currencyconverter.dto.AlertRequest;
import com.currencyconverter.dto.AlertResponse;
import com.currencyconverter.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(
            @Valid @RequestBody AlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alertService.createAlert(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getUserAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alertService.getUserAlerts(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        alertService.deleteAlert(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
