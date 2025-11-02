package com.example.demo.service;

import com.example.demo.dto.sales.SaleRequest;
import com.example.demo.dto.sales.SaleResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.Sale;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.SalesRepository;
import com.example.demo.service.sales.SalesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @InjectMocks
    private SalesService salesService;

    private User centralUser;
    private User branchUser;

    @BeforeEach
    void setUp() {
        centralUser = new User();
        centralUser.setUsername("oreo.admin");
        centralUser.setRole(Role.CENTRAL);

        branchUser = new User();
        branchUser.setUsername("mira.user");
        branchUser.setRole(Role.BRANCH);
        branchUser.setBranch("Miraflores");
    }

    private Sale sale(String sku, int units, double price, String branch, LocalDateTime soldAt, String createdBy) {
        return Sale.builder()
                .id("id_" + sku)
                .sku(sku)
                .units(units)
                .price(price)
                .branch(branch)
                .soldAt(soldAt)
                .createdBy(createdBy)
                .build();
    }

    private SaleRequest request(String sku, int units, double price, String branch, LocalDateTime soldAt) {
        SaleRequest r = new SaleRequest();
        r.setSku(sku);
        r.setUnits(units);
        r.setPrice(price);
        r.setBranch(branch);
        r.setSoldAt(soldAt);
        return r;
    }

    // ------------------------------------------------------

    @Test
    @DisplayName("Test 1: CENTRAL puede crear venta para cualquier sucursal")
    void centralCanCreateForAnyBranch() {
        SaleRequest req = request("OREO_CLASSIC", 10, 1.99, "San Isidro", LocalDateTime.now());
        Sale mockSale = sale("OREO_CLASSIC", 10, 1.99, "San Isidro", LocalDateTime.now(), "oreo.admin");

        when(salesRepository.save(any(Sale.class))).thenReturn(mockSale);

        SaleResponse resp = salesService.create(req, centralUser);

        assertThat(resp.getSku()).isEqualTo("OREO_CLASSIC");
        assertThat(resp.getBranch()).isEqualTo("San Isidro");
        assertThat(resp.getUnits()).isEqualTo(10);
    }

    @Test
    @DisplayName("Test 2: BRANCH no puede crear venta para otra sucursal")
    void branchCannotCreateForOtherBranch() {
        SaleRequest req = request("OREO_CLASSIC", 10, 1.99, "San Isidro", LocalDateTime.now());
        assertThatThrownBy(() -> salesService.create(req, branchUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No puede registrar ventas para otra sucursal")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Test
    @DisplayName("Test 3: Filtrado por fechas: solo incluye ventas dentro del rango (repositorio ya filtra)")
    void listFiltersByDateRange() {
        // Datos
        LocalDate from = LocalDate.of(2025, 9, 1);
        LocalDate to   = LocalDate.of(2025, 9, 7);

        // Venta dentro del rango
        LocalDateTime inRange = LocalDateTime.of(2025, 9, 2, 12, 0);
        // Venta fuera del rango (NO debe venir desde el repo en este test)
        LocalDateTime outRange = LocalDateTime.of(2025, 8, 31, 23, 59);

        // Simulamos el contrato correcto: el repositorio YA devuelve solo lo del rango.
        List<Sale> repoFiltered = List.of(
                sale("OREO_CLASSIC", 10, 1.99, "Miraflores", inRange, "mira.user")
                // Intencionalmente NO incluimos la venta outRange aquí
        );

        when(salesRepository.findByDateRange(any(), any())).thenReturn(repoFiltered);

        Page<SaleResponse> result = salesService.list(
                from, to,
                null,
                PageRequest.of(0, 10),
                branchUser // BRANCH Miraflores
        );

        // Solo debe quedar la venta dentro del rango
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getContent().get(0).getBranch()).isEqualTo("Miraflores");
    }


    @Test
    @DisplayName("Test 4: Filtrado por sucursal (BRANCH ve solo su branch)")
    void branchSeesOnlyOwnBranch() {
        List<Sale> mockSales = List.of(
                sale("OREO_CLASSIC", 10, 1.99, "Miraflores", LocalDateTime.now(), "mira.user"),
                sale("OREO_DOUBLE", 8, 2.49, "San Isidro", LocalDateTime.now(), "si.user")
        );

        when(salesRepository.findByDateRange(any(), any())).thenReturn(mockSales);

        Page<SaleResponse> result = salesService.list(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                null,
                PageRequest.of(0, 10),
                branchUser
        );

        assertThat(result.getContent())
                .extracting(SaleResponse::getBranch)
                .containsOnly("Miraflores");
    }

    @Test
    @DisplayName("Test 5: Update mantiene restricciones por rol (BRANCH no puede cambiar sucursal)")
    void branchCannotChangeBranchOnUpdate() {
        Sale existing = sale("OREO_CLASSIC", 10, 1.99, "Miraflores", LocalDateTime.now(), "mira.user");
        when(salesRepository.findById("id1")).thenReturn(Optional.of(existing));

        SaleRequest req = request("OREO_DOUBLE", 15, 2.49, "San Isidro", LocalDateTime.now());

        assertThatThrownBy(() -> salesService.update("id1", req, branchUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No puede cambiar la sucursal de la venta")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }
}
