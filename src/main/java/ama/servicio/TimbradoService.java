 
package ama.servicio;
 
import ama.dominio.Timbrado;
import java.util.List;

public interface TimbradoService {
    
    public List<Timbrado> listar();
    
    public Timbrado guardar(Timbrado timbrado);
    
    public Timbrado encontrar(Timbrado timbrado);
    
    public void eliminar(Timbrado timbrado);
    
    public Integer getCodigoTimbrado();
}
