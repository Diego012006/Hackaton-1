package com.example.demo.service.sales;

import com.example.demo.dto.sales.SalesAggregates;
import com.example.demo.entity.Sale;
import com.example.demo.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesAggregationService {

    private final SalesRepository salesRepository;

    public SalesAggregates calculateAggregates(LocalDate from, LocalDate to, String branch) {
        LocalDateTime start = from != null ? from.atStartOfDay() : LocalDate.now().minusDays(6).atStartOfDay();
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDateTime end = effectiveTo.atTime(23, 59, 59, 999_000_000);

        List<Sale> sales = salesRepository.findByDateRange(start, end);
        if (branch != null) {
            sales = sales.stream()
                    .filter(sale -> branch.equalsIgnoreCase(sale.getBranch()))
                    .toList();
        }

        int totalUnits = sales.stream().mapToInt(Sale::getUnits).sum();
        double totalRevenue = sales.stream().mapToDouble(sale -> sale.getUnits() * sale.getPrice()).sum();

        String topSku = topValue(sales, Sale::getSku, Sale::getUnits);
        String topBranch = topValue(sales, Sale::getBranch, Sale::getUnits);

        return SalesAggregates.builder()
                .totalUnits(totalUnits)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .topSku(topSku)
                .topBranch(topBranch)
                .build();
    }

    private String topValue(List<Sale> sales,
                            java.util.function.Function<Sale, String> classifier,
                            java.util.function.ToIntFunction<Sale> unitsExtractor) {
        if (sales.isEmpty()) {
            return null;
        }
        Map<String, Integer> totals = sales.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.summingInt(unitsExtractor)));
        Optional<Map.Entry<String, Integer>> top = totals.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey));
        return top.map(Map.Entry::getKey).orElse(null);
    }
}