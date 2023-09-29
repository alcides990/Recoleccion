 
package ama.servicios.implementaciones;

import ama.dao.TipoFacturaDao;
import ama.dominio.TipoFactura;
import ama.servicio.ServicioTipoFactura;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

 @Service
public class ImplTipoFactura implements ServicioTipoFactura {

    @Autowired
    TipoFacturaDao tipoFacturaDao;
    @Override
    public List<TipoFactura> listar() {
       return (List<TipoFactura>) tipoFacturaDao.findAll();
    }

    @Override
    public void guardar(TipoFactura tipoFactura) {
       tipoFacturaDao.save(tipoFactura);
    }

    @Override
    public void eliminar(TipoFactura tipoFactura) {
        tipoFacturaDao.delete(tipoFactura);
    }
   @Override
        public TipoFactura encontrar(TipoFactura tipoFactura) {
       return tipoFacturaDao.findById(tipoFactura.getCodigoTipoFactura()).orElse(null);
    }
    
}
