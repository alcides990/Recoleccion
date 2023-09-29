 
package ama.servicio;
 
import ama.dominio.TipoDocumento;
import java.util.List;
public interface ServicioTipoDocumento {
    
    public List<TipoDocumento> listar();
    
    public void guardar(TipoDocumento tipoDocumento);
    
    public void eliminar(TipoDocumento tipoDocumento);
    
    public TipoDocumento encontrar(TipoDocumento tipoDocumento);
    
}
