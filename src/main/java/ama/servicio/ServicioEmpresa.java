 
package ama.servicio;
 
import ama.dominio.Empresa;
import java.util.List;

public interface ServicioEmpresa {
    
    public List<Empresa> listar();
    
    public void guardar(Empresa empresa);
    
    public void eliminar(Empresa empresa);
    
    public void encontrar(Empresa empresa);
}
