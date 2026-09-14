package ama.modulos.egresos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ama.modulos.egresos.EgresoException.datosInvalidos;

@Service
public class EgresoService {
    private final EgresoRepository repositorio;
    private final CatalogoService catalogos;

    public EgresoService(EgresoRepository repositorio, CatalogoService catalogos) {
        this.repositorio = repositorio;
        this.catalogos = catalogos;
    }

    @Transactional
    public long guardar(int sucursal, int usuario, Long id, EgresoSolicitud solicitud) {
        EgresoSolicitud dato = EgresoValidacion.normalizar(solicitud);
        BigDecimal total = EgresoValidacion.total(dato);
        catalogos.validarTipo(sucursal, dato.tipoId());
        if (id != null) validarEdicion(sucursal, id);

        long proveedor = catalogos.resolverNombre(sucursal, ClaseCatalogo.PROVEEDOR, dato.proveedor());
        long gasto = guardarCabecera(sucursal, usuario, id, proveedor, dato, total);
        guardarDetalles(sucursal, gasto, dato.detalles());
        return gasto;
    }

    @Transactional(readOnly = true)
    public List<EgresoDto> listar(int sucursal, LocalDate desde, LocalDate hasta) {
        return listar(sucursal, desde, hasta, null);
    }

    @Transactional(readOnly = true)
    public List<EgresoDto> listar(int sucursal, LocalDate desde, LocalDate hasta, Long categoriaId) {
        EgresoValidacion.rango(desde, hasta);
        return repositorio.listar(sucursal, desde, hasta, categoriaId);
    }

    @Transactional(readOnly = true)
    public EgresoDto detalle(int sucursal, long id) {
        EgresoDto gasto = repositorio.encontrar(sucursal, id).orElseThrow(() ->
                new EgresoException(EgresoException.Motivo.NO_ENCONTRADO, "Gasto no encontrado."));
        return gasto.conDetalles(repositorio.detalles(sucursal, id));
    }

    @Transactional
    public void anular(int sucursal, long id) {
        if (!repositorio.anular(sucursal, id)) throw datosInvalidos("Gasto no encontrado o ya anulado.");
    }

    private void validarEdicion(int sucursal, long id) {
        boolean anulado = repositorio.bloquearEstado(sucursal, id)
                .orElseThrow(() -> datosInvalidos("Gasto no encontrado o anulado."));
        if (anulado) throw datosInvalidos("Gasto no encontrado o anulado.");
    }

    private long guardarCabecera(int sucursal, int usuario, Long id, long proveedor,
            EgresoSolicitud dato, BigDecimal total) {
        if (id == null) return repositorio.crear(sucursal, usuario, proveedor, dato, total);
        repositorio.actualizar(sucursal, id, proveedor, dato, total);
        repositorio.borrarDetalles(sucursal, id);
        return id;
    }

    private void guardarDetalles(int sucursal, long gasto, List<LineaEgreso> detalles) {
        for (LineaEgreso linea : detalles) {
            long producto = catalogos.resolverNombre(sucursal, ClaseCatalogo.PRODUCTO, linea.producto());
            repositorio.agregarDetalle(sucursal, gasto, producto, linea, EgresoValidacion.subtotal(linea));
            catalogos.actualizarMonto(sucursal, producto, linea.precio());
        }
    }
}
