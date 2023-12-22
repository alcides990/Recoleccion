package ama.dao;

import ama.dominio.UsuarioSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioSistemaDao extends JpaRepository<UsuarioSistema, Integer> {

    @Query("""
           SELECT u  FROM UsuarioSistema u 
           LEFT JOIN FETCH u.detalleUsuarioSistema AS dtus
           LEFT JOIN FETCH dtus.rol as r
           LEFT JOIN FETCH u.estado
           WHERE u.nombre= ?1
           """)
UsuarioSistema findByNombre(String nombre);

  //generar codigo de usuario para nuevo registro
    @Query("SELECT MAX(u.codigoUsuarioSistema) as codigoUsuarioSistema FROM UsuarioSistema u ")
    public Integer getCodigoUsuarioSistema();

}
