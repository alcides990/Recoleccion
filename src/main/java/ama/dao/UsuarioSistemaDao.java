package ama.dao;

import ama.dominio.UsuarioSistema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioSistemaDao extends JpaRepository<UsuarioSistema, Integer> {

    @Query("""
           SELECT u  FROM UsuarioSistema u 
           LEFT JOIN FETCH u.detalleUsuarioSistema AS dtus
           LEFT JOIN FETCH dtus.rol as r
           JOIN FETCH u.sucursal s
           JOIN FETCH s.empresa 
           JOIN FETCH s.ciudad
           JOIN FETCH u.estado
           WHERE u.nombre= ?1
           """)
    UsuarioSistema findByNombre(String nombre);

    @Query(value = """
           SELECT u  FROM UsuarioSistema u 
           JOIN FETCH u.sucursal s
           JOIN FETCH s.empresa 
           JOIN FETCH s.ciudad
           JOIN FETCH u.estado
           WHERE u.codigoUsuarioSistema > 0
           """,
            countQuery = "SELECT COUNT(u) FROM UsuarioSistema u  WHERE u.codigoUsuarioSistema >0")
    Page<UsuarioSistema> listar(Pageable pageable); 

    //generar codigo de usuario para nuevo registro
    @Query("SELECT MAX(u.codigoUsuarioSistema) as codigoUsuarioSistema FROM UsuarioSistema u ")
    public Integer getCodigoUsuarioSistema();

}
