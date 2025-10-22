package com.example.demo.controller;
import com.example.demo.dto.sales.SaleRequest;
import com.example.demo.dto.sales.SaleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.http.*;
        import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(
            @Valid @RequestBody SaleRequest request,
            @AuthenticationPrincipal User user
    ) {
        SaleResponseDTO sale = salesService.createSale(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSaleById(
            @PathVariable String id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(salesService.getSaleById(id, user));
    }

    @GetMapping
    public ResponseEntity<List<SaleResponse>> listSales(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(salesService.listSales(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponse> updateSale(
            @PathVariable String id,
            @Valid @RequestBody SaleRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(salesService.updateSale(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(
            @PathVariable String id,
            @AuthenticationPrincipal User user
    ) {
        salesService.deleteSale(id, user);
        return ResponseEntity.noContent().build();
    }
}
