package com.example.demo.dto.sales;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponse {

    private String id;
    private String sku;
    private int units;
    private double price;
    private String branch;
    private LocalDateTime soldAt;
    private String createdBy;
}
