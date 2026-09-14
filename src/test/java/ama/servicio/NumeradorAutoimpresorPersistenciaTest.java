package ama.servicio;

import ama.dominio.DetalleTimbrado;
import ama.dominio.DetalleTimbradoPK;
import ama.dominio.Serie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumeradorAutoimpresorPersistenciaTest {
    private EmbeddedDatabase db;
    private JdbcTemplate jdbc;
    private NumeradorAutoimpresorService numerador;
    private DetalleTimbrado detalle;

    @BeforeEach
    void preparar() {
        db = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(db);
        jdbc.execute("SET MODE MySQL");
        jdbc.execute("""
            CREATE TABLE comprobantes (numero_comprobante INT, codigo_sucursal INT,
                codigo_punto_expedicion INT, codigo_tipo_comprobante INT, codigo_serie INT,
                codigo_estado INT)
            """);
        jdbc.execute("""
            CREATE TABLE numeradores_autoimpresor (codigo_timbrado INT, codigo_punto_expedicion INT,
                codigo_sucursal INT, codigo_tipo_comprobante INT, codigo_serie INT, ultimo_numero INT,
                PRIMARY KEY(codigo_timbrado, codigo_punto_expedicion, codigo_sucursal,
                    codigo_tipo_comprobante, codigo_serie))
            """);
        numerador = new NumeradorAutoimpresorService(jdbc);
        detalle = new DetalleTimbrado();
        detalle.setDetalleTimbradoPK(new DetalleTimbradoPK(5, 2, 3));
        detalle.setSerie(new Serie(0));
        detalle.setModoEmision("AUTOIMPRESOR");
        detalle.setNumeroDesde(100);
        detalle.setNumeroHasta(999);
    }

    @AfterEach
    void cerrar() {
        db.shutdown();
    }

    @Test
    void sugiereYReservaDespuesDelMaximoAunqueElContadorEsteAtrasado() {
        jdbc.update("INSERT INTO numeradores_autoimpresor VALUES (5, 2, 3, 4, 0, 110)");
        jdbc.update("INSERT INTO comprobantes VALUES (150, 3, 2, 4, 0, 1)");
        jdbc.update("INSERT INTO comprobantes VALUES (170, 3, 2, 4, 0, 3)");

        assertEquals(171, numerador.consultarSiguiente(detalle, 4));
        assertEquals(110, jdbc.queryForObject("SELECT ultimo_numero FROM numeradores_autoimpresor", Integer.class));
        assertEquals(171, numerador.reservar(detalle, 4));
        assertEquals(172, numerador.consultarSiguiente(detalle, 4));
    }

    @Test
    void respetaElContadorCuandoEsMayorQueLosComprobantes() {
        jdbc.update("INSERT INTO numeradores_autoimpresor VALUES (5, 2, 3, 4, 0, 200)");
        jdbc.update("INSERT INTO comprobantes VALUES (150, 3, 2, 4, 0, 1)");
        assertEquals(201, numerador.consultarSiguiente(detalle, 4));
        assertEquals(201, numerador.reservar(detalle, 4));
    }

    @Test
    void consultaSinReservarYRespetaElInicioDelRango() {
        assertEquals(100, numerador.consultarSiguiente(detalle, 4));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM numeradores_autoimpresor", Integer.class));
        assertEquals(100, numerador.reservar(detalle, 4));
    }

    @Test
    void noMezclaSucursalesPuntosTiposNiSeries() {
        jdbc.update("INSERT INTO comprobantes VALUES (800, 9, 2, 4, 0, 1)");
        jdbc.update("INSERT INTO comprobantes VALUES (800, 3, 9, 4, 0, 1)");
        jdbc.update("INSERT INTO comprobantes VALUES (800, 3, 2, 9, 0, 1)");
        jdbc.update("INSERT INTO comprobantes VALUES (800, 3, 2, 4, 9, 1)");
        jdbc.update("INSERT INTO numeradores_autoimpresor VALUES (9, 2, 3, 4, 0, 800)");
        detalle.setSerie(null);
        assertEquals(100, numerador.consultarSiguiente(detalle, 4));
    }

    @Test
    void rechazaElRangoAgotadoAunqueElContadorEsteAtrasado() {
        jdbc.update("INSERT INTO numeradores_autoimpresor VALUES (5, 2, 3, 4, 0, 110)");
        jdbc.update("INSERT INTO comprobantes VALUES (999, 3, 2, 4, 0, 1)");
        assertThrows(IllegalStateException.class, () -> numerador.consultarSiguiente(detalle, 4));
        assertThrows(IllegalStateException.class, () -> numerador.reservar(detalle, 4));
        assertEquals(110, jdbc.queryForObject("SELECT ultimo_numero FROM numeradores_autoimpresor", Integer.class));
    }
}
