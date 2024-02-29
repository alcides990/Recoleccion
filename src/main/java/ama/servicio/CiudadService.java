
package ama.servicio;

import ama.dominio.Ciudad;
import java.util.List;

 
public interface CiudadService {
    
    public List<Ciudad> listarCiudad();
    

    public Ciudad guardar(Ciudad ciudad);

    public void eliminar(Ciudad ciudad);

    public Ciudad encontrarCiudad(Ciudad ciudad);
    
    public Integer getCodigoCiudad();
}
