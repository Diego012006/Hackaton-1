package com.example.demo.event;

import com.example.demo.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ReportRequestedEvent {

    private final String requestId;
    private final String requesterUsername;
    private final String requesterEmail;
    private final Role requesterRole;
    private final String branch;
    private final LocalDate from;
    private final LocalDate to;
    private final String emailTo;
    private final boolean premium;
    private final boolean includeCharts;
    private final boolean attachPdf;
}