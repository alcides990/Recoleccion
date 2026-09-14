package ama.controladorMVC;

import ama.dominio.DetalleUsuarioSistema;
import ama.dominio.DetalleUsuarioSistemaPK;
import ama.dominio.Rol;
import ama.dominio.UsuarioSistema;
import ama.servicio.RolService;
import ama.servicio.UsuarioSistemaService;
import ama.validador.Mayuscula;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ama.servicio.DetalleUsuarioSistemaService;
import ama.servicio.AuditoriaEntidadService;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Controller
@RequestMapping("/detalleUsuarioSistema")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
public class DetalleUsuarioSistemaController {

    @Autowired
    private DetalleUsuarioSistemaService servicioDetalleUsuarioSistema;
    
    @Autowired
    private UsuarioSistemaService usuarioSistemaService;

    @Autowired
    private RolService rolService;

    @Autowired
    private AuditoriaEntidadService auditoriaEntidad;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

   

    @GetMapping("/agregar/{codigoUsuarioSistema}")
    public String asignarRol(@PathVariable int codigoUsuarioSistema, Model model) {
        UsuarioSistema usuarioSistema = usuarioSistemaService.findById(codigoUsuarioSistema)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        model.addAttribute("usuarioSistema", usuarioSistema);

        List<DetalleUsuarioSistema> detalles = servicioDetalleUsuarioSistema.listar(usuarioSistema);
        Set<Integer> codigosAsignados = detalles.stream()
                .map(detalle -> detalle.getDetalleUsuarioSistemaPK().getCodigoRol())
                .collect(Collectors.toSet());
        List<Rol> rolesDisponibles = rolService.listar().stream()
                .filter(rol -> !codigosAsignados.contains(rol.getCodigoRol()))
                .toList();
        model.addAttribute("roles", rolesDisponibles);
        model.addAttribute("detallesUsuarioSistema", detalles);

        return "usuarioSistema/agregarDetalleUsuarioSistema";
    }

    @PostMapping("/guardar")
    @Transactional
    public String guardar(
            @RequestParam int codigoUsuarioSistema,
            @RequestParam("rol") int codigoRol,
            RedirectAttributes flash) {
        String redireccion = "redirect:/detalleUsuarioSistema/agregar/" + codigoUsuarioSistema;
        if (usuarioSistemaService.findById(codigoUsuarioSistema).isEmpty()) {
            flash.addFlashAttribute("error", "El usuario indicado no existe.");
            return redireccion;
        }
        Rol rol = rolService.encontrar(new Rol(codigoRol));
        if (rol == null) {
            flash.addFlashAttribute("error", "El rol seleccionado no existe.");
            return redireccion;
        }
        DetalleUsuarioSistemaPK clave = new DetalleUsuarioSistemaPK(codigoUsuarioSistema, codigoRol);
        if (servicioDetalleUsuarioSistema.buscar(clave).isPresent()) {
            flash.addFlashAttribute("error", "El usuario ya tiene asignado ese rol.");
            return redireccion;
        }
        Map<String, Object> datosAntes = auditoriaEntidad.usuarioSistema(codigoUsuarioSistema);
        DetalleUsuarioSistema detalle = new DetalleUsuarioSistema(clave);
        detalle.setUsuarioSistema(new UsuarioSistema(codigoUsuarioSistema));
        detalle.setRol(rol);
        servicioDetalleUsuarioSistema.guardar(detalle);
        auditoriaEntidad.registrar("USUARIO_SISTEMA", "MODIFICACION",
                String.valueOf(codigoUsuarioSistema), datosAntes,
                auditoriaEntidad.usuarioSistema(codigoUsuarioSistema),
                "Asignación de rol");
        flash.addFlashAttribute("mensaje", "Rol asignado correctamente.");
        return redireccion;
    }

   
    @GetMapping("/editar/{codigoUsuarioSistema}")
    public String editar(UsuarioSistema usuarioSistema, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        if (usuarioSistema == null) {
            throw new Error("Usuario no encontrado !!");
        }
        model.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @PostMapping("/eliminar/{codigoUsuarioSistema}/{codigoRol}")
    @Transactional
    public ResponseEntity<?> modal(
            @PathVariable int codigoUsuarioSistema,
            @PathVariable int codigoRol) {
        DetalleUsuarioSistemaPK clave =
                new DetalleUsuarioSistemaPK(codigoUsuarioSistema, codigoRol);
        if (servicioDetalleUsuarioSistema.buscar(clave).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("El rol indicado ya no está asignado al usuario.");
        }
        Map<String, Object> datosAntes = auditoriaEntidad.usuarioSistema(codigoUsuarioSistema);
        servicioDetalleUsuarioSistema.eliminar(clave);
        auditoriaEntidad.registrar("USUARIO_SISTEMA", "MODIFICACION",
                String.valueOf(codigoUsuarioSistema), datosAntes,
                auditoriaEntidad.usuarioSistema(codigoUsuarioSistema),
                "Eliminación de rol");
        return ResponseEntity.ok("Rol eliminado correctamente.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> manejarErrorOperacion(Exception excepcion) {
        log.error("No se pudo completar la operación de roles", excepcion);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("No fue posible completar la operación del rol.");
    }
}
