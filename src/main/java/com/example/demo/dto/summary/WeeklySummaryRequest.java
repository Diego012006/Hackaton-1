package com.example.demo.dto.summary;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySummaryRequest {

    private LocalDate from;  // opcional: fecha inicial del rango
    private LocalDate to;    // opcional: fecha final del rango

    @NotBlank(message = "La sucursal (branch) es obligatoria.")
    private String branch;

    @NotBlank(message = "El email del destinatario es obligatorio.")
    @Email(message = "Formato de correo inválido.")
    private String emailTo;
}
