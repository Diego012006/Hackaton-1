package com.example.demo.event;

import com.example.demo.dto.sales.SalesAggregates;
import com.example.demo.service.mail.MailService;
import com.example.demo.service.sales.SalesAggregationService;
import com.example.demo.service.summary.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportRequestedListener {

    private final SalesAggregationService aggregationService;
    private final LlmClient llmClient;
    private final MailService mailService;

    @Async
    @EventListener
    public void handleReportRequest(ReportRequestedEvent event) {
        log.info("Procesando solicitud de resumen {} para sucursal {}", event.getRequestId(), event.getBranch());
        try {
            SalesAggregates aggregates = aggregationService.calculateAggregates(event.getFrom(), event.getTo(), event.getBranch());
            String summaryText = llmClient.generateSummary(aggregates, event.getBranch(), event.getFrom(), event.getTo());
            mailService.sendSummaryEmail(event, aggregates, summaryText);
        } catch (Exception ex) {
            log.error("Error generando el resumen {}", event.getRequestId(), ex);
            mailService.sendFailureNotification(event, ex.getMessage());
        }
    }
}