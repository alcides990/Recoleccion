 
package ama.servicio;
 
import ama.dominio.PuntoExpedicion;
import java.util.List;

public interface PuntoExpedicionService {
    
    public List<PuntoExpedicion> listar();
    
    public void guardar(PuntoExpedicion puntoExpedicion);
    
    public void eliminar(PuntoExpedicion puntoExpedicion);
    
    public PuntoExpedicion encontrar(PuntoExpedicion puntoExpedicion);
    
    public Integer getCodigoPuntoExpedicion();
}
