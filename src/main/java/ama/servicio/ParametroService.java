
package ama.servicio;

import ama.dominio.Parametro;
import ama.dominio.Sucursal;
import java.util.List;

 
public interface ParametroService {
    
    public List<Parametro> listar();
    

    public Parametro guardar(Parametro parametro);

    public void eliminar(Parametro parametro);

    public Parametro encontrar(Sucursal sucursal );
    
    public Integer generarCodigo();
}
