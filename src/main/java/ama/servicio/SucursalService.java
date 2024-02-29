 
package ama.servicio;

import ama.dominio.Sucursal;
import java.util.List;


public interface SucursalService {
     public List<Sucursal> listar();
    
    public Sucursal guardar(Sucursal sucursal);
    
    public void eliminar(Sucursal sucursal);
    
    public Sucursal encontrar(Sucursal sucursal);
    
    public Integer getCodigoSucursal();
}
