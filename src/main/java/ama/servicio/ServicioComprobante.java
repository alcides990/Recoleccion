package ama.servicio;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicioComprobante {

    public Page<Comprobante> listar(Pageable pageable);
    
    public Page<Comprobante> filtrar(Pageable pageable, ComprobantePK comprobantePK);

    public Comprobante guardar(Comprobante comprobante);

    public void anular(Comprobante comprobante);

    public Comprobante getComprobante(ComprobantePK comprobantePK);
    
    public Integer getNumeroComprobante(ComprobantePK comprobantePK);

    public int getCantidadComprobante(String codigoUsuario);
    
    public Page<Comprobante> getComprobantesCuenta(Pageable page, Servicio servicio);
    
}
