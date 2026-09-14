package ama.modulos.comprobantesv2;

import ama.dominio.Comprobante;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

public final class ComprobanteSpecificationsV2 {
    private ComprobanteSpecificationsV2() { }
    public static Specification<Comprobante> filtrar(ComprobanteFiltroV2 f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("comprobantePK").get("puntoExpedicionPK").get("codigoSucursal"), f.getSucursal()));
            if (f.getPuntoExpedicion() != null) p.add(cb.equal(root.get("comprobantePK").get("puntoExpedicionPK").get("codigoPuntoExpedicion"), f.getPuntoExpedicion()));
            if (f.getSerie() != null) p.add(cb.equal(root.get("comprobantePK").get("codigoSerie"), f.getSerie()));
            if (f.getNumero() != null) p.add(cb.equal(root.get("comprobantePK").get("numeroComprobante"), f.getNumero()));
            if (f.getEstado() != null) p.add(cb.equal(root.get("estado").get("codigoEstado"), f.getEstado()));
            like(cb, p, root.get("servicio").get("cuentaCorriente"), f.getCuenta());
            like(cb, p, root.get("usuario").get("numeroDocumento"), f.getDocumento());
            if (f.getNombre() != null && !f.getNombre().isBlank()) {
                String v = "%" + f.getNombre().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("razonSocial")), v), cb.like(cb.lower(root.get("usuario").get("nombre")), v), cb.like(cb.lower(root.get("usuario").get("apellido")), v)));
            }
            LocalDate desde = f.getDesde(), hasta = f.getHasta();
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fechaPago"), desde));
            if (hasta != null) p.add(cb.lessThanOrEqualTo(root.get("fechaPago"), hasta));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
    private static void like(jakarta.persistence.criteria.CriteriaBuilder cb, List<Predicate> p, jakarta.persistence.criteria.Path<String> path, String value) {
        if (value != null && !value.isBlank()) p.add(cb.like(cb.lower(path), "%" + value.trim().toLowerCase() + "%"));
    }
}
