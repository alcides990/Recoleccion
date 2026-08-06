package ama.servicios.implementaciones;

import ama.dao.CobradorDao;
import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.CobradorService;

@Service
public class ImplCobrador implements CobradorService {

    @Autowired
    CobradorDao cobradorDao;

    @Override
    public List<Cobrador> listar(Sucursal sucursal) {
        return cobradorDao.listar(sucursal);
    }

    @Override
    public List<Cobrador> listarIsEstadoActivo(Sucursal sucursal) {
        return cobradorDao.listarIsEstdoActivo(sucursal) ;
    }

    @Override
    public void guardar(Cobrador cobrador) {
        cobradorDao.save(cobrador);
    }

    @Override
    public void eliminar(Cobrador cobrador) {
        cobradorDao.delete(cobrador);
    }

    @Override
    public Cobrador encontrar(Cobrador cobrador) {
        return cobradorDao.encontrar(cobrador);
    }

    @Override
    public Integer getCodigoCobrador() {
        Integer codigoCobrador = 0;
        if (cobradorDao.getCodigoCobrador() != null) {
            codigoCobrador = cobradorDao.getCodigoCobrador();
        }
        return codigoCobrador;
    }

}
