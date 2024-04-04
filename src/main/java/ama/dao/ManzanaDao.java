package ama.dao;

import ama.dominio.*;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ManzanaDao extends JpaRepository<Manzana, ManzanaPK> {

       // Seleccionar una manzana
       @Query("""
                     SELECT  m  FROM Manzana m
                     JOIN FETCH m.zona z
                     JOIN FETCH m.sucursal s
                     JOIN FETCH s.ciudad
                     JOIN FETCH z.cobrador c
                     WHERE m.manzanaPK = ?1 """)
       Manzana encontrar(ManzanaPK manzanaPK);

       // Seleccionar Lista detallezona correspomdiende a una zona y cobrador
       @Query(value = """
                     SELECT  m FROM Manzana m
                      JOIN FETCH m.zona z
                      JOIN FETCH z.cobrador c
                      JOIN FETCH m.sucursal suc
                      JOIN FETCH suc.ciudad ciud
                      WHERE z=?1
                             """)
       List<Manzana> listar(Zona zona);

       @Transactional
       @Modifying(flushAutomatically = true)
       @Query("DELETE FROM Manzana m WHERE m.manzanaPK = ?1")
       void eliminarManzana(ManzanaPK manzanaPK);

}
