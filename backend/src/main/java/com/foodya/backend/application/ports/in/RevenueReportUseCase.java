package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.RevenueReportView;

import java.time.LocalDate;
import java.util.UUID;

public interface RevenueReportUseCase {

    RevenueReportView platformRevenueReport(LocalDate from, LocalDate to);

    RevenueReportView merchantRevenueReport(UUID merchantUserId, LocalDate from, LocalDate to, Integer topItemsLimit);
}
