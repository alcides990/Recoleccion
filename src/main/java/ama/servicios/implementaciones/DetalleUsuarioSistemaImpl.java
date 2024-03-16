package ama.servicios.implementaciones;

import ama.dao.DetalleUsuarioSistemaDao;
import ama.dominio.DetalleUsuarioSistema;
import ama.dominio.DetalleUsuarioSistemaPK;
import ama.dominio.UsuarioSistema;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.DetalleUsuarioSistemaService;

@Slf4j
@Service
public class DetalleUsuarioSistemaImpl implements DetalleUsuarioSistemaService {

    @Autowired
    DetalleUsuarioSistemaDao detalleUsuarioSistemaDao;

        
    @Transactional(readOnly = true)
    @Override
    public List<DetalleUsuarioSistema> listar(UsuarioSistema usuarioSistema) {
        return (List<DetalleUsuarioSistema>) detalleUsuarioSistemaDao.findByUser(usuarioSistema);
    }

    @Transactional
    @Override
    public void guardar(DetalleUsuarioSistema detalleUsuarioSistema) {
        detalleUsuarioSistemaDao.save(detalleUsuarioSistema);
    }

    @Transactional
    @Modifying
    @Override
    public void eliminar(DetalleUsuarioSistemaPK detalleUsuarioSistemaPK) {
        
        detalleUsuarioSistemaDao.delete(new DetalleUsuarioSistema(detalleUsuarioSistemaPK));
    }


}
