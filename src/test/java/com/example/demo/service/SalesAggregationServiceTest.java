package com.example.demo.service;

import com.example.demo.dto.sales.SalesAggregates;
import com.example.demo.entity.Sale;
import com.example.demo.repository.SalesRepository;
import com.example.demo.service.sales.SalesAggregationService;
import org.junit.jupiter.api.DisplayName;
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

    // Helper para crear ventas simuladas
    private Sale createSale(String sku, int units, double price, String branch, LocalDateTime soldAt) {
        return Sale.builder()
                .id("id_" + sku + "_" + soldAt)
                .sku(sku)
                .units(units)
                .price(price)
                .branch(branch)
                .soldAt(soldAt)
                .createdBy("tester")
                .build();
    }

    // 1️⃣ Dataset válido
    @Test
    @DisplayName("Debe calcular correctamente totalUnits, totalRevenue, topSku y topBranch con datos válidos")
    void shouldCalculateCorrectAggregatesWithValidData() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores", LocalDateTime.now()),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro", LocalDateTime.now()),
                createSale("OREO_CLASSIC", 15, 1.99, "Miraflores", LocalDateTime.now())
        );

        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // totalUnits = 30, totalRevenue = 62.20
        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isEqualTo(62.20);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // 2️⃣ Lista vacía
    @Test
    @DisplayName("Debe retornar 0 y null cuando no hay ventas en el rango")
    void shouldHandleEmptySalesList() {
        when(salesRepository.findByDateRange(any(), any())).thenReturn(List.of());

        SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        assertThat(result.getTotalUnits()).isZero();
        assertThat(result.getTotalRevenue()).isZero();
        assertThat(result.getTopSku()).isNull();
        assertThat(result.getTopBranch()).isNull();
    }

    // 3️⃣ Filtrado por sucursal
    @Test
    @DisplayName("Debe considerar solo ventas de la sucursal especificada")
    void shouldFilterByBranch() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores", LocalDateTime.now()),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro", LocalDateTime.now()),
                createSale("OREO_MEGA", 8, 1.50, "Miraflores", LocalDateTime.now())
        );

        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), "Miraflores"
        );

        // Solo Miraflores: 18 unidades, 31.9 totalRevenue
        assertThat(result.getTotalUnits()).isEqualTo(18);
        assertThat(result.getTotalRevenue()).isEqualTo(31.9);
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
    }

    // 4️⃣ Filtrado por fechas
    @Test
    @DisplayName("Debe respetar las ventas dentro del rango provistas por el repositorio")
    void shouldRespectRepositoryDateRange() {
        LocalDate from = LocalDate.of(2025, 9, 1);
        LocalDate to = LocalDate.of(2025, 9, 7);

        List<Sale> repoFiltered = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores",
                        LocalDateTime.of(2025, 9, 3, 10, 0))
        );

        when(salesRepository.findByDateRange(any(), any())).thenReturn(repoFiltered);

        SalesAggregates result = salesAggregationService.calculateAggregates(from, to, null);

        assertThat(result.getTotalUnits()).isEqualTo(10);
        assertThat(result.getTotalRevenue()).isEqualTo(19.9);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // 5️⃣ Empate por unidades: gana el SKU alfabéticamente mayor
    @Test
    @DisplayName("En empate por unidades, debe devolver el SKU alfabéticamente mayor (por max())")
    void shouldResolveTopSkuTieAlphabeticallyDescending() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores", LocalDateTime.now()),
                createSale("OREO_DOUBLE", 10, 2.49, "Miraflores", LocalDateTime.now())
        );
        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // Por .max() con thenComparing(key ascendente) → gana "OREO_DOUBLE"
        assertThat(result.getTopSku()).isEqualTo("OREO_DOUBLE");
    }
}
