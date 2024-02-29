package ama.servicios.implementaciones;

import ama.dao.DetalleComprobanteDao;
import ama.dominio.DetalleComprobante;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.DetalleComprobanteService;

@Slf4j
@Service
public class ImplDetalleComprobante implements DetalleComprobanteService {

    @Autowired
    DetalleComprobanteDao detalleComprobanteDao;

    @Transactional(readOnly = true)
    @Override
    public List<DetalleComprobante> listar() {
        return (List<DetalleComprobante>) detalleComprobanteDao.findAll();
    }

    @Transactional
    @Override
    public void guardar(DetalleComprobante detalleComprobante) {
        detalleComprobanteDao.save(detalleComprobante);
    }

    @Transactional
    @Override
    public void eliminar(DetalleComprobante detalleComprobante) {
        detalleComprobanteDao.delete(detalleComprobante);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DetalleComprobante> encontrar(DetalleComprobante detalleComprobante) {
        return null;//(List<Detallecomprobante>) detalleComprobanteDao.findByNumeroComprobante(detalleComprobante.getComprobante().getNumeroComprobante());
    }

    

 

}
