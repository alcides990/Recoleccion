package ama.servicio;

import ama.dominio.Cobrador;
import ama.dominio.DetalleZona;
import ama.dominio.DetalleZonaPK;
import ama.dominio.Zona;
import java.util.List;

public interface ServicioDetalleZona {

    public List<DetalleZona> listar(Zona zona);


    public void guardar(DetalleZona detalleZona);

    public void eliminar(DetalleZonaPK detalleZonaPK);

    public void eliminarManzana(DetalleZona detalleZona);

    public DetalleZona encontrar(DetalleZona detalleZona);
    
    public void modificar(DetalleZonaPK detalleZonaPK, Cobrador cobradorNuevo);
}
