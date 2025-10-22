package com.example.demo.dto.sales;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesAggregates {
    private int totalUnits;
    private double totalRevenue;
    private String topSku;
    private String topBranch;
}
