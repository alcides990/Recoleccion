
package ama.dao;

import ama.dominio.DetalleComprobante;
import ama.dominio.DetalleComprobantePK;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleComprobanteDao extends JpaRepository<DetalleComprobante, DetalleComprobantePK> {

}
