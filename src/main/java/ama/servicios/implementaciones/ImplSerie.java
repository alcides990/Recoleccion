package ama.servicios.implementaciones;

import ama.dao.SerieDao;
import ama.dominio.Serie;
import ama.servicio.ServicioSerie;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImplSerie implements ServicioSerie{

    @Autowired
    SerieDao serieDao;

    @Transactional(readOnly = true)
    @Override
    public List<Serie> listar() {
        return (List<Serie>) serieDao.findAll();
    }

    @Transactional
    @Override
    public Serie guardar(Serie categoria) {
     return serieDao.save(categoria);
    }

    @Transactional
    @Override
    public void eliminar(Serie serie) {
        serieDao.delete(serie);
    }

    @Transactional(readOnly = true)
    @Override
    public Serie encontrar(Serie serie) {
        return serieDao.findById(serie.getCodigoSerie()).orElse(null);
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCodigoSerie() {
        Integer codigoSerie = 0;
        if (serieDao.getCodigoSerie()!= null) {
            codigoSerie = serieDao.getCodigoSerie();
        }
        return codigoSerie;
    }

}
