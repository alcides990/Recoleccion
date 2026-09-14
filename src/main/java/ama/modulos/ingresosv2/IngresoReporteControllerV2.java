package ama.modulos.ingresosv2;

import ama.dominio.UsuarioSistema;
import jakarta.servlet.http.HttpSession;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/reporte/ingresos-v2")
public class IngresoReporteControllerV2 {

    private final IngresoComprobanteRepositoryV2 comprobantes;
    private final HttpSession session;

    public IngresoReporteControllerV2(IngresoComprobanteRepositoryV2 comprobantes, HttpSession session) {
        this.comprobantes = comprobantes;
        this.session = session;
    }

    @GetMapping("/resumen")
    public String resumen() {
        usuarioSesion();
        return "reportes/resumen-ingresos";
    }

    @GetMapping("/comparativo")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
    public String comparativo() {
        usuarioSesion();
        return "reportes/comparativo-ingresos";
    }

    @GetMapping("/resumen/datos")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
    @ResponseBody
    public ResponseEntity<IngresoResumenResponseV2> resumenDatos(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        UsuarioSistema usuario = usuarioSesion();
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            return ResponseEntity.badRequest().build();
        }
        Integer sucursal = usuario.getSucursal().getCodigoSucursal();
        var porCobrador = comprobantes.resumenPorCobrador(sucursal, desde, hasta);
        var porMedio = comprobantes.resumenPorMedioPago(sucursal, desde, hasta);
        var detallePorCobrador = comprobantes.detallePorCobradorYMedio(sucursal, desde, hasta);
        double total = porCobrador.stream().map(IngresoResumenProjectionV2::getImporte)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        long cantidad = porCobrador.stream().map(IngresoResumenProjectionV2::getCantidad)
                .filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
        return ResponseEntity.ok(new IngresoResumenResponseV2(
                desde, hasta, total, cantidad, porCobrador, porMedio, detallePorCobrador));
    }

    @GetMapping("/resumen/usuario/datos")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<?> resumenUsuarioDatos(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        UsuarioSistema usuario = usuarioSesion();
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            return ResponseEntity.badRequest().build();
        }
        var filas = comprobantes.resumenPorUsuarioPuntoYCobrador(
                usuario.getSucursal().getCodigoSucursal(), usuario.getCodigoUsuarioSistema(), desde, hasta);
        double total = filas.stream().map(IngresoCobradorPuntoProjectionV2::getImporte)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        long cantidad = filas.stream().map(IngresoCobradorPuntoProjectionV2::getCantidad)
                .filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
        return ResponseEntity.ok(Map.of("desde", desde, "hasta", hasta, "total", total,
                "comprobantes", cantidad, "porPuntoYCobrador", filas));
    }

    @GetMapping("/datos")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
    @ResponseBody
    public ResponseEntity<IngresoGraficoResponseV2> datos(
            @RequestParam(defaultValue = "dia") String periodo,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        UsuarioSistema usuario = usuarioSesion();
        LocalDate hoy = LocalDate.now();
        Integer sucursal = usuario.getSucursal().getCodigoSucursal();
        Integer anioConsulta = anio == null ? hoy.getYear() : anio;
        Integer mesConsulta = mes == null ? hoy.getMonthValue() : mes;
        if (anioConsulta < 2000 || anioConsulta > hoy.getYear() + 1 || mesConsulta < 1 || mesConsulta > 12) {
            return ResponseEntity.badRequest().build();
        }

        List<IngresoPeriodoV2> ingresos;
        List<IngresoCobradorV2> cobradores;
        switch (periodo) {
            case "dia" -> {
                ingresos = comprobantes.ingresosPorDia(sucursal, anioConsulta, mesConsulta);
                cobradores = comprobantes.ingresosPorCobrador(sucursal, anioConsulta, mesConsulta);
            }
            case "mes" -> {
                ingresos = comprobantes.ingresosPorMes(sucursal, anioConsulta);
                cobradores = comprobantes.ingresosPorCobrador(sucursal, anioConsulta, null);
            }
            case "anio" -> {
                ingresos = comprobantes.ingresosPorAnio(sucursal);
                cobradores = comprobantes.ingresosPorCobradorHistorico(sucursal);
            }
            default -> {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }

        var mediosPago = comprobantes.ingresosPorMedioPago(sucursal,
                "anio".equals(periodo) ? null : anioConsulta,
                "dia".equals(periodo) ? mesConsulta : null, periodo);
        double total = ingresos.stream().mapToDouble(i -> i.getImporte() == null ? 0D : i.getImporte()).sum();
        double promedio = ingresos.isEmpty() ? 0D : total / ingresos.size();
        double maximo = ingresos.stream().mapToDouble(i -> i.getImporte() == null ? 0D : i.getImporte()).max().orElse(0D);
        return ResponseEntity.ok(new IngresoGraficoResponseV2(periodo, ingresos, cobradores, mediosPago, total, promedio, maximo));
    }

    @GetMapping("/comparativo/datos")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
    @ResponseBody
    public ResponseEntity<IngresoComparativoResponseV2> comparativoDatos(
            @RequestParam(defaultValue = "mes") String tipo,
            @RequestParam(defaultValue = "ZONA") String agrupacion,
            @RequestParam(required = false) Integer anioA,
            @RequestParam(required = false) Integer mesA,
            @RequestParam(required = false) Integer anioB,
            @RequestParam(required = false) Integer mesB,
            @RequestParam(required = false) LocalDate desdeA,
            @RequestParam(required = false) LocalDate hastaA,
            @RequestParam(required = false) LocalDate desdeB,
            @RequestParam(required = false) LocalDate hastaB) {
        UsuarioSistema usuario = usuarioSesion();
        String agrupacionNormalizada = agrupacion == null ? "ZONA" : agrupacion.trim().toUpperCase();
        if (!List.of("ZONA", "COBRADOR").contains(agrupacionNormalizada)) {
            return ResponseEntity.badRequest().build();
        }

        PeriodosComparacion periodos = resolverPeriodos(tipo, anioA, mesA, anioB, mesB, desdeA, hastaA, desdeB, hastaB);
        if (periodos == null) {
            return ResponseEntity.badRequest().build();
        }

        Integer sucursal = usuario.getSucursal().getCodigoSucursal();
        List<IngresoComparativoFilaV2> filas = new ArrayList<>();
        filas.addAll(comprobantes.comparativo(sucursal, "A", periodos.escala(),
                agrupacionNormalizada, periodos.desdeA(), periodos.hastaA()).stream()
                .map(this::filaComparativo).toList());
        filas.addAll(comprobantes.comparativo(sucursal, "B", periodos.escala(),
                agrupacionNormalizada, periodos.desdeB(), periodos.hastaB()).stream()
                .map(this::filaComparativo).toList());

        return ResponseEntity.ok(new IngresoComparativoResponseV2(periodos.escala(), agrupacionNormalizada,
                periodos.desdeA(), periodos.hastaA(), periodos.desdeB(), periodos.hastaB(), filas));
    }

    private IngresoComparativoFilaV2 filaComparativo(IngresoComparativoProjectionV2 fila) {
        return new IngresoComparativoFilaV2(fila.getSerie(), fila.getPeriodo(), fila.getNombre(),
                fila.getImporte(), fila.getCantidad());
    }

    private PeriodosComparacion resolverPeriodos(String tipo, Integer anioA, Integer mesA, Integer anioB,
            Integer mesB, LocalDate desdeA, LocalDate hastaA, LocalDate desdeB, LocalDate hastaB) {
        LocalDate hoy = LocalDate.now();
        String tipoNormalizado = tipo == null ? "mes" : tipo.trim().toLowerCase();
        try {
            return switch (tipoNormalizado) {
                case "mes" -> {
                    YearMonth primero = YearMonth.of(anioA == null ? hoy.getYear() : anioA,
                            mesA == null ? hoy.getMonthValue() : mesA);
                    YearMonth segundo = YearMonth.of(anioB == null ? hoy.minusMonths(1).getYear() : anioB,
                            mesB == null ? hoy.minusMonths(1).getMonthValue() : mesB);
                    yield new PeriodosComparacion("DIA", primero.atDay(1), primero.atEndOfMonth(),
                            segundo.atDay(1), segundo.atEndOfMonth());
                }
                case "anio" -> {
                    int primero = anioA == null ? hoy.getYear() : anioA;
                    int segundo = anioB == null ? hoy.minusYears(1).getYear() : anioB;
                    yield new PeriodosComparacion("MES", LocalDate.of(primero, 1, 1),
                            LocalDate.of(primero, 12, 31), LocalDate.of(segundo, 1, 1),
                            LocalDate.of(segundo, 12, 31));
                }
                case "rango" -> {
                    if (desdeA == null || hastaA == null || desdeB == null || hastaB == null
                            || hastaA.isBefore(desdeA) || hastaB.isBefore(desdeB)) {
                        yield null;
                    }
                    long diasA = ChronoUnit.DAYS.between(desdeA, hastaA);
                    long diasB = ChronoUnit.DAYS.between(desdeB, hastaB);
                    String escala = Math.max(diasA, diasB) > 62 ? "MES" : "DIA";
                    yield new PeriodosComparacion(escala, desdeA, hastaA, desdeB, hastaB);
                }
                default -> null;
            };
        } catch (DateTimeException ex) {
            return null;
        }
    }

    private record PeriodosComparacion(String escala, LocalDate desdeA, LocalDate hastaA,
            LocalDate desdeB, LocalDate hastaB) {
    }

    private UsuarioSistema usuarioSesion() {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return usuario;
    }
}
