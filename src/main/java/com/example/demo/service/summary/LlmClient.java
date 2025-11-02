package com.example.demo.service.summary;

import com.example.demo.dto.sales.SalesAggregates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final WebClient githubModelsWebClient;

    @Value("${github.model.id:gpt-4o-mini}")
    private String modelId;

    @Value("${github.token:}")
    private String githubToken;

    public String generateSummary(SalesAggregates aggregates, String branch, LocalDate from, LocalDate to) {
        if (!StringUtils.hasText(githubToken) || githubModelsWebClient == null) {
            return fallbackSummary(aggregates, branch, from, to);
        }
        Map<String, Object> payload = Map.of(
                "model", modelId,
                "messages", List.of(
                        Map.of("role", "system", "content", "Eres un analista que escribe resúmenes breves y claros para emails corporativos."),
                        Map.of("role", "user", "content", buildPrompt(aggregates, branch, from, to))
                ),
                "max_tokens", 200
        );
        try {
            Map<String, Object> response = githubModelsWebClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorResume(err -> {
                        log.warn("Fallo al invocar GitHub Models: {}", err.getMessage());
                        return Mono.empty();
                    })
                    .block();
            if (response == null) {
                return fallbackSummary(aggregates, branch, from, to);
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.get("content") != null) {
                    return message.get("content").toString();
                }
            }
        } catch (Exception ex) {
            log.warn("Error procesando respuesta del modelo: {}", ex.getMessage());
        }
        return fallbackSummary(aggregates, branch, from, to);
    }

    private String buildPrompt(SalesAggregates aggregates, String branch, LocalDate from, LocalDate to) {
        return "Con estos datos: totalUnits=" + aggregates.getTotalUnits() +
                ", totalRevenue=" + String.format("%.2f", aggregates.getTotalRevenue()) +
                ", topSku=" + (aggregates.getTopSku() != null ? aggregates.getTopSku() : "N/A") +
                ", topBranch=" + (aggregates.getTopBranch() != null ? aggregates.getTopBranch() : branch) +
                ". Periodo: " + from + " a " + to + ". Devuelve un resumen ≤120 palabras para enviar por email en español.";
    }

    private String fallbackSummary(SalesAggregates aggregates, String branch, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resumen automático Oreo (fallback) del ")
                .append(from)
                .append(" al ")
                .append(to)
                .append(". ");
        if (aggregates.getTotalUnits() > 0) {
            sb.append("Se vendieron ")
                    .append(aggregates.getTotalUnits())
                    .append(" unidades, con ingresos de S/ ")
                    .append(String.format("%.2f", aggregates.getTotalRevenue()))
                    .append(". ");
        } else {
            sb.append("No se registraron ventas en el periodo. ");
        }
        if (aggregates.getTopSku() != null) {
            sb.append("SKU destacado: ")
                    .append(aggregates.getTopSku())
                    .append(". ");
        }
        if (aggregates.getTopBranch() != null) {
            sb.append("Sucursal líder: ")
                    .append(aggregates.getTopBranch())
                    .append('.');
        } else if (branch != null) {
            sb.append("Sucursal consultada: ")
                    .append(branch)
                    .append('.');
        }
        return sb.toString();
    }
}