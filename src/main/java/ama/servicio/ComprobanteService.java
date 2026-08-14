package ama.servicio;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Servicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ComprobanteService {

    public Page<Comprobante> listar(Pageable pageable);

    public Page<Comprobante> filtrar(Pageable pageable, ComprobantePK comprobantePK);

    public Comprobante guardar(Comprobante comprobante);

    public void anular(Comprobante comprobante);

    public Optional<Comprobante> findById(ComprobantePK comprobantePK);
    
    public Comprobante getComprobante(ComprobantePK comprobantePK);

    public Integer getNumeroComprobante(ComprobantePK comprobantePK);

    public int getCantidadComprobante(String codigoUsuario);

    public Page<Comprobante> getComprobantesCuenta(Pageable page, Servicio servicio);

    public int getCantidadPago(String cuentaCorriente);
    
    public Optional<String> getPagoDesde(String cuentaCorriente);

    public Optional<Comprobante> getUltimoComprobanteCuentaActivo(String cuentaCorriente);

    public List<Object[]> getPagoDesdeAndSaldo(String cuentaCorriente);

}
