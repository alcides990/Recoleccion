 
package ama.servicio;
 
import ama.dominio.Categoria;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoriaService {
    
    public List<Categoria> listar();
    
    public List<Categoria> listar(Sucursal sucursal);
    
    public Page<Categoria> listar(Pageable pageable, Sucursal sucursal);

    public Page<Categoria> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro);

    public long contarPorSucursal(Integer codigoSucursal);
    
    public Categoria guardar(Categoria categoria);
    
    public void eliminar(Categoria categoria);
    
    public Categoria encontrar(Categoria categoria);
    
    public Integer getCodigoCategoria();
}
