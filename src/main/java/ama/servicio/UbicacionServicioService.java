package ama.servicio;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UbicacionServicioService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public DetalleUbicacion consultar(String cuentaCorriente) {
        List<UbicacionActual> ubicaciones = jdbcTemplate.query("""
                SELECT u.latitud, u.longitud, u.precision_metros,
                       u.metodo, u.origen, us.usuario,
                       u.fecha_registro, u.fecha_actualizacion
                FROM ubicaciones_servicio u
                JOIN usuarios_sistema us
                  ON us.codigo_usuario_sistema = u.codigo_usuario_sistema
                WHERE u.cuenta_corriente = ?
                """, (rs, fila) -> new UbicacionActual(
                    rs.getBigDecimal("latitud"),
                    rs.getBigDecimal("longitud"),
                    rs.getBigDecimal("precision_metros"),
                    rs.getString("metodo"),
                    rs.getString("origen"),
                    rs.getString("usuario"),
                    aLocalDateTime(rs.getTimestamp("fecha_registro")),
                    aLocalDateTime(rs.getTimestamp("fecha_actualizacion"))),
                cuentaCorriente);

        List<CambioUbicacion> historial = jdbcTemplate.query("""
                SELECT h.codigo_historial, h.latitud_anterior, h.longitud_anterior,
                       h.latitud_nueva, h.longitud_nueva,
                       h.precision_anterior_metros, h.precision_nueva_metros,
                       h.metodo, h.origen, us.usuario, h.fecha_modificacion
                FROM historial_ubicaciones_servicio h
                JOIN usuarios_sistema us
                  ON us.codigo_usuario_sistema = h.codigo_usuario_sistema
                WHERE h.cuenta_corriente = ?
                ORDER BY h.fecha_modificacion DESC, h.codigo_historial DESC
                """, (rs, fila) -> new CambioUbicacion(
                    rs.getLong("codigo_historial"),
                    rs.getBigDecimal("latitud_anterior"),
                    rs.getBigDecimal("longitud_anterior"),
                    rs.getBigDecimal("latitud_nueva"),
                    rs.getBigDecimal("longitud_nueva"),
                    rs.getBigDecimal("precision_anterior_metros"),
                    rs.getBigDecimal("precision_nueva_metros"),
                    rs.getString("metodo"),
                    rs.getString("origen"),
                    rs.getString("usuario"),
                    aLocalDateTime(rs.getTimestamp("fecha_modificacion"))),
                cuentaCorriente);

        return new DetalleUbicacion(
                ubicaciones.isEmpty() ? null : ubicaciones.get(0), historial);
    }

    @Transactional
    public DetalleUbicacion guardar(String cuentaCorriente, BigDecimal latitud,
            BigDecimal longitud, BigDecimal precisionMetros, String metodo,
            Integer codigoUsuarioSistema) {
        return guardar(cuentaCorriente, latitud, longitud, precisionMetros,
                metodo, codigoUsuarioSistema, "WEB");
    }

    @Transactional
    public DetalleUbicacion guardar(String cuentaCorriente, BigDecimal latitud,
            BigDecimal longitud, BigDecimal precisionMetros, String metodo,
            Integer codigoUsuarioSistema, String origen) {
        String metodoNormalizado = validar(latitud, longitud, precisionMetros, metodo);
        String origenNormalizado = normalizarOrigen(origen);

        // Bloquea la cuenta para serializar dos actualizaciones simultáneas.
        jdbcTemplate.queryForObject("""
                SELECT cuenta_corriente
                FROM servicios
                WHERE cuenta_corriente = ?
                FOR UPDATE
                """, String.class, cuentaCorriente);

        List<CoordenadasAnteriores> anteriores = jdbcTemplate.query("""
                SELECT latitud, longitud, precision_metros
                FROM ubicaciones_servicio
                WHERE cuenta_corriente = ?
                """, (rs, fila) -> new CoordenadasAnteriores(
                    rs.getBigDecimal("latitud"),
                    rs.getBigDecimal("longitud"),
                    rs.getBigDecimal("precision_metros")),
                cuentaCorriente);
        CoordenadasAnteriores anterior = anteriores.isEmpty() ? null : anteriores.get(0);

        jdbcTemplate.update("""
                INSERT INTO historial_ubicaciones_servicio
                    (cuenta_corriente, latitud_anterior, longitud_anterior,
                     latitud_nueva, longitud_nueva,
                     precision_anterior_metros, precision_nueva_metros,
                     metodo, origen, codigo_usuario_sistema)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, cuentaCorriente,
                anterior == null ? null : anterior.latitud(),
                anterior == null ? null : anterior.longitud(),
                latitud, longitud,
                anterior == null ? null : anterior.precisionMetros(),
                precisionMetros, metodoNormalizado, origenNormalizado,
                codigoUsuarioSistema);

        jdbcTemplate.update("""
                INSERT INTO ubicaciones_servicio
                    (cuenta_corriente, latitud, longitud, precision_metros,
                     metodo, origen, codigo_usuario_sistema)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    latitud = VALUES(latitud),
                    longitud = VALUES(longitud),
                    precision_metros = VALUES(precision_metros),
                    metodo = VALUES(metodo),
                    origen = VALUES(origen),
                    codigo_usuario_sistema = VALUES(codigo_usuario_sistema),
                    fecha_actualizacion = CURRENT_TIMESTAMP
                """, cuentaCorriente, latitud, longitud, precisionMetros,
                metodoNormalizado, origenNormalizado, codigoUsuarioSistema);

        return consultar(cuentaCorriente);
    }

    private String normalizarOrigen(String origen) {
        String normalizado = origen == null
                ? "WEB" : origen.trim().toUpperCase(Locale.ROOT);
        if (!List.of("WEB", "APP").contains(normalizado)) {
            throw new IllegalArgumentException("El origen de la ubicación no es válido");
        }
        return normalizado;
    }

    private String validar(BigDecimal latitud, BigDecimal longitud,
            BigDecimal precisionMetros, String metodo) {
        if (latitud == null || longitud == null) {
            throw new IllegalArgumentException("Indique la latitud y la longitud");
        }
        if (latitud.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitud.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90");
        }
        if (longitud.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitud.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180");
        }
        if (precisionMetros != null && (precisionMetros.signum() < 0
                || precisionMetros.compareTo(BigDecimal.valueOf(100000)) > 0)) {
            throw new IllegalArgumentException("La precisión indicada no es válida");
        }
        String metodoNormalizado = metodo == null
                ? "MANUAL" : metodo.trim().toUpperCase(Locale.ROOT);
        if (!List.of("GPS", "MANUAL", "MAPA").contains(metodoNormalizado)) {
            throw new IllegalArgumentException("El método de ubicación no es válido");
        }
        return metodoNormalizado;
    }

    private static LocalDateTime aLocalDateTime(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }

    private record CoordenadasAnteriores(
            BigDecimal latitud, BigDecimal longitud, BigDecimal precisionMetros) {}

    public record DetalleUbicacion(
            UbicacionActual actual, List<CambioUbicacion> historial) {}

    public record UbicacionActual(
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal precisionMetros,
            String metodo,
            String origen,
            String usuario,
            LocalDateTime fechaRegistro,
            LocalDateTime fechaActualizacion) {}

    public record CambioUbicacion(
            Long codigoHistorial,
            BigDecimal latitudAnterior,
            BigDecimal longitudAnterior,
            BigDecimal latitudNueva,
            BigDecimal longitudNueva,
            BigDecimal precisionAnteriorMetros,
            BigDecimal precisionNuevaMetros,
            String metodo,
            String origen,
            String usuario,
            LocalDateTime fechaModificacion) {}
}
