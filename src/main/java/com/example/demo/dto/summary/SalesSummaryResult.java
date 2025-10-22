package com.example.demo.dto.summary;


import lombok.*;

/**
 * Resultado final del resumen (lo que se envía por email o guarda en la BD).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResult {

    private int totalUnits;
    private double totalRevenue;
    private String topSku;
    private String topBranch;

    private String summaryText; // generado por el modelo LLM
}
