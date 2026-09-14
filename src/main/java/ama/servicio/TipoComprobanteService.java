 
package ama.servicio;
 
import ama.dominio.TipoComprobante;
import java.util.List;
public interface TipoComprobanteService {
    
    public List<TipoComprobante> listar();
    
    public void guardar(TipoComprobante tipoFactura);
    
    public void eliminar(TipoComprobante tipoFactura);
    
    public TipoComprobante encontrar(TipoComprobante tipoFactura);
    
        public Integer getCodigoTipoComprobante();
    
}
