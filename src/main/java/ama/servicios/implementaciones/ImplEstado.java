 
package ama.servicios.implementaciones;

import ama.dao.EstadoDao;
import ama.dominio.Estado;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.EstadoService;

 @Service
public class ImplEstado implements EstadoService {

    @Autowired
    EstadoDao EstadoDao;
            
    @Override
    public List<Estado> listar() {
       return (List<Estado>) EstadoDao.findAll();
    }
    @Override
    public List<Estado> findByEstadoIn(List<String> estados ){
       return (List<Estado>) EstadoDao.findByEstadoIn(estados);
    }

    @Override
    public void guardar(Estado estado) {
        EstadoDao.save(estado);
    }

    @Override
    public void eliminar(Estado estado) {
        EstadoDao.delete(estado);
    }

    @Override
    public Estado encontrar(Estado estado) {
       return EstadoDao.findById(estado.getCodigoEstado()).orElse(null);
    }
    
}
