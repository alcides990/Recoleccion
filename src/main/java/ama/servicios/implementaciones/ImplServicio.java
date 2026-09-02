package ama.servicios.implementaciones;

import ama.dao.ServicioDao;
import ama.dominio.Servicio;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.ServicioService;

@Service
public class ImplServicio implements ServicioService {

    @Autowired
    ServicioDao servicioDao;

    @Transactional(readOnly = true)
    public List<Servicio> listar() {
        return (List<Servicio>) servicioDao.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Servicio> listar(Pageable pageable) {
        return servicioDao.listar(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Servicio> buscar(Pageable pageable, String filtro) {
        return servicioDao.buscar(pageable, filtro);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Servicio> listarPorSucursal(Pageable pageable, Integer codigoSucursal) {
        return servicioDao.listarPorSucursal(pageable, codigoSucursal);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Servicio> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro) {
        return servicioDao.buscarPorSucursal(pageable, codigoSucursal, filtro);
    }

    @Transactional(readOnly = true)
    @Override
    public long contarPorSucursal(Integer codigoSucursal) {
        return servicioDao.contarServiciosPorSucursal(codigoSucursal);
    }

    @Transactional
    @Override
    public void guardar(Servicio servicio) {
        servicioDao.save(servicio);
    }

    @Transactional
    @Override
    public void eliminar(Servicio servicio) {
        servicioDao.delete(servicio);
        servicioDao.flush();
    }

  
    @Transactional(readOnly = true)
    @Override
    public Servicio encontrar(String cuentaCorriente) {
        return servicioDao.encontrar(cuentaCorriente);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> listaServicioCuenta(Servicio servicio) {
        return servicioDao.listaServicioCuenta(servicio.getUsuario().getCodigoUsuario());
    }

}
