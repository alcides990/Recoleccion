 
package ama.servicio;
 
import ama.dominio.MetodoPago;
import java.util.List;

public interface MetodoPagoService {
    
    public List<MetodoPago> listar();
    
    public void guardar(MetodoPago metodoPago);
    
    public void eliminar(MetodoPago metodoPago);
    
    public MetodoPago encontrar(MetodoPago metodoPago);
    
    public Integer getCodigoMetodoPago();
}
