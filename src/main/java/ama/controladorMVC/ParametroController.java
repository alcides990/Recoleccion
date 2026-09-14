package ama.controladorMVC;

import ama.dominio.Comision;
import ama.dominio.Empresa;
import ama.dominio.Parametro;
import ama.dominio.UsuarioSistema;
import ama.dominio.Sucursal;
import ama.servicio.ParametroService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import ama.servicio.ComisionService;
import ama.servicio.EmpresaServise;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("/parametro")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
public class ParametroController {

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private ParametroService parametroService;
    @Autowired
    private SucursalService sucursalService;
    @Autowired
    private CiudadService ciudadService;
    @Autowired
    private EmpresaServise empresaServise;
    @Autowired
    private ComisionService comisionService;

    @GetMapping("/listar")
    public String listaCategoria(
            @RequestParam(required = false) Integer codigoSucursal,
            Model modelo) {
        modelo.addAttribute("titulo", "Parametro");
        Sucursal sucursal = resolverSucursal(codigoSucursal);
        modelo.addAttribute("empresa", empresaServise.encontrar(sucursal.getEmpresa()));
        Optional<Parametro> encontrado = parametroService.buscar(sucursal);
        Parametro parametro = encontrado.orElseGet(() -> Parametro.builder()
                .cierrePeriodo(LocalDate.now()).recargoMora(0)
                .sucursal(sucursal).empresa(sucursal.getEmpresa()).build());
        modelo.addAttribute("parametro", parametro);
        modelo.addAttribute("existeParametro", encontrado.isPresent());

        List<Comision> comisiones = comisionService.listar();
        modelo.addAttribute("comisiones", comisiones);

        modelo.addAttribute("sucursal", sucursal);
        modelo.addAttribute("esRoot", esRoot());
        if (esRoot()) {
            modelo.addAttribute("sucursales", sucursalService.listar());
        }
        return "parametro";
    }
    @Transactional
    @PostMapping("/guardar")
    public String guardar(Empresa empresa, Parametro parametro,
            @RequestParam(required = false) Integer codigoSucursal,
            RedirectAttributes flash) {
        Sucursal sucursal = resolverSucursal(codigoSucursal);
        if (parametro.getCierrePeriodo() == null) {
            flash.addFlashAttribute("error", "Indique la fecha de cierre del período.");
            return redireccion(sucursal, flash);
        }
        if(parametro.getCierrePeriodo().isAfter(LocalDate.now())){
            flash.addFlashAttribute("error", "Fecha cierre periodo no puede ser mayor a la fecha de hoy!!");
            return redireccion(sucursal, flash);
        }
        if (parametro.getComision() == null
                || parametro.getComision().getCodigoComision() == null) {
            flash.addFlashAttribute("error", "Seleccione una comisión válida.");
            return redireccion(sucursal, flash);
        }
        Optional<Parametro> parametroExistente = parametroService.buscar(sucursal);
        boolean esNuevo = parametroExistente.isEmpty();
        Parametro existente = parametroExistente.orElseGet(() -> {
            Parametro nuevo = new Parametro();
            nuevo.setCodigoParametro(parametroService.generarCodigo());
            return nuevo;
        });
        Comision comision = comisionService.encontrar(parametro.getComision());
        if (comision == null) {
            flash.addFlashAttribute("error", "La comisión seleccionada no existe.");
            return redireccion(sucursal, flash);
        }
        Empresa empresaAutorizada = empresaServise.encontrar(sucursal.getEmpresa());
        empresaAutorizada.setRuc(empresa.getRuc());
        empresaAutorizada.setRazonSocial(empresa.getRazonSocial());
        existente.setEmpresa(empresaAutorizada);
        existente.setSucursal(sucursal);
        existente.setCierrePeriodo(parametro.getCierrePeriodo());
        existente.setRecargoMora(parametro.getRecargoMora());
        existente.setComision(comision);
        parametroService.guardar(existente);
        flash.addFlashAttribute("mensaje", esNuevo
                ? "Parámetros creados correctamente."
                : "Parámetros guardados correctamente.");
        return redireccion(sucursal, flash);
    }

    @Transactional
    @PostMapping("/eliminar")
    public String eliminar(@RequestParam(required = false) Integer codigoSucursal,
            RedirectAttributes flash) {
        Sucursal sucursal = resolverSucursal(codigoSucursal);
        Optional<Parametro> parametro = parametroService.buscar(sucursal);
        if (parametro.isEmpty()) {
            flash.addFlashAttribute("error", "La sucursal no tiene parámetros configurados.");
            return redireccion(sucursal, flash);
        }
        try {
            parametroService.eliminar(parametro.get());
            flash.addFlashAttribute("mensaje", "Parámetro eliminado correctamente.");
        } catch (DataIntegrityViolationException excepcion) {
            flash.addFlashAttribute("error",
                    "No se puede eliminar el parámetro porque está siendo utilizado.");
        }
        return redireccion(sucursal, flash);
    }

    private Sucursal resolverSucursal(Integer codigoSolicitado) {
        UsuarioSistema usuario = getUserSession();
        if (usuario == null || usuario.getSucursal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!esRoot()) return usuario.getSucursal();
        Integer codigo = codigoSolicitado == null
                ? usuario.getSucursal().getCodigoSucursal() : codigoSolicitado;
        Sucursal sucursal = sucursalService.encontrar(new Sucursal(codigo));
        if (sucursal == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada");
        }
        return sucursal;
    }

    private boolean esRoot() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> "ROOT".equals(autoridad.getAuthority()));
    }

    private String redireccion(Sucursal sucursal, RedirectAttributes flash) {
        if (esRoot()) flash.addAttribute("codigoSucursal", sucursal.getCodigoSucursal());
        return "redirect:/parametro/listar";
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
