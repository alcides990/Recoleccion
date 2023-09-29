 
package ama.dao;
 
import ama.dominio.DetalleComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DetalleComprobanteDao extends JpaRepository<DetalleComprobante, Integer>{
    
    
    @Query("SELECT SUM(cantidadPago) FROM DetalleComprobante dtc  "
            + "WHERE dtc.comprobante.servicio.cuentaCorriente= ?1 GROUP BY dtc.detalleComprobantePK.codigoMetodoPago")
    Integer getCantidadPago(String cuentaCorriente);
}
