package com.example.demo.util;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class DateRange {
    private final LocalDateTime from;
    private final LocalDateTime to;

    private DateRange(LocalDateTime from, LocalDateTime to) {
        this.from = from;
        this.to = to;
    }

    public static DateRange of(LocalDate from, LocalDate to) {
        LocalDateTime start = (from != null) ? from.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = (to != null) ? to.atTime(23, 59, 59) : LocalDateTime.now();

        // Asegurar que el rango sea válido
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("La fecha 'from' no puede ser posterior a 'to'");
        }

        return new DateRange(start, end);
    }

    public static DateRange ofLastWeek() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(7);
        return new DateRange(start, end);
    }

    public static DateRange ofLastMonth() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30);
        return new DateRange(start, end);
    }
}