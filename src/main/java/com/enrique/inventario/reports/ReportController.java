package com.enrique.inventario.reports;

import com.enrique.inventario.reports.dto.SalesByDayEntry;
import com.enrique.inventario.reports.dto.SalesDailySummaryResponse;
import com.enrique.inventario.reports.dto.TopProductEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes agregados sobre ventas. Cualquier autenticado puede leerlos.
 * El input es siempre LocalDate (UI-friendly); el service traduce a UTC.
 */
@RestController
@RequestMapping("/v1/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/sales/daily")
    public SalesDailySummaryResponse daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.daily(date);
    }

    @GetMapping("/sales/range")
    public List<SalesByDayEntry> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.byDayRange(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductEntry> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return service.topProducts(from, to, limit);
    }
}
