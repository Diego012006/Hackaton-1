package com.example.demo.service.sales;

import com.example.demo.dto.sales.SaleRequest;
import com.example.demo.dto.sales.SaleResponse;
import com.example.demo.entity.Sale;
import org.apache.catalina.User;

import com.example.demo.repository.SalesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;

import java.util.*;
        import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

    private final SalesRepository salesRepository;

    // Crear nueva venta
    @Transactional
    public SaleResponse createSale(SaleRequest request, User user) {
        if (user.getRole().name().equals("BRANCH") && !user.getBranch().equals(request.getBranch())) {
            throw new RuntimeException("403: No puedes registrar ventas de otra sucursal.");
        }

        Sale sale = Sale.builder()
                .sku(request.getSku())
                .units(request.getUnits())
                .price(request.getPrice())
                .branch(request.getBranch())
                .soldAt(request.getSoldAt())
                .createdBy(user.getUsername())
                .build();

        salesRepository.save(sale);

        return mapToDTO(sale);
    }

    // Obtener venta por ID (con validación de rol)
    public SaleResponse getSaleById(String id, User user) {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("404: Venta no encontrada"));

        if (user.getRole().name().equals("BRANCH") && !sale.getBranch().equals(user.getBranch())) {
            throw new RuntimeException("403: No tienes permiso para ver ventas de otra sucursal.");
        }

        return mapToDTO(sale);
    }

    // Listar ventas (filtradas según rol)
    public List<SaleResponse> listSales(User user) {
        List<Sale> sales;

        if (user.getRole().name().equals("CENTRAL")) {
            sales = salesRepository.findAll();
        } else {
            sales = salesRepository.findByBranch(user.getBranch());
        }

        return sales.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // Actualizar venta
    @Transactional
    public SaleResponse updateSale(String id, SaleRequest request, User user) {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("404: Venta no encontrada"));

        if (user.getRole().name().equals("BRANCH") && !sale.getBranch().equals(user.getBranch())) {
            throw new RuntimeException("403: No puedes modificar ventas de otra sucursal.");
        }

        sale.setSku(request.getSku());
        sale.setUnits(request.getUnits());
        sale.setPrice(request.getPrice());
        sale.setSoldAt(request.getSoldAt());
        sale.setBranch(request.getBranch());

        salesRepository.save(sale);
        return mapToDTO(sale);
    }

    // Eliminar venta (solo CENTRAL)
    @Transactional
    public void deleteSale(String id, User user) {
        if (!user.getRole().name().equals("CENTRAL")) {
            throw new RuntimeException("403: Solo usuarios CENTRAL pueden eliminar ventas.");
        }

        salesRepository.deleteById(id);
    }

    // Mapper Entity -> DTO
    private SaleResponse mapToDTO(Sale sale) {
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
