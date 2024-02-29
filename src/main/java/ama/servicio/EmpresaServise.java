 
package ama.servicio;
 
import ama.dominio.Empresa;
import java.util.List;

public interface EmpresaServise {
    
    public List<Empresa> listar();
    
    public void guardar(Empresa empresa);
    
    public void eliminar(Empresa empresa);
    
    public Empresa encontrar(Empresa empresa);
}
