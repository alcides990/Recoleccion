package ama.dao;

import ama.dominio.DetalleUsuarioSistema;
import ama.dominio.UsuarioSistema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DetalleUsuarioSistemaDao extends JpaRepository<DetalleUsuarioSistema, Integer> {

    @Query(value = """
                   SELECT dtus FROM DetalleUsuarioSistema AS dtus
                   JOIN FETCH dtus.usuarioSistema AS us
                   JOIN FETCH dtus.rol
                   WHERE us=?1
                """)
    List<DetalleUsuarioSistema> findByUser(UsuarioSistema usuarioSistema);

    @Modifying()
    @Transactional
    @Query("DELETE FROM DetalleUsuarioSistema dtus WHERE dtus.usuarioSistema = ?1")
    void deleteByUsuarioSistema(UsuarioSistema usuarioSistema);

}
