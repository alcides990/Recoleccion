 
package ama.servicios.implementaciones;
 
import ama.dao.EmpresaDao;
import ama.dominio.Empresa;
import ama.servicio.ServicioEmpresa;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImplEmpresa implements ServicioEmpresa{

    @Autowired
    EmpresaDao empresaDao;
    
    @Override
    public List<Empresa> listar() {
        return (List<Empresa>) empresaDao.findAll();
    }

    @Override
    public void guardar(Empresa empresa) {
       empresaDao.save(empresa);
    }

    @Override
    public void eliminar(Empresa empresa) {
       empresaDao.delete(empresa);
    }

    @Override
    public void encontrar(Empresa empresa) {
       empresaDao.findById(empresa.getCodigoEmpresa()).orElse(null);
    }
    
}
