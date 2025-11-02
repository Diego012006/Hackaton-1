package com.example.demo.oreo.insightfactory.service;

import com.example.demo.dto.sales.SalesAggregates;
import com.example.demo.entity.Sale;
import com.example.demo.repository.SalesRepository;
import com.example.demo.service.sales.SalesAggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    // Test 1: Datos válidos
    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        // Given
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
                createSale("OREO_CLASSIC", 15, 1.99, "Miraflores")
        );
        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(
                fromDate(), toDate(), null
        );

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isEqualTo(54.25); // (10*1.99) + (5*2.49) + (15*1.99)
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 2: Lista vacía
    @Test
    void shouldHandleEmptySalesList() {
        // Given
        when(salesRepository.findByDateRange(any(), any())).thenReturn(List.of());

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(
                fromDate(), toDate(), null
        );

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isNull();
        assertThat(result.getTopBranch()).isNull();
    }

    // Test 3: Filtrado por sucursal
    @Test
    void shouldFilterSalesByBranch() {
        // Given
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
                createSale("OREO_CLASSIC", 15, 1.99, "Miraflores")
        );
        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(
                fromDate(), toDate(), "Miraflores"
        );

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(25); // Solo Miraflores: 10 + 15
        assertThat(result.getTotalRevenue()).isEqualTo(49.75); // (10*1.99) + (15*1.99)
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 4: Filtrado por fechas
    @Test
    void shouldConsiderOnlySalesWithinDateRange() {
        // Given
        LocalDate from = LocalDate.of(2025, 9, 1);
        LocalDate to = LocalDate.of(2025, 9, 5);

        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores",
                        LocalDateTime.of(2025, 9, 2, 10, 0)), // Dentro del rango
                createSale("OREO_DOUBLE", 5, 2.49, "Miraflores",
                        LocalDateTime.of(2025, 9, 6, 10, 0)) // Fuera del rango
        );

        // Simular que el repositorio solo retorna ventas dentro del rango
        List<Sale> filteredSales = mockSales.stream()
                .filter(sale -> !sale.getSoldAt().isAfter(to.atTime(23, 59, 59)))
                .toList();

        when(salesRepository.findByDateRange(any(), any())).thenReturn(filteredSales);

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(from, to, null);

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(10); // Solo la primera venta
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
    }

    // Test 5: SKU más vendido con empate
    @Test
    void shouldDetermineTopSkuWithTiebreak() {
        // Given
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 10, 2.49, "Miraflores"),
                createSale("OREO_THINS", 8, 2.19, "Miraflores")
        );
        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(
                fromDate(), toDate(), null
        );

        // Then - En empate, debería tomar el primero alfabéticamente o el primero encontrado
        assertThat(result.getTopSku()).isIn("OREO_CLASSIC", "OREO_DOUBLE");
        assertThat(result.getTotalUnits()).isEqualTo(28);
        assertThat(result.getTotalRevenue()).isEqualTo(65.42); // (10*1.99) + (10*2.49) + (8*2.19)
    }

    // Test adicional: Sucursal top cuando hay filtro por branch
    @Test
    void shouldSetBranchAsTopWhenFilteringByBranch() {
        // Given
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 20, 2.49, "San Isidro") // Esta tiene más unidades
        );
        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        // When
        SalesAggregates result = salesAggregationService.calculateAggregates(
                fromDate(), toDate(), "Miraflores"
        );

        // Then - Cuando se filtra por branch, esa branch debe ser la top
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
        assertThat(result.getTotalUnits()).isEqualTo(10);
    }

    // Métodos utilitarios
    private Sale createSale(String sku, int units, double price, String branch) {
        return createSale(sku, units, price, branch, LocalDateTime.now());
    }

    private Sale createSale(String sku, int units, double price, String branch, LocalDateTime soldAt) {
        return Sale.builder()
                .sku(sku)
                .units(units)
                .price(price)
                .branch(branch)
                .soldAt(soldAt)
                .createdBy("test-user")
                .build();
    }

    private LocalDate fromDate() {
        return LocalDate.now().minusDays(7);
    }

    private LocalDate toDate() {
        return LocalDate.now();
    }
}