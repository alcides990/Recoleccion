package ama.servicios.implementaciones;

import ama.dao.TipoComprobanteDao;
import ama.dominio.TipoComprobante;
import ama.servicio.TipoComprobanteService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplTipoComprobante implements TipoComprobanteService {

    @Autowired
    TipoComprobanteDao tipoComprobanteDao;

    @Override
    public List<TipoComprobante> listar() {
        return (List<TipoComprobante>) tipoComprobanteDao.findAll();
    }

    @Override
    public void guardar(TipoComprobante tipoComprobante) {
        tipoComprobanteDao.save(tipoComprobante);
    }

    @Override
    public void eliminar(TipoComprobante tipoComprobante) {
        tipoComprobanteDao.delete(tipoComprobante);
    }

    @Override
    public TipoComprobante encontrar(TipoComprobante tipoComprobante) {
        return tipoComprobanteDao.findById(tipoComprobante.getCodigoTipoComprobante()).orElse(null);
    }

    @Override
    public Integer getCodigoTipoComprobante() {
        return tipoComprobanteDao.getCodigoTipoComprobante() + 1;
    }

}
