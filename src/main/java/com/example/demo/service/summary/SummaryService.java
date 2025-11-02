package com.example.demo.service.summary;

import com.example.demo.dto.summary.SalesSummaryPremiumRequest;
import com.example.demo.dto.summary.SalesSummaryResponse;
import com.example.demo.dto.summary.WeeklySummaryRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.event.ReportRequestedEvent;
import com.example.demo.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ApplicationEventPublisher publisher;

    public SalesSummaryResponse requestWeeklySummary(WeeklySummaryRequest request, User requester) {
        validateBranchAccess(request.getBranch(), requester);
        LocalDate from = request.getFrom() != null ? request.getFrom() : LocalDate.now().minusDays(6);
        LocalDate to = request.getTo() != null ? request.getTo() : LocalDate.now();
        String requestId = "req_" + UUID.randomUUID();

        ReportRequestedEvent event = ReportRequestedEvent.builder()
                .requestId(requestId)
                .requesterUsername(requester.getUsername())
                .requesterEmail(requester.getEmail())
                .requesterRole(requester.getRole())
                .branch(request.getBranch())
                .from(from)
                .to(to)
                .emailTo(request.getEmailTo())
                .premium(false)
                .includeCharts(false)
                .attachPdf(false)
                .build();
        publisher.publishEvent(event);

        return SalesSummaryResponse.builder()
                .requestId(requestId)
                .status("PROCESSING")
                .message("Su solicitud de reporte está siendo procesada. Recibirá el resumen en " + request.getEmailTo() + " en unos momentos.")
                .estimatedTime("30-60 segundos")
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public SalesSummaryResponse requestPremiumSummary(SalesSummaryPremiumRequest request, User requester) {
        validateBranchAccess(request.getBranch(), requester);
        if (!"PREMIUM".equalsIgnoreCase(request.getFormat())) {
            throw new IllegalArgumentException("Formato de reporte no soportado");
        }
        LocalDate from = request.getFrom() != null ? request.getFrom() : LocalDate.now().minusDays(6);
        LocalDate to = request.getTo() != null ? request.getTo() : LocalDate.now();
        String requestId = "req_premium_" + UUID.randomUUID();

        ReportRequestedEvent event = ReportRequestedEvent.builder()
                .requestId(requestId)
                .requesterUsername(requester.getUsername())
                .requesterEmail(requester.getEmail())
                .requesterRole(requester.getRole())
                .branch(request.getBranch())
                .from(from)
                .to(to)
                .emailTo(request.getEmailTo())
                .premium(true)
                .includeCharts(request.isIncludeCharts())
                .attachPdf(request.isAttachPdf())
                .build();
        publisher.publishEvent(event);

        List<String> features = List.of("HTML_FORMAT");
        if (request.isIncludeCharts() || request.isAttachPdf()) {
            features = new java.util.ArrayList<>(features);
            if (request.isIncludeCharts()) {
                features.add("CHARTS");
            }
            if (request.isAttachPdf()) {
                features.add("PDF_ATTACHMENT");
            }
        }

        return SalesSummaryResponse.builder()
                .requestId(requestId)
                .status("PROCESSING")
                .message("Su reporte premium está siendo generado. Incluirá gráficos y PDF adjunto.")
                .estimatedTime("60-90 segundos")
                .requestedAt(LocalDateTime.now())
                .features(features)
                .build();
    }

    private void validateBranchAccess(String branch, User requester) {
        if (requester.getRole() == Role.BRANCH && !requester.getBranch().equalsIgnoreCase(branch)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo puede solicitar reportes de su sucursal");
        }
    }
}