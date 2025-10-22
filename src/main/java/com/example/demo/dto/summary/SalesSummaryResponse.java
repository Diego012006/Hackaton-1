package com.example.demo.dto.summary;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta inmediata cuando se acepta la solicitud del resumen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResponse{

    private String requestId;
    private String status; // "PROCESSING"
    private String message;
    private String estimatedTime;
    private LocalDateTime requestedAt;

    // Solo para premium
    private List<String> features;
}
