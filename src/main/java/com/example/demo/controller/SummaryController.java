package com.example.demo.controller;

import com.example.demo.dto.summary.SalesSummaryPremiumRequest;
import com.example.demo.dto.summary.SalesSummaryResponse;
import com.example.demo.dto.summary.WeeklySummaryRequest;
import com.example.demo.entity.User;
import com.example.demo.service.summary.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @PostMapping("/weekly")
    public ResponseEntity<SalesSummaryResponse> requestWeeklySummary(@Valid @RequestBody WeeklySummaryRequest request,
                                                                     Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        SalesSummaryResponse response = summaryService.requestWeeklySummary(request, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/weekly/premium")
    public ResponseEntity<SalesSummaryResponse> requestPremiumSummary(@Valid @RequestBody SalesSummaryPremiumRequest request,
                                                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        SalesSummaryResponse response = summaryService.requestPremiumSummary(request, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}