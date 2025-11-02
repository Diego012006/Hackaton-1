package com.example.demo.util;

import com.example.demo.dto.sales.SaleRequest;
import com.example.demo.dto.sales.SaleResponse;
import com.example.demo.entity.Sale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Mapper {

    public static Sale toSale(SaleRequest request) {
        return Sale.builder()
                .sku(request.getSku())
                .units(request.getUnits())
                .price(request.getPrice())
                .branch(request.getBranch())
                .soldAt(request.getSoldAt())
                .build();
    }

    public static SaleResponse toResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .sku(sale.getSku())
                .units(sale.getUnits())
                .price(sale.getPrice())
                .branch(sale.getBranch())
                .soldAt(sale.getSoldAt())
                .createdBy(sale.getCreatedBy())
                .build();
    }
}