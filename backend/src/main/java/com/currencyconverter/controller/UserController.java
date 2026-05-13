package com.currencyconverter.controller;

import com.currencyconverter.entity.User;
import com.currencyconverter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "favoritePairs", user.getFavoritePairs()
        ));
    }

    @PostMapping("/favorites")
    public ResponseEntity<Void> addFavorite(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String pair = body.get("pair");
        if (pair == null || pair.isBlank()) return ResponseEntity.badRequest().build();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!user.getFavoritePairs().contains(pair)) {
            user.getFavoritePairs().add(pair);
            userRepository.save(user);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/favorites/{pair}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable String pair,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.getFavoritePairs().remove(pair);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }
}
