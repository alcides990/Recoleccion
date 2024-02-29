 
package ama.servicios.implementaciones;
 
import ama.dao.CondicionVentaDao;
import ama.dominio.CondicionVenta;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.CondicionVentaService;

@Service
public class ImplCondicionVenta implements CondicionVentaService{

    @Autowired
    CondicionVentaDao condicionVentaDao;
    
    @Override
    public List<CondicionVenta> listar() {
        return (List<CondicionVenta>) condicionVentaDao.findAll();
    }

    @Override
    public void guardar(CondicionVenta condicioVenta) {
       condicionVentaDao.save(condicioVenta);
    }

    @Override
    public void eliminar(CondicionVenta condicioVenta) {
       condicionVentaDao.delete(condicioVenta);
    }

    @Override
    public void encontrar(CondicionVenta condicioVenta) {
       condicionVentaDao.findById(condicioVenta.getCodigoCondicionVenta()).orElse(null);
    }
    
}
