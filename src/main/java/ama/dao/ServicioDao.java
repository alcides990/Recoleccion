package ama.dao;

import ama.dominio.Servicio;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServicioDao extends JpaRepository<Servicio, Integer> {
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
                      JOIN FETCH m.sucursal suc
                      JOIN FETCH suc.ciudad
                      JOIN FETCH s.estado est
                      WHERE s.cuentaCorriente
                      LIKE %?1% OR u.numeroDocumento
                      LIKE %?1% OR CONCAT(u.nombre,' ',u.apellido)
                      LIKE %?1%
            """, countQuery = "SELECT COUNT(s) FROM Servicio s   ")
    Page<Servicio> buscar(Pageable pagina, String filtro);

    // Page<Servicio> findByCuentaCorriente(Pageable pagina, String
    // cuentaCorriente);
    @Query("SELECT s FROM Servicio s  JOIN s.usuario u WHERE u.codigoUsuario= ?1")
    List<Servicio> listaServicioCuenta(Integer codigoUsuario);

    @Query("""
            SELECT s FROM Servicio s
                     JOIN FETCH s.usuario u
                     JOIN FETCH s.categoria cat
                     JOIN FETCH s.estado est
                     JOIN FETCH s.manzana m
                     JOIN FETCH m.cobrador c
                     JOIN FETCH m.zona z
                     JOIN FETCH m.sucursal suc
                     JOIN FETCH suc.ciudad
            WHERE s.cuentaCorriente= ?1
            """)
    Servicio encontrar(String cuentaCorriente);
}
