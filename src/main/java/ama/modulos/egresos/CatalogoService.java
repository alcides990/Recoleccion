package ama.modulos.egresos;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ama.modulos.egresos.EgresoException.datosInvalidos;
import static ama.modulos.egresos.EgresoValidacion.texto;

@Service
public class CatalogoService {
    private final CatalogoRepository repositorio;

    public CatalogoService(CatalogoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<CatalogoDto> buscar(int sucursal, ClaseCatalogo clase, String busqueda) {
        return repositorio.buscar(sucursal, clase, texto(busqueda, 180, false));
    }

    public void validarTipo(int sucursal, long id) {
        if (!repositorio.existe(sucursal, ClaseCatalogo.TIPO, id)) {
            throw datosInvalidos("Tipo de gasto no válido.");
        }
    }

    @Transactional
    public long guardar(int sucursal, ClaseCatalogo clase, CatalogoDto dato) {
        if (dato == null) throw datosInvalidos("Catálogo no válido.");
        Long categoriaId = clase == ClaseCatalogo.PRODUCTO ? dato.categoriaId() : null;
        if (!repositorio.categoriaProductoValida(sucursal, categoriaId)) {
            throw datosInvalidos("Categoría de producto no válida.");
        }
        CatalogoDto limpio = new CatalogoDto(dato.id(), texto(dato.nombre(), 180, true),
                texto(dato.documento(), 40, false), texto(dato.telefono(), 60, false),
                texto(dato.direccion(), 250, false), texto(dato.correo(), 180, false),
                validarMonto(dato.monto()), categoriaId, null);
        if (limpio.id() == null) return repositorio.crearOEncontrar(sucursal, clase, limpio);
        if (!repositorio.existe(sucursal, clase, limpio.id())) throw datosInvalidos("Registro no encontrado.");
        repositorio.actualizar(sucursal, clase, limpio);
        return limpio.id();
    }

    @Transactional
    public long resolverNombre(int sucursal, ClaseCatalogo clase, String nombre) {
        return guardar(sucursal, clase, CatalogoDto.nuevo(nombre));
    }
    public CatalogoDto encontrar(int sucursal, ClaseCatalogo clase, long id) {
        return repositorio.encontrar(sucursal, clase, id);
    }

    @Transactional
    public void actualizarMonto(int sucursal, long producto, java.math.BigDecimal monto) {
        repositorio.actualizarMonto(sucursal, producto, validarMonto(monto));
    }

    @Transactional
    public void eliminar(int sucursal, ClaseCatalogo clase, long id) {
        if (repositorio.eliminar(sucursal, clase, id) == 0) throw datosInvalidos("Registro no encontrado.");
    }

    private java.math.BigDecimal validarMonto(java.math.BigDecimal monto) {
        if (monto == null) return java.math.BigDecimal.ZERO;
        if (monto.signum() < 0 || monto.scale() > 2
                || monto.compareTo(new java.math.BigDecimal("9999999999999999.99")) > 0) {
            throw datosInvalidos("Monto no válido.");
        }
        return monto;
    }
}
