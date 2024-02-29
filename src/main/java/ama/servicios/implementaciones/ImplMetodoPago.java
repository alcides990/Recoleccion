package ama.servicios.implementaciones;

import ama.dao.MetodoPagoDao;
import ama.dominio.MetodoPago;
import ama.servicio.MetodoPagoService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplMetodoPago implements MetodoPagoService {

    @Autowired
    MetodoPagoDao metodoPagoDao;

    @Override
    public List<MetodoPago> listar() {
        return (List<MetodoPago>) metodoPagoDao.findAll();
    }

    @Override
    public void guardar(MetodoPago metodoPago) {
        metodoPagoDao.save(metodoPago);
    }

    @Override
    public void eliminar(MetodoPago metodoPago) {
        metodoPagoDao.delete(metodoPago);
    }

    @Override
    public MetodoPago encontrar(MetodoPago metodoPago) {
        return metodoPagoDao.findById(metodoPago.getCodigoMetodoPago()).orElse(null);
    }

    @Override
    public Integer getCodigoMetodoPago() {
        Integer codigoMetodoPago = metodoPagoDao.getCodigoMetodoPago();

        return codigoMetodoPago == null ? 1 : codigoMetodoPago + 1;
    }

}
