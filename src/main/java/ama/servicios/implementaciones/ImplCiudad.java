package ama.servicios.implementaciones;

import ama.dao.CiudadDao;
import ama.dominio.Ciudad;
import ama.servicio.ServicioCiudad;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplCiudad implements ServicioCiudad {

    @Autowired
    CiudadDao ciudadDao;

    @Override
    public List<Ciudad> listarCiudad() {
        return (List<Ciudad>) ciudadDao.findAll();
    }

    @Override
    public Ciudad guardar(Ciudad ciudad) {
        return ciudadDao.save(ciudad);
    }

    @Override
    public void eliminar(Ciudad ciudad) {
        ciudadDao.delete(ciudad);
    }

    @Override
    public Ciudad encontrarCiudad(Ciudad ciudad) {
        return ciudadDao.findById(ciudad.getCodigoCiudad()).orElse(null);
    }

    @Override
    public Integer getCodigoCiudad() {
        Integer codigoCiudad = 0;
        if (ciudadDao.getCodigoCiudad() != null) {
            codigoCiudad = ciudadDao.getCodigoCiudad();
        }
        return codigoCiudad;
    }

}
