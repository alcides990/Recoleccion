package ama.servicios.implementaciones;

import ama.dao.PuntoExpedicionDao;
import ama.dominio.PuntoExpedicion;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.PuntoExpedicionService;

@Service
public class ImplPuntoiExpedicion implements PuntoExpedicionService {

    @Autowired
    PuntoExpedicionDao puntoExpedicionDao;

    @Override
    public List<PuntoExpedicion> listar() {
        return (List<PuntoExpedicion>) puntoExpedicionDao.getAll();
    }

    @Override
    public List<PuntoExpedicion> listar(Sucursal sucursal) {
        
        return (List<PuntoExpedicion>) puntoExpedicionDao.getAllFiandSucursal(sucursal);
    }

    @Override
    public List<PuntoExpedicion> isMayorCero(Sucursal sucursal) {
        return (List<PuntoExpedicion>) puntoExpedicionDao.fiandBySucursalAndCodigoPunteExpedicionMayorCero(sucursal);
    }

    @Override
    public void guardar(PuntoExpedicion cobrador) {
        puntoExpedicionDao.save(cobrador);
    }

    @Override
    public void eliminar(PuntoExpedicionPK puntoExpedicionPK) {
        puntoExpedicionDao.eliminar(puntoExpedicionPK);
    }

    @Override
    public PuntoExpedicion encontrar(PuntoExpedicionPK puntoExpedicionPK) {
        return puntoExpedicionDao.getPuntoExpedicion(puntoExpedicionPK);
    }

    @Override
    public Integer getCodigoPuntoExpedicion(Sucursal sucursal) {
        Integer codigoPuntoExpedicion = puntoExpedicionDao.getCodigoPuntoExpedicion(sucursal);

        return codigoPuntoExpedicion != null ? codigoPuntoExpedicion : 0;
    }

}
