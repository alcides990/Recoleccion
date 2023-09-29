 
package ama.servicio;
 
import ama.dominio.TipoFactura;
import java.util.List;
public interface ServicioTipoFactura {
    
    public List<TipoFactura> listar();
    
    public void guardar(TipoFactura tipoFactura);
    
    public void eliminar(TipoFactura tipoFactura);
    
    public TipoFactura encontrar(TipoFactura tipoFactura);
    
}
