 
package ama.servicios.implementaciones;
 
import ama.dao.ComisionDao;
import ama.dominio.Comision;
import ama.servicio.ServicioComision;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplComision implements ServicioComision{

    @Autowired
    ComisionDao comisionDao;
    
    @Override
    public List<Comision> listar() {
        return (List<Comision>) comisionDao.findAll();
        
    }

    @Override
    public void guardar(Comision comision) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void eliminar(Comision comision) {
        comisionDao.delete(comision);
    }

    @Override
    public void encontrar(Comision comision) {
       comisionDao.findById(comision.getCodigoComision()).orElse(null);
    }
    
}
