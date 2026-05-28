package com.enrique.inventario.reports;

import com.enrique.inventario.reports.dto.SalesByDayEntry;
import com.enrique.inventario.reports.dto.SalesDailySummaryResponse;
import com.enrique.inventario.reports.dto.TopProductEntry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de reportes. Lectura pura — todas las queries con @Transactional(readOnly=true).
 *
 * Convención de fechas: los endpoints aceptan {@code LocalDate}; internamente se
 * convierte a [from = date 00:00 UTC, to = date+1 00:00 UTC). Esto es una
 * simplificación de portafolio; en prod con zona horaria del negocio (ej.
 * America/Lima), aplicar TZ tanto al input como al boundary del query.
 */
@Service
public class ReportService {

    private final ReportRepository repository;

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SalesDailySummaryResponse daily(LocalDate date) {
        OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to   = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        var p = repository.summaryForRange(from, to);
        BigDecimal ticket = p.getTicketPromedio().setScale(2, RoundingMode.HALF_UP);
        return new SalesDailySummaryResponse(date, p.getTotalVentas(), p.getRevenue(), ticket);
    }

    @Transactional(readOnly = true)
    public List<SalesByDayEntry> byDayRange(LocalDate from, LocalDate to) {
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs   = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return repository.salesByDay(fromTs, toTs).stream()
                .map(r -> new SalesByDayEntry(
                        r.getDia(),
                        r.getTotalVentas(),
                        r.getRevenue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopProductEntry> topProducts(LocalDate from, LocalDate to, int limit) {
        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs   = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return repository.topProducts(fromTs, toTs, limit).stream()
                .map(r -> new TopProductEntry(
                        r.getProductId(), r.getSku(), r.getNombre(),
                        r.getUnidadesVendidas(), r.getRevenue()))
                .toList();
    }
}
