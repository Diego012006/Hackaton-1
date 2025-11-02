package com.example.demo.service.sales;

import com.example.demo.dto.sales.SaleRequest;
import com.example.demo.dto.sales.SaleResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.Sale;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.SalesRepository;
import com.example.demo.util.DateRange;
import com.example.demo.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesService {

    private final SalesRepository salesRepository;

    public SaleResponse create(SaleRequest request, User currentUser) {
        if (currentUser.getRole() == Role.BRANCH && !currentUser.getBranch().equalsIgnoreCase(request.getBranch())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No puede registrar ventas para otra sucursal");
        }
        Sale sale = Mapper.toSale(request);
        sale.setCreatedBy(currentUser.getUsername());
        Sale saved = salesRepository.save(sale);
        return Mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(String id, User currentUser) {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        validateAccessToSale(currentUser, sale);
        return Mapper.toResponse(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> list(LocalDate from, LocalDate to, String branchFilter, Pageable pageable, User currentUser) {
        DateRange range = DateRange.of(from, to);
        List<Sale> sales = salesRepository.findByDateRange(range.getFrom(), range.getTo());

        if (currentUser.getRole() == Role.BRANCH) {
            sales = sales.stream()
                    .filter(s -> s.getBranch().equalsIgnoreCase(currentUser.getBranch()))
                    .toList();
        } else if (branchFilter != null && !branchFilter.isBlank()) {
            sales = sales.stream()
                    .filter(s -> s.getBranch().equalsIgnoreCase(branchFilter))
                    .toList();
        }

        sales = sales.stream()
                .sorted(Comparator.comparing(Sale::getSoldAt).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sales.size());
        if (start > end) {
            start = end;
        }
        List<SaleResponse> content = sales.subList(start, end).stream()
                .map(Mapper::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, sales.size());
    }

    public SaleResponse update(String id, SaleRequest request, User currentUser) {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        validateAccessToSale(currentUser, sale);

        if (currentUser.getRole() == Role.BRANCH && !currentUser.getBranch().equalsIgnoreCase(request.getBranch())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No puede cambiar la sucursal de la venta");
        }
        if (currentUser.getRole() == Role.CENTRAL) {
            sale.setBranch(request.getBranch());
        }
        sale.setSku(request.getSku());
        sale.setUnits(request.getUnits());
        sale.setPrice(request.getPrice());
        sale.setSoldAt(request.getSoldAt());
        Sale saved = salesRepository.save(sale);
        return Mapper.toResponse(saved);
    }

    public void delete(String id, User currentUser) {
        if (currentUser.getRole() != Role.CENTRAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo la oficina central puede eliminar ventas");
        }
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        salesRepository.delete(sale);
    }

    private void validateAccessToSale(User currentUser, Sale sale) {
        if (currentUser.getRole() == Role.BRANCH && !sale.getBranch().equalsIgnoreCase(currentUser.getBranch())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No tiene permisos sobre esta venta");
        }
    }
}
