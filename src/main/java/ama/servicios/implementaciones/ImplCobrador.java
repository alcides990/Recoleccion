package ama.servicios.implementaciones;

import ama.dao.CobradorDao;
import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.CobradorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImplCobrador implements CobradorService {

    @Autowired
    CobradorDao cobradorDao;

    @Override
    public List<Cobrador> listar(Sucursal sucursal) {
        return cobradorDao.listar(sucursal);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Cobrador> listarPorSucursal(Pageable pageable, Integer codigoSucursal) {
        return cobradorDao.listarPorSucursal(pageable, codigoSucursal);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Cobrador> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro) {
        return cobradorDao.buscarPorSucursal(pageable, codigoSucursal, filtro);
    }

    @Transactional(readOnly = true)
    @Override
    public long contarPorSucursal(Integer codigoSucursal) {
        return cobradorDao.contarPorSucursal(codigoSucursal);
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
    @Transactional
    public void eliminar(Cobrador cobrador) {
        cobradorDao.delete(cobrador);
        cobradorDao.flush();
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
