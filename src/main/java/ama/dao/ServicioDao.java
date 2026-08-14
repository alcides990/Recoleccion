package ama.dao;

import ama.dominio.Servicio;
import java.util.List;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioDao extends JpaRepository<Servicio, Integer> {

    @Query(value = """
            SELECT s.cuenta_corriente,
                   DATE_FORMAT(
                       DATE_ADD(s.fecha_inicio, INTERVAL COALESCE(SUM(
                           CASE WHEN c.codigo_estado = 1
                                THEN COALESCE(c.cantidad_pago, 0) ELSE 0 END
                       ), 0) MONTH),
                       '%m-%Y'
                   ) AS pago_desde,
                   COALESCE((
                       SELECT c2.saldo
                         FROM comprobantes c2
                        WHERE c2.cuenta_corriente = s.cuenta_corriente
                          AND c2.codigo_estado = 1
                        ORDER BY c2.fecha_emision DESC,
                                 c2.numero_comprobante DESC,
                                 c2.codigo_sucursal DESC,
                                 c2.codigo_punto_expedicion DESC,
                                 c2.codigo_serie DESC,
                                 c2.codigo_tipo_comprobante DESC
                        LIMIT 1
                   ), 0) AS saldo
              FROM servicios s
              LEFT JOIN comprobantes c ON c.cuenta_corriente = s.cuenta_corriente
             WHERE s.cuenta_corriente IN (:cuentas)
             GROUP BY s.cuenta_corriente, s.fecha_inicio
            """, nativeQuery = true)
    List<Object[]> resumirEstadosMovil(@Param("cuentas") Collection<String> cuentas);

    @Query("""
            SELECT s FROM Servicio s
            JOIN FETCH s.usuario u
            JOIN FETCH s.categoria cat
            JOIN FETCH s.manzana m
            JOIN FETCH s.estado est
            WHERE s.sucursal.codigoSucursal = :sucursal
              AND est.codigoEstado = 1
              AND m.manzanaPK.numeroManzana = :manzana
            ORDER BY u.nombre, u.apellido, s.cuentaCorriente
            """)
    List<Servicio> buscarActivosPorManzana(@Param("sucursal") Integer sucursal,
            @Param("manzana") Integer manzana, Pageable limite);

    @Query("""
            SELECT s FROM Servicio s
            JOIN FETCH s.usuario u
            JOIN FETCH s.categoria cat
            JOIN FETCH s.manzana m
            JOIN FETCH s.estado est
            WHERE s.sucursal.codigoSucursal = :sucursal
              AND est.codigoEstado = 1
              AND LOWER(CONCAT(u.nombre, ' ', COALESCE(u.apellido, '')))
                  LIKE LOWER(CONCAT('%', :nombre, '%'))
            ORDER BY u.nombre, u.apellido, s.cuentaCorriente
            """)
    List<Servicio> buscarActivosPorNombre(@Param("sucursal") Integer sucursal,
            @Param("nombre") String nombre, Pageable limite);

    @Query("""
           SELECT COUNT(s) FROM Servicio s
           WHERE s.sucursal.codigoSucursal = :codigoSucursal
           """)
    long contarServiciosPorSucursal(@Param("codigoSucursal") Integer codigoSucursal);

    @Query(value = """
            SELECT s FROM Servicio s
            JOIN FETCH s.usuario u
            JOIN FETCH s.categoria cat
            JOIN FETCH s.estado est
            WHERE s.sucursal.codigoSucursal = :codigoSucursal
            """, countQuery = """
            SELECT COUNT(s) FROM Servicio s
            WHERE s.sucursal.codigoSucursal = :codigoSucursal
            """)
    Page<Servicio> listarPorSucursal(Pageable pagina,
            @Param("codigoSucursal") Integer codigoSucursal);

    @Query(value = """
            SELECT s FROM Servicio s
            JOIN FETCH s.usuario u
            JOIN FETCH s.categoria cat
            JOIN FETCH s.estado est
            WHERE s.sucursal.codigoSucursal = :codigoSucursal
              AND (LOWER(s.cuentaCorriente) LIKE LOWER(CONCAT('%', :filtro, '%'))
                OR LOWER(u.numeroDocumento) LIKE LOWER(CONCAT('%', :filtro, '%'))
                OR LOWER(CONCAT(u.nombre, ' ', COALESCE(u.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')))
            """, countQuery = """
            SELECT COUNT(s) FROM Servicio s
            JOIN s.usuario u
            WHERE s.sucursal.codigoSucursal = :codigoSucursal
              AND (LOWER(s.cuentaCorriente) LIKE LOWER(CONCAT('%', :filtro, '%'))
                OR LOWER(u.numeroDocumento) LIKE LOWER(CONCAT('%', :filtro, '%'))
                OR LOWER(CONCAT(u.nombre, ' ', COALESCE(u.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')))
            """)
    Page<Servicio> buscarPorSucursal(Pageable pagina,
            @Param("codigoSucursal") Integer codigoSucursal,
            @Param("filtro") String filtro);

    // listar servicio

    @Query(value = """
                        SELECT   s FROM Servicio s
                                   JOIN FETCH s.usuario u
                                   JOIN FETCH s.categoria cat
                                   JOIN FETCH s.estado est
                        """, countQuery = "SELECT COUNT(s) FROM Servicio s")
    Page<Servicio> listar(Pageable pagina);

    // buscar servicio
    @Query(value = """
                         SELECT s FROM Servicio s
                                  JOIN FETCH s.usuario u
                                  JOIN FETCH s.categoria cat
                                  JOIN FETCH s.manzana m
                                  JOIN FETCH m.zona z
                                  JOIN FETCH z.cobrador cob
                                  JOIN FETCH m.sucursal suc
                                  JOIN FETCH suc.ciudad
                                  JOIN FETCH s.estado est
                                  WHERE s.cuentaCorriente
                                  LIKE %?1% OR u.numeroDocumento
                                  LIKE %?1% OR CONCAT_WS(' ',u.nombre, u.apellido)
                                  LIKE %?1%
                        """, countQuery = """
                                              SELECT COUNT(s)
                                                  FROM Servicio s
                                                  JOIN s.usuario u
                                                  WHERE s.cuentaCorriente LIKE %?1%
                                                     OR u.numeroDocumento LIKE %?1%
                                                     OR CONCAT_WS(' ',u.nombre,u.apellido) LIKE %?1%
                                          """)
    Page<Servicio> buscar(Pageable pagina, String filtro);

    // Page<Servicio> findByCuentaCorriente(Pageable pagina, String
    // cuentaCorriente);
    @Query("SELECT cuentaCorriente FROM Servicio s  JOIN s.usuario u WHERE u.codigoUsuario= ?1")
    List<String> listaServicioCuenta(Integer codigoUsuario);

    @Query("""
                        SELECT s FROM Servicio s
                                 JOIN FETCH s.usuario u
                                 JOIN FETCH s.categoria cat
                                 JOIN FETCH s.estado est
                                 JOIN FETCH s.manzana m
                                 JOIN FETCH m.zona z
                                 JOIN FETCH z.cobrador c
                                 JOIN FETCH m.sucursal suc
                                 JOIN FETCH suc.ciudad
                        WHERE s.cuentaCorriente= ?1
                        """)
    Servicio encontrar(String cuentaCorriente);

}
