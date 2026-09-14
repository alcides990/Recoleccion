package ama.modulos.egresos;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(EgresoIntegracionTest.Configuracion.class)
class EgresoIntegracionTest {
    @Configuration
    @EnableTransactionManagement
    static class Configuracion {
        @Bean DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }
        @Bean JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean CatalogoRepository catalogoRepository(JdbcTemplate jdbc) { return new CatalogoRepository(jdbc); }
        @Bean EgresoRepository egresoRepository(JdbcTemplate jdbc) { return new EgresoRepository(jdbc); }
        @Bean CatalogoService catalogoService(CatalogoRepository r) { return new CatalogoService(r); }
        @Bean EgresoService egresoService(EgresoRepository r, CatalogoService c) { return new EgresoService(r, c); }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EgresoService gastos;
    @Autowired CatalogoService catalogos;
    private long tipo;
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 8);

    @BeforeEach void esquemaAislado() throws Exception {
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("SET MODE MySQL");
        try (var fuente = new ClassPathResource("db/migration/V17__modulo_egresos.sql").getInputStream()) {
            // H2 usa las mismas columnas y claves; se omiten opciones físicas exclusivas de MySQL.
            String sql = new String(fuente.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", "");
            for (String sentencia : sql.split(";")) {
                if (!sentencia.isBlank()) jdbc.execute(sentencia);
            }
        }
        try (var fuente = new ClassPathResource("db/migration/V18__datos_catalogos_egresos.sql").getInputStream()) {
            jdbc.execute(new String(fuente.readAllBytes(), StandardCharsets.UTF_8));
        }
        jdbc.execute("ALTER TABLE egreso_catalogos ADD COLUMN categoria_id BIGINT NULL");
        tipo = catalogos.guardar(1, ClaseCatalogo.TIPO, CatalogoDto.nuevo("Mantenimiento"));
    }

    @Test void guardaReutilizaCatalogosEditaYAnula() {
        long id = gastos.guardar(1, 5, null, solicitud("  Aceite   motor ", "2.5", "10.01"));
        assertEquals(new BigDecimal("25.03"), gastos.detalle(1, id).total());
        gastos.guardar(1, 5, null, solicitud("Aceite motor", "1", "10.01"));
        assertEquals(1, catalogos.buscar(1, ClaseCatalogo.PROVEEDOR, "").size());
        assertEquals(1, catalogos.buscar(1, ClaseCatalogo.PRODUCTO, "").size());
        gastos.guardar(1, 5, id, solicitud("Aceite motor", "3", "12"));
        assertEquals(new BigDecimal("36.00"), gastos.detalle(1, id).total());
        assertEquals(1, gastos.detalle(1, id).detalles().size());
        assertEquals(new BigDecimal("12.00"), catalogos.buscar(1, ClaseCatalogo.PRODUCTO, "").get(0).monto());
        gastos.anular(1, id);
        assertTrue(gastos.detalle(1, id).anulado());
        assertThrows(EgresoException.class, () -> gastos.guardar(1, 5, id, solicitud("Aceite motor", "1", "1")));
    }

    @Test void mantieneAislamientoPorSucursal() {
        long id = gastos.guardar(1, 5, null, solicitud("Aceite", "1", "10"));
        assertTrue(gastos.listar(2, FECHA, FECHA).isEmpty());
        assertTrue(catalogos.buscar(2, ClaseCatalogo.PRODUCTO, "").isEmpty());
        assertThrows(EgresoException.class, () -> gastos.detalle(2, id));
        assertThrows(EgresoException.class, () -> gastos.anular(2, id));
        assertThrows(EgresoException.class, () -> gastos.guardar(2, 5, null, solicitud("Aceite", "1", "10")));
        assertFalse(gastos.detalle(1, id).anulado());
    }

    @Test void revierteCabeceraDetallesYCatalogosSiFallaLaPersistencia() {
        jdbc.execute("ALTER TABLE egreso_catalogos ADD CONSTRAINT fallo_prueba CHECK (nombre <> 'Falla')");
        var dato = new EgresoSolicitud(FECHA, tipo, "Proveedor nuevo", "", "", List.of(
                new LineaEgreso("Primero", BigDecimal.ONE, BigDecimal.TEN),
                new LineaEgreso("Falla", BigDecimal.ONE, BigDecimal.TEN)));
        assertThrows(RuntimeException.class, () -> gastos.guardar(1, 5, null, dato));
        assertTrue(gastos.listar(1, FECHA, FECHA).isEmpty());
        assertTrue(catalogos.buscar(1, ClaseCatalogo.PROVEEDOR, "").isEmpty());
        assertTrue(catalogos.buscar(1, ClaseCatalogo.PRODUCTO, "").isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM egreso_detalles", Integer.class));
    }

    @Test void revierteEdicionConservandoDetallesOriginales() {
        long id = gastos.guardar(1, 5, null, solicitud("Aceite", "1", "10"));
        jdbc.execute("ALTER TABLE egreso_catalogos ADD CONSTRAINT fallo_prueba CHECK (nombre <> 'Falla')");
        assertThrows(RuntimeException.class, () -> gastos.guardar(1, 5, id, solicitud("Falla", "9", "99")));
        assertEquals(new BigDecimal("10.00"), gastos.detalle(1, id).total());
        assertEquals("Aceite", gastos.detalle(1, id).detalles().get(0).producto());
    }

    @Test void incluyeAmbosExtremosDelPeriodo() {
        gastos.guardar(1, 5, null, solicitud("Aceite", "1", "10"));
        assertEquals(1, gastos.listar(1, FECHA, FECHA).size());
        assertTrue(gastos.listar(1, FECHA.plusDays(1), FECHA.plusDays(2)).isEmpty());
    }

    private EgresoSolicitud solicitud(String producto, String cantidad, String precio) {
        return new EgresoSolicitud(FECHA, tipo, "Proveedor nuevo", "001", "", List.of(
                new LineaEgreso(producto, new BigDecimal(cantidad), new BigDecimal(precio))));
    }
}
