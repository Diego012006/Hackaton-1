package com.example.demo.dto.sales;


import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRequest{

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @Positive(message = "Las unidades deben ser mayores a 0")
    private int units;

    @Positive(message = "El precio debe ser mayor que 0")
    private double price;

    @NotBlank(message = "La sucursal (branch) es obligatoria")
    private String branch;

    @NotNull(message = "La fecha y hora de venta es obligatoria")
    private LocalDateTime soldAt;
}
