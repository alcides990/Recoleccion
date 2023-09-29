package ama.servicios.implementaciones;

import ama.dao.PuntoExpedicionDao;
import ama.dominio.PuntoExpedicion;
import ama.servicio.ServicioPuntoExpedicion;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplPuntoiExpedicion implements ServicioPuntoExpedicion {

    @Autowired
    PuntoExpedicionDao puntoExpedicionDao;

    @Override
    public List<PuntoExpedicion> listar() {
        return (List<PuntoExpedicion>) puntoExpedicionDao.findAll();
    }

    @Override
    public void guardar(PuntoExpedicion cobrador) {
        puntoExpedicionDao.save(cobrador);
    }

    @Override
    public void eliminar(PuntoExpedicion cobrador) {
        puntoExpedicionDao.delete(cobrador);
    }

    @Override
    public PuntoExpedicion encontrar(PuntoExpedicion puntoExpedicion) {
        return puntoExpedicionDao.findById(puntoExpedicion.getCodigoPuntoExpedicion()).orElse(null);
    }

    @Override
    public Integer getCodigoPuntoExpedicion() {
        Integer codigoPuntoExpedicion = 0;
        if (puntoExpedicionDao.getCodigoPuntoExpedicion()!= null) {
            codigoPuntoExpedicion = puntoExpedicionDao.getCodigoPuntoExpedicion();
        }
        return codigoPuntoExpedicion;
    }

}
