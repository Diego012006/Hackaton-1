package com.example.demo.dto.summary;


import lombok.*;


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
