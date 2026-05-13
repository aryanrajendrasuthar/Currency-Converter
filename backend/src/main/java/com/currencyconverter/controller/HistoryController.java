package com.currencyconverter.controller;

import com.currencyconverter.entity.ConversionHistory;
import com.currencyconverter.service.ConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final ConversionService conversionService;

    @GetMapping
    public ResponseEntity<Page<ConversionHistory>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                conversionService.getHistory(userDetails.getUsername(), PageRequest.of(page, size)));
    }
}
