package ama.servicios.implementaciones;

import ama.dao.DetalleZonaDao;
import ama.dominio.Cobrador;
import ama.dominio.DetalleZona;
import ama.dominio.DetalleZonaPK;
import ama.dominio.Zona;
import ama.servicio.ServicioDetalleZona;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ImplDetalleZona implements ServicioDetalleZona {

    @Autowired
    DetalleZonaDao detalleZonaDao;

        
    @Transactional(readOnly = true)
    @Override
    public List<DetalleZona> listar(Zona zona) {
        return (List<DetalleZona>) detalleZonaDao.listar(zona);
    }

    @Transactional
    @Override
    public void guardar(DetalleZona detalleZona) {
        detalleZonaDao.save(detalleZona);
    }

    @Transactional
    @Modifying
    @Override
    public void eliminar(DetalleZonaPK detalleZonaPK) {
        
        detalleZonaDao.delete(new DetalleZona(detalleZonaPK));
    }

    @Transactional
    @Override
    public void eliminarManzana(DetalleZona detalleZona) {
        detalleZonaDao.eliminarManzana(detalleZona);
    }
    
    @Transactional(readOnly = true)
    @Override
    public DetalleZona encontrar(DetalleZona detalleZona) {
        return detalleZonaDao.encontrar(detalleZona.getZona(), detalleZona.getCobrador());
    }


    @Transactional
    @Override
    public void modificar(DetalleZonaPK detalleZonaPK,Cobrador cobradorNuevo) {
        detalleZonaDao.modificar(detalleZonaPK, cobradorNuevo);
    }

}
