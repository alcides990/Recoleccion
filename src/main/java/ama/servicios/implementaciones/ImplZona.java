package ama.servicios.implementaciones;

import ama.dao.ZonaDao;
import ama.dominio.Zona;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.ZonaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class ImplZona implements ZonaService {

    @Autowired
    ZonaDao zonaDao;

    @Transactional(readOnly = true)
    @Override
    public List<Zona> listar() {
        return (List<Zona>) zonaDao.listar();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Zona> listarPorSucursal(Pageable pageable, Integer codigoSucursal) {
        return zonaDao.listarPorSucursal(pageable, codigoSucursal);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Zona> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro) {
        return zonaDao.buscarPorSucursal(pageable, codigoSucursal, filtro);
    }

    @Transactional(readOnly = true)
    @Override
    public long contarPorSucursal(Integer codigoSucursal) {
        return zonaDao.contarPorSucursal(codigoSucursal);
    }

    @Transactional
    @Override
    public void guardar(Zona zona) {
        zonaDao.save(zona);
    }

    @Transactional
    @Override
    public void eliminar(Zona zona) {
        zonaDao.delete(zona);
    }

    @Transactional(readOnly = true)
    @Override
    public Zona encontrar(Zona zona) {
        return zonaDao.econtrar(zona);
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCodigoZona() {
        Integer codigoZona = 0;
        if (zonaDao.getCodigoZona() != null) {
            codigoZona = zonaDao.getCodigoZona();
        }
        return codigoZona;
    }

}
