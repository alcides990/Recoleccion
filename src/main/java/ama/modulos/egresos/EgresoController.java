package ama.modulos.egresos;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/egresos")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR','SECRETARIO')")
public class EgresoController {
    public record Guardado(long id) {}
    public record Mensaje(String mensaje) {}

    private final EgresoService gastos;
    private final CatalogoService catalogos;
    private final EgresoSesion sesion;

    public EgresoController(EgresoService gastos, CatalogoService catalogos, EgresoSesion sesion) {
        this.gastos = gastos;
        this.catalogos = catalogos;
        this.sesion = sesion;
    }

    @GetMapping
    public String pagina(Model modelo) {
        sesion.actual();
        modelo.addAttribute("titulo", "Egresos y gastos");
        return "egresos/inicio";
    }

    @GetMapping(value = "/catalogos/{clase}", produces = "application/json")
    @ResponseBody
    public List<CatalogoDto> catalogos(@PathVariable ClaseCatalogo clase, @RequestParam(defaultValue = "") String q) {
        return catalogos.buscar(sesion.actual().sucursal(), clase, q);
    }

    @PostMapping(value = "/catalogos/{clase}", produces = "application/json")
    @ResponseBody
    public Guardado catalogo(@PathVariable ClaseCatalogo clase, @RequestBody CatalogoDto dato) {
        return new Guardado(catalogos.guardar(sesion.actual().sucursal(), clase, dato));
    }

    @GetMapping(value = "/datos", produces = "application/json")
    @ResponseBody
    public List<EgresoDto> listar(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(required = false) Long categoriaId) {
        return gastos.listar(sesion.actual().sucursal(), desde, hasta, categoriaId);
    }

    @GetMapping(value = "/{id}/datos", produces = "application/json")
    @ResponseBody
    public EgresoDto detalle(@PathVariable long id) {
        return gastos.detalle(sesion.actual().sucursal(), id);
    }

    @PostMapping(value = "/guardar", produces = "application/json")
    @ResponseBody
    public Guardado guardar(@RequestParam(required = false) Long id, @RequestBody EgresoSolicitud dato) {
        var contexto = sesion.actual();
        return new Guardado(gastos.guardar(contexto.sucursal(), contexto.usuario(), id, dato));
    }

    @PostMapping(value = "/{id}/anular", produces = "application/json")
    @ResponseBody
    public Mensaje anular(@PathVariable long id) {
        gastos.anular(sesion.actual().sucursal(), id);
        return new Mensaje("Gasto anulado.");
    }
    @GetMapping({"/agregar", "/editar/{id}", "/catalogos/{clase}/lista",
            "/catalogos/{clase}/agregar", "/catalogos/{clase}/editar/{id}"})
    public String pantalla(Model modelo) {
        return pagina(modelo);
    }

    @GetMapping(value = "/catalogos/{clase}/{id}/datos", produces = "application/json")
    @ResponseBody
    public CatalogoDto detalleCatalogo(@PathVariable ClaseCatalogo clase, @PathVariable long id) {
        return catalogos.encontrar(sesion.actual().sucursal(), clase, id);
    }

    @PostMapping(value = "/catalogos/{clase}/{id}/eliminar", produces = "application/json")
    @ResponseBody
    public Mensaje eliminarCatalogo(@PathVariable ClaseCatalogo clase, @PathVariable long id) {
        catalogos.eliminar(sesion.actual().sucursal(), clase, id);
        return new Mensaje("Registro eliminado.");
    }
}
