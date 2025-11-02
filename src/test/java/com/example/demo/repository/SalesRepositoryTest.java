package com.example.demo.repository;

import com.example.demo.entity.Sale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SalesRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private SalesRepository salesRepository;

    private static LocalDateTime t(String iso) { return LocalDateTime.parse(iso); }

    private static Sale sale(
            String sku, int units, double price, String branch,
            String soldAtIso, String createdBy
    ) {
        return Sale.builder()
                .sku(sku)
                .units(units)
                .price(price)
                .branch(branch)
                .soldAt(t(soldAtIso))
                .createdBy(createdBy != null ? createdBy : "seed.user")
                .build();
    }

    @BeforeEach
    void seed() {
        // Dentro de la semana 2025-09-01 .. 2025-09-07 (incluye bordes)
        salesRepository.saveAll(List.of(
                sale("OREO_CLASSIC_12", 25, 1.99, "Miraflores", "2025-09-01T10:30:00", "mira.user"),
                sale("OREO_DOUBLE",     40, 2.49, "Miraflores", "2025-09-02T15:10:00", "mira.user"),
                sale("OREO_THINS",      32, 2.19, "San Isidro", "2025-09-03T11:05:00", "si.user"),
                sale("OREO_DOUBLE",     55, 2.49, "San Isidro", "2025-09-04T18:50:00", "si.user"),
                sale("OREO_CLASSIC_12", 20, 1.99, "Miraflores", "2025-09-05T09:40:00", "mira.user"),
                // Bordes exactos del rango
                sale("OREO_MINI", 10, 1.20, "Miraflores", "2025-09-01T00:00:00", "mira.user"),
                sale("OREO_GOLD",  5, 3.10, "San Isidro", "2025-09-07T23:59:59", "si.user")
        ));

        // Fuera del rango
        salesRepository.saveAll(List.of(
                sale("OREO_OUT_LOW",  9, 1.50, "Miraflores", "2025-08-31T23:59:59", "mira.user"),
                sale("OREO_OUT_HIGH", 7, 2.10, "San Isidro", "2025-09-08T00:00:00", "si.user")
        ));
    }

    // Helpers de agregación
    private static int totalUnits(List<Sale> sales) {
        return sales.stream().mapToInt(Sale::getUnits).sum();
    }

    private static double totalRevenue(List<Sale> sales) {
        return sales.stream().mapToDouble(s -> s.getUnits() * s.getPrice()).sum();
    }

    private static String topSkuByUnits(List<Sale> sales) {
        if (sales.isEmpty()) return null;
        Map<String, Integer> unitsBySku = sales.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingInt(Sale::getUnits)));
        int max = unitsBySku.values().stream().mapToInt(i -> i).max().orElse(0);
        return unitsBySku.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst().orElse(null);
    }

    private static String topBranchByUnits(List<Sale> sales) {
        if (sales.isEmpty()) return null;
        Map<String, Integer> unitsByBranch = sales.stream()
                .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingInt(Sale::getUnits)));
        int max = unitsByBranch.values().stream().mapToInt(i -> i).max().orElse(0);
        return unitsByBranch.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst().orElse(null);
    }

    // 1) Agregados con datos válidos
    @Test
    @DisplayName("Agregados correctos con dataset conocido en rango [2025-09-01 .. 2025-09-07]")
    void aggregates_withValidData() {
        LocalDateTime from = t("2025-09-01T00:00:00");
        LocalDateTime to   = t("2025-09-07T23:59:59");

        List<Sale> inRange = salesRepository.findByDateRange(from, to);

        assertThat(inRange).extracting(Sale::getSku)
                .contains("OREO_MINI", "OREO_GOLD")
                .doesNotContain("OREO_OUT_LOW", "OREO_OUT_HIGH");
        assertThat(inRange).hasSize(7);

        int units = totalUnits(inRange);              // 187
        double revenue = totalRevenue(inRange);       // 423.68
        String topSku = topSkuByUnits(inRange);       // OREO_DOUBLE (95)
        String topBranch = topBranchByUnits(inRange); // Miraflores (95) > San Isidro (92)

        assertThat(units).isEqualTo(187);
        assertThat(revenue).isCloseTo(423.68, offset(0.01));
        assertThat(topSku).isEqualTo("OREO_DOUBLE");
        assertThat(topBranch).isEqualTo("Miraflores");
    }

    // 2) Lista vacía
    @Test
    @DisplayName("Lista vacía: rango sin ventas devuelve totales 0 y tops nulos")
    void aggregates_emptyList() {
        LocalDateTime from = t("2025-08-01T00:00:00");
        LocalDateTime to   = t("2025-08-10T23:59:59");

        List<Sale> none = salesRepository.findByDateRange(from, to);
        assertThat(none).isEmpty();

        assertThat(totalUnits(none)).isZero();
        assertThat(totalRevenue(none)).isCloseTo(0.0, offset(0.001));
        assertThat(topSkuByUnits(none)).isNull();
        assertThat(topBranchByUnits(none)).isNull();
    }


    // 3) Filtrado por sucursal (adaptado: filtrado en memoria para evitar dependencia a findByBranch)
    @Test
    @DisplayName("Filtrado por sucursal: (en memoria) solo se consideran ventas de esa sucursal")
    void filter_byBranch_onlyThatBranch() {
        List<Sale> mira = salesRepository.findAll().stream()
                .filter(s -> "Miraflores".equalsIgnoreCase(s.getBranch()))
                .toList();

        assertThat(mira).isNotEmpty();
        assertThat(mira).allSatisfy(s -> assertThat(s.getBranch()).isEqualTo("Miraflores"));

        int units = totalUnits(mira);                    // 25+20+40+10+9 = 104
        double revenue = totalRevenue(mira);             // 25*1.99 + 20*1.99 + 40*2.49 + 10*1.20 + 9*1.50
        String topSku = topSkuByUnits(mira);             // -> OREO_CLASSIC_12 (45 unidades)
        String topBranch = topBranchByUnits(mira);       // -> Miraflores

        assertThat(units).isEqualTo(104);
        assertThat(revenue).isCloseTo(
                25*1.99 + 20*1.99 + 40*2.49 + 10*1.20 + 9*1.50,
                org.assertj.core.data.Offset.offset(0.01)
        );
        // FIX: el topSku en Miraflores es OREO_CLASSIC_12 (45), no OREO_DOUBLE (40).
        assertThat(topSku).isEqualTo("OREO_CLASSIC_12");
        assertThat(topBranch).isEqualTo("Miraflores");
    }


    // 4) Filtrado por fechas
    @Test
    @DisplayName("Filtrado por fechas: solo ventas dentro de [2025-09-02 .. 2025-09-05]")
    void filter_byDateRange_onlyInside() {
        LocalDateTime from = t("2025-09-02T00:00:00");
        LocalDateTime to   = t("2025-09-05T23:59:59");

        List<Sale> inRange = salesRepository.findByDateRange(from, to);

        assertThat(inRange).extracting(Sale::getSku)
                .contains("OREO_DOUBLE", "OREO_THINS", "OREO_CLASSIC_12")
                .doesNotContain("OREO_MINI", "OREO_GOLD", "OREO_OUT_LOW", "OREO_OUT_HIGH");

        assertThat(inRange).hasSize(4);

        int units = totalUnits(inRange);            // 147
        double revenue = totalRevenue(inRange);

        assertThat(units).isEqualTo(147);
        assertThat(revenue).isCloseTo(40*2.49 + 32*2.19 + 55*2.49 + 20*1.99, offset(0.01));
        assertThat(topSkuByUnits(inRange)).isEqualTo("OREO_DOUBLE"); // 95
        assertThat(topBranchByUnits(inRange)).isEqualTo("San Isidro"); // 87 > 60
    }

    // 5) Top SKU con empates
    @Test
    @DisplayName("Top SKU con empates por unidades: acepta cualquiera de los empatados")
    void topSku_ties_areHandled() {
        salesRepository.saveAll(List.of(
                sale("OREO_CLASSIC_12", 30, 1.99, "Miraflores", "2025-09-06T10:00:00", "mira.user"),
                sale("OREO_CLASSIC_12", 20, 1.99, "San Isidro", "2025-09-06T12:00:00", "si.user")
        ));

        LocalDateTime from = t("2025-09-01T00:00:00");
        LocalDateTime to   = t("2025-09-07T23:59:59");
        List<Sale> inRange = salesRepository.findByDateRange(from, to);

        var unitsBySku = inRange.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingInt(Sale::getUnits)));

        assertThat(unitsBySku.get("OREO_DOUBLE")).isEqualTo(95);
        assertThat(unitsBySku.get("OREO_CLASSIC_12")).isEqualTo(95);

        String topSku = topSkuByUnits(inRange);
        assertThat(topSku).isIn("OREO_DOUBLE", "OREO_CLASSIC_12");
    }
}
