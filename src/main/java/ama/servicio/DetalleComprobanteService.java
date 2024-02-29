package ama.servicio;

import ama.dominio.DetalleComprobante;
import java.util.List;

public interface DetalleComprobanteService {

    public List<DetalleComprobante> listar();

    public void guardar(DetalleComprobante detalleComprobante);

    public void eliminar(DetalleComprobante detalleComprobante);

    public List<DetalleComprobante> encontrar(DetalleComprobante detalleComprobante);

    
   
}
