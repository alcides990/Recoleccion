package ama.servicios.implementaciones;

import ama.dao.RolDao;
import ama.dominio.Rol;
import ama.servicio.RolService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplRol implements RolService {

    @Autowired
    RolDao rolDao;

    @Override
    public List<Rol> listar() {
        return (List<Rol>) rolDao.findAll();
    }

    @Override
    public void guardar(Rol rol) {
        rolDao.save(rol);
    }

    @Override
    public void eliminar(Rol rol) {
        rolDao.delete(rol);
    }

    @Override
    public Rol encontrar(Rol rol) {
        return rolDao.findById(rol.getCodigoRol()).orElse(null);
    }

}
