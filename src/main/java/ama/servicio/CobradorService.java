 
package ama.servicio;
 
import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CobradorService {
    
    public List<Cobrador> listar(Sucursal sucursal);

    public Page<Cobrador> listarPorSucursal(Pageable pageable, Integer codigoSucursal);

    public Page<Cobrador> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro);

    public long contarPorSucursal(Integer codigoSucursal);
    
    public List<Cobrador> listarIsEstadoActivo(Sucursal sucursal);
    
    public void guardar(Cobrador cobrador);
    
    public void eliminar(Cobrador cobrador);
    
    public Cobrador encontrar(Cobrador cobrador);
    
    public Integer getCodigoCobrador();
}
