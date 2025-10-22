package com.example.demo.dto.summary;


import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO para el endpoint premium (HTML, gráficos, PDF).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryPremiumRequest {

    private LocalDate from;
    private LocalDate to;

    @NotBlank(message = "La sucursal es obligatoria")
    private String branch;

    @NotBlank(message = "El email destinatario es obligatorio")
    @Email(message = "Formato de email inválido")
    private String emailTo;

    @NotBlank(message = "Debe especificar el formato del reporte (PREMIUM o BASIC)")
    private String format;

    private boolean includeCharts;
    private boolean attachPdf;
}
