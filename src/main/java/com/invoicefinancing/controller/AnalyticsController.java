package com.invoicefinancing.controller;

import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.UserRepository;
import com.invoicefinancing.security.UserPrincipal;
import com.invoicefinancing.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/platform")
    public ResponseEntity<?> getPlatformAnalytics() {
        Map<String, Object> analytics = analyticsService.getPlatformAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserAnalytics(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> analytics = analyticsService.getUserAnalytics(user);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/trends")
    public ResponseEntity<?> getMonthlyTrends() {
        Map<String, Object> trends = analyticsService.getMonthlyTrends();
        return ResponseEntity.ok(trends);
    }
}