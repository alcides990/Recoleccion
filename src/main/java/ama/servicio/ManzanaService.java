package ama.servicio;

import ama.dominio.Manzana;
import ama.dominio.ManzanaPK;
import ama.dominio.Zona;
import java.util.List;

public interface ManzanaService {

    public List<Manzana> listar(Zona zona);


    public void guardar(Manzana manzana);

    public void eliminar(Manzana manzana);

    public Manzana encontrar(ManzanaPK manzanaPK);
    
    public void modificar(Manzana manzana);
}
