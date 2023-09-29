package ama.dao;

import ama.dominio.Usuario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

    //generar codigo de usuario para nuevo registro
    @Query("SELECT MAX(u.codigoUsuario) as codigoUsuario FROM Usuario u ")
    public Integer getCodigoUsuario();

    //Filtrar usuario por nombre y apellidos o numero de documento
    @Query("""
           SELECT u FROM Usuario u 
           JOIN fetch u.sucursal AS s
           JOIN fetch s.ciudad ciud
           WHERE u.numeroDocumento LIKE %?1% OR concat( u.nombre, ' ', u.apellido) LIKE %?1% 
           """)
    public List<Usuario> buscarUsuario(String filtro);

    //Filtrar usuario por nombre y apellidos o numero de documento con paginacion
    @Query(value = """
       SELECT u FROM Usuario u 
       JOIN FETCH u.sucursal AS s
       JOIN FETCH s.ciudad AS c
       WHERE u.numeroDocumento LIKE %?1% OR concat( u.nombre, ' ', u.apellido) LIKE %?1% """,
            countQuery = "SELECT COUNT(u) FROM Usuario u WHERE u.numeroDocumento LIKE %?1% OR concat( u.nombre, ' ', u.apellido) LIKE %?1%"
    )
    public Page<Usuario> getUsuarios(Pageable pageable, String filtro);

    //Filtrar usuario por nombre y apellidos o numero de documento
    @Query("""
           SELECT u FROM Usuario u 
           JOIN FETCH u.tipoDocumento AS td
           JOIN FETCH u.sucursal AS s
           JOIN FETCH s.ciudad AS c
           WHERE u.codigoUsuario =?1
           """)
    public Usuario encontrar(Integer codigoUsuario);
}
