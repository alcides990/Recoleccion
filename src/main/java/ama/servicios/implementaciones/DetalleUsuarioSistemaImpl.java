package ama.servicios.implementaciones;

import ama.dao.DetalleUsuarioSistemaDao;
import ama.dominio.DetalleUsuarioSistema;
import ama.dominio.DetalleUsuarioSistemaPK;
import ama.dominio.UsuarioSistema;
import java.util.List;
import java.util.Optional;
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
        detalleUsuarioSistemaDao.saveAndFlush(detalleUsuarioSistema);
    }

    @Transactional
    @Modifying
    @Override
    public void eliminar(DetalleUsuarioSistemaPK detalleUsuarioSistemaPK) {
        detalleUsuarioSistemaDao.deleteById(detalleUsuarioSistemaPK);
        detalleUsuarioSistemaDao.flush();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<DetalleUsuarioSistema> buscar(DetalleUsuarioSistemaPK detalleUsuarioSistemaPK) {
        return detalleUsuarioSistemaDao.findById(detalleUsuarioSistemaPK);
    }


}
