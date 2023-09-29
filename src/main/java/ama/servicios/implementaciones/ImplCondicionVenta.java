 
package ama.servicios.implementaciones;
 
import ama.dao.CondicionVentaDao;
import ama.dominio.Condicionventa;
import ama.servicio.ServicioCondicionVenta;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplCondicionVenta implements ServicioCondicionVenta{

    @Autowired
    CondicionVentaDao condicionVentaDao;
    
    @Override
    public List<Condicionventa> listar() {
        return (List<Condicionventa>) condicionVentaDao.findAll();
    }

    @Override
    public void guardar(Condicionventa condicioVenta) {
       condicionVentaDao.save(condicioVenta);
    }

    @Override
    public void eliminar(Condicionventa condicioVenta) {
       condicionVentaDao.delete(condicioVenta);
    }

    @Override
    public void encontrar(Condicionventa condicioVenta) {
       condicionVentaDao.findById(condicioVenta.getCodigoCondicionVenta()).orElse(null);
    }
    
}
