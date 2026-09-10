package ama.modulos.egresos;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class EgresoRepository {
    private static final String CONSULTA = """
            SELECT e.id, e.fecha, e.tipo_id, t.nombre tipo, p.nombre proveedor,
                   e.comprobante, e.observacion, e.total, e.anulado
              FROM egresos e
              JOIN egreso_catalogos t ON t.id = e.tipo_id AND t.codigo_sucursal = e.codigo_sucursal
              JOIN egreso_catalogos p ON p.id = e.proveedor_id AND p.codigo_sucursal = e.codigo_sucursal
            """;
    private final JdbcTemplate jdbc;

    public EgresoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EgresoDto> listar(int sucursal, LocalDate desde, LocalDate hasta, Long categoriaId) {
        String filtroCategoria = categoriaId == null ? "" : """
                  AND EXISTS (
                      SELECT 1
                        FROM egreso_detalles d
                        JOIN egreso_catalogos prod
                          ON prod.id = d.producto_id AND prod.codigo_sucursal = d.codigo_sucursal
                       WHERE d.egreso_id = e.id
                         AND d.codigo_sucursal = e.codigo_sucursal
                         AND prod.categoria_id = ?
                  )
                """;
        Object[] parametros = categoriaId == null
                ? new Object[]{sucursal, desde, hasta}
                : new Object[]{sucursal, desde, hasta, categoriaId};
        return jdbc.query(CONSULTA + """
                WHERE e.codigo_sucursal = ? AND e.fecha BETWEEN ? AND ?
                """ + filtroCategoria + """
                ORDER BY e.fecha DESC, e.id DESC
                """, this::mapear, parametros);
    }

    public Optional<EgresoDto> encontrar(int sucursal, long id) {
        return jdbc.query(CONSULTA + " WHERE e.id = ? AND e.codigo_sucursal = ?",
                this::mapear, id, sucursal).stream().findFirst();
    }

    public Optional<Boolean> bloquearEstado(int sucursal, long id) {
        return jdbc.query("""
                SELECT anulado FROM egresos WHERE id = ? AND codigo_sucursal = ? FOR UPDATE
                """, (rs, fila) -> rs.getBoolean("anulado"), id, sucursal).stream().findFirst();
    }

    public List<LineaEgreso> detalles(int sucursal, long id) {
        return jdbc.query("""
                SELECT d.descripcion, d.cantidad, d.precio, d.producto_id,
                       prod.categoria_id, cat.nombre AS categoria
                  FROM egreso_detalles d
                  LEFT JOIN egreso_catalogos prod
                    ON prod.id = d.producto_id AND prod.codigo_sucursal = d.codigo_sucursal
                  LEFT JOIN egreso_catalogos cat
                    ON cat.id = prod.categoria_id AND cat.codigo_sucursal = prod.codigo_sucursal
                 WHERE d.egreso_id = ? AND d.codigo_sucursal = ? ORDER BY d.id
                """, (rs, fila) -> new LineaEgreso(rs.getString("descripcion"),
                rs.getBigDecimal("cantidad"), rs.getBigDecimal("precio"), rs.getLong("producto_id"),
                rs.getObject("categoria_id", Long.class), rs.getString("categoria")), id, sucursal);
    }

    public long crear(int sucursal, int usuario, long proveedorId, EgresoSolicitud dato, BigDecimal total) {
        var clave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("""
                    INSERT INTO egresos (codigo_sucursal, codigo_usuario_sistema, fecha, tipo_id,
                                         proveedor_id, comprobante, observacion, total)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            sentencia.setInt(1, sucursal);
            sentencia.setInt(2, usuario);
            sentencia.setObject(3, dato.fecha());
            sentencia.setLong(4, dato.tipoId());
            sentencia.setLong(5, proveedorId);
            sentencia.setString(6, dato.comprobante());
            sentencia.setString(7, dato.observacion());
            sentencia.setBigDecimal(8, total);
            return sentencia;
        }, clave);
        if (clave.getKey() == null) throw new IllegalStateException("No se generó el identificador del gasto.");
        return clave.getKey().longValue();
    }

    public void actualizar(int sucursal, long id, long proveedorId, EgresoSolicitud dato, BigDecimal total) {
        jdbc.update("""
                UPDATE egresos SET fecha = ?, tipo_id = ?, proveedor_id = ?, comprobante = ?, observacion = ?, total = ?
                 WHERE id = ? AND codigo_sucursal = ?
                """, dato.fecha(), dato.tipoId(), proveedorId, dato.comprobante(), dato.observacion(), total, id, sucursal);
    }

    public void borrarDetalles(int sucursal, long id) {
        jdbc.update("DELETE FROM egreso_detalles WHERE egreso_id = ? AND codigo_sucursal = ?", id, sucursal);
    }

    public void agregarDetalle(int sucursal, long id, long productoId, LineaEgreso linea, BigDecimal subtotal) {
        jdbc.update("""
                INSERT INTO egreso_detalles (egreso_id, codigo_sucursal, producto_id, descripcion, cantidad, precio, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, sucursal, productoId, linea.producto(), linea.cantidad(), linea.precio(), subtotal);
    }

    public boolean anular(int sucursal, long id) {
        return jdbc.update("""
                UPDATE egresos SET anulado = TRUE WHERE id = ? AND codigo_sucursal = ? AND anulado = FALSE
                """, id, sucursal) == 1;
    }

    private EgresoDto mapear(ResultSet rs, int fila) throws SQLException {
        return new EgresoDto(rs.getLong("id"), rs.getDate("fecha").toLocalDate(), rs.getLong("tipo_id"),
                rs.getString("tipo"), rs.getString("proveedor"), rs.getString("comprobante"),
                rs.getString("observacion"), rs.getBigDecimal("total"), rs.getBoolean("anulado"), List.of());
    }
}
