package com.example.demo.oreo.insightfactory.service;

import com.example.demo.entity.Sale;
import com.example.demo.service.sales.SalesAggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    // Test 1: Datos válidos
    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
                createSale("OREO_CLASSIC", 15, 1.99, "Miraflores")
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = salesAggregationService.calculateAggregates(fromDate(), toDate(), null);

        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isEqualTo(42.43);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 2: Lista vacía
    @Test
    void shouldHandleEmptySalesList() {
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(List.of());

        SalesAggregates result = salesAggregationService.calculateAggregates(fromDate(), toDate(), null);

        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isNull();
        assertThat(result.getTopBranch()).isNull();
    }

    // Test 3: Filtrado por sucursal
    @Test
    void shouldFilterSalesByBranch() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro")
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = salesAggregationService.calculateAggregates(fromDate(), toDate(), "Miraflores");

        assertThat(result.getTotalUnits()).isEqualTo(10);
        assertThat(result.getTotalRevenue()).isEqualTo(19.9);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 4: Filtrado por fechas
    @Test
    void shouldConsiderOnlySalesWithinDateRange() {
        LocalDateTime from = LocalDate.of(2025, 9, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(2025, 9, 5).atTime(23, 59);

        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores", LocalDateTime.of(2025, 9, 1, 10, 0)),
                createSale("OREO_DOUBLE", 5, 2.49, "Miraflores", LocalDateTime.of(2025, 9, 6, 10, 0)) // fuera de rango
        );
        when(saleRepository.findBySoldAtBetween(from, to)).thenReturn(
                mockSales.stream().filter(sale -> !sale.getSoldAt().isAfter(to)).toList()
        );

        SalesAggregates result = salesAggregationService.calculateAggregates(from.toLocalDate(), to.toLocalDate(), null);

        assertThat(result.getTotalUnits()).isEqualTo(10);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
    }

    // Test 5: SKU más vendido con empate
    @Test
    void shouldDetermineTopSkuWithTiebreak() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 10, 2.49, "Miraflores")
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = salesAggregationService.calculateAggregates(fromDate(), toDate(), null);

        // Asume que toma el primer SKU en caso de empate
        assertThat(result.getTopSku()).isIn("OREO_CLASSIC", "OREO_DOUBLE");
    }

    // Utils
    private Sale createSale(String sku, int units, double price, String branch) {
        return createSale(sku, units, price, branch, LocalDateTime.now());
    }

    private Sale createSale(String sku, int units, double price, String branch, LocalDateTime soldAt) {
        Sale sale = new Sale();
        sale.setSku(sku);
        sale.setUnits(units);
        sale.setPrice(price);
        sale.setBranch(branch);
        sale.setSoldAt(soldAt);
        return sale;
    }

    private LocalDate fromDate() {
        return LocalDate.now().minusDays(7);
    }

    private LocalDate toDate() {
        return LocalDate.now();
    }
}
