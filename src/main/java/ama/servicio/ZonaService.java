 
package ama.servicio;
 
import ama.dominio.Zona;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ZonaService {
    
    public List<Zona> listar();

    public Page<Zona> listarPorSucursal(Pageable pageable, Integer codigoSucursal);

    public Page<Zona> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro);

    public long contarPorSucursal(Integer codigoSucursal);
    
    public void guardar(Zona zona);
    
    public void eliminar(Zona zona);
    
    public Zona encontrar(Zona zona);
    
    public Integer getCodigoZona();
}
