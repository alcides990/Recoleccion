package ama.servicios.implementaciones;

import ama.dao.CiudadDao;
import ama.dominio.Ciudad;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ama.servicio.CiudadService;

@Service
@RequiredArgsConstructor
public class ImplCiudad implements CiudadService {

    private final CiudadDao ciudadDao;

    @Override
    public List<Ciudad> listarCiudad() {
        return ciudadDao.findAll();
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
        Integer codigoMaximo = ciudadDao.getCodigoCiudad();
        return codigoMaximo == null ? 0 : codigoMaximo;
    }

}
