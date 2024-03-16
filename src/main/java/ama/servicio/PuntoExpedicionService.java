
package ama.servicio;

import ama.dominio.PuntoExpedicion;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Sucursal;
import java.util.List;

public interface PuntoExpedicionService {

    public List<PuntoExpedicion> listar();

    public List<PuntoExpedicion> listar(Sucursal sucursal);

    public List<PuntoExpedicion> isMayorCero(Sucursal sucursal);

    public void guardar(PuntoExpedicion puntoExpedicion);

    public void eliminar(PuntoExpedicionPK puntoExpedicionPK);

    public PuntoExpedicion encontrar(PuntoExpedicionPK puntoExpedicionPK);

    public Integer getCodigoPuntoExpedicion(Sucursal sucursal);
}
