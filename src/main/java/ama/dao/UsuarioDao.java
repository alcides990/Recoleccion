package ama.dao;

import ama.dominio.Usuario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
           SELECT u FROM Usuario u
           JOIN FETCH u.sucursal s
           JOIN FETCH s.ciudad c
           JOIN FETCH u.estado e
           WHERE s.codigoSucursal = :codigoSucursal
           """, countQuery = """
           SELECT COUNT(u) FROM Usuario u
           WHERE u.sucursal.codigoSucursal = :codigoSucursal
           """)
    Page<Usuario> listarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal);

    @Query(value = """
           SELECT u FROM Usuario u
           JOIN FETCH u.sucursal s
           JOIN FETCH s.ciudad c
           JOIN FETCH u.estado e
           WHERE s.codigoSucursal = :codigoSucursal
             AND (LOWER(u.numeroDocumento) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(CONCAT(u.nombre, ' ', COALESCE(u.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.celular, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.correo, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.barrio, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.direccion, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """, countQuery = """
           SELECT COUNT(u) FROM Usuario u
           WHERE u.sucursal.codigoSucursal = :codigoSucursal
             AND (LOWER(u.numeroDocumento) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(CONCAT(u.nombre, ' ', COALESCE(u.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.celular, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.correo, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.barrio, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(u.direccion, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """)
    Page<Usuario> buscarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal,
            @Param("filtro") String filtro);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.sucursal.codigoSucursal = :codigoSucursal")
    long contarPorSucursal(@Param("codigoSucursal") Integer codigoSucursal);

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
