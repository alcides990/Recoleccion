package ama.controladorMVC;

import ama.dao.DetalleUsuarioSistemaDao;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import ama.dominio.Estado;
import ama.dominio.Rol;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.RolService;
import ama.servicio.SucursalService;
import ama.servicio.UsuarioSistemaService;
import ama.servicio.AuditoriaEntidadService;
import ama.utilerias.PageRender;
import ama.validador.Mayuscula;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/usuarioSistema")
public class UsuarioSistemaController {

    @Autowired
    private UsuarioSistemaService usuarioSistemaService;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private RolService rolService;

    @Autowired
    private DetalleUsuarioSistemaDao detalleUsuarioSistemaDao;

    @Autowired
    private AuditoriaEntidadService auditoriaEntidad;

    @Autowired
    private SucursalService sucursalService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @GetMapping("/listar")
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
    public String listaUsuario(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantElemento", defaultValue = "10") int cantElemento,
            @RequestParam(name = "filtro", defaultValue = "") String filtro, Model modelo) {
        modelo.addAttribute("titulo", "UsuarioSistema");
        return "usuarioSistema/usuarioSistema";
    }

    @PostMapping("/tabla")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
    public DataTableResponseV2<Map<String, Object>> tabla(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "0") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "asc") String direccion) {
        int limite = Math.min(Math.max(length, 1), 100);
        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(start, 0) / limite, limite,
                Sort.by(sentido, ordenUsuarioSistema(columna)));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Page<UsuarioSistema> pagina = filtro.isBlank()
                ? usuarioSistemaService.findAll(pageable)
                : usuarioSistemaService.buscar(pageable, filtro);
        List<Map<String, Object>> filas = pagina.getContent().stream().map(usuario -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", usuario.getCodigoUsuarioSistema());
            fila.put("nombre", usuario.getNombre());
            fila.put("sucursal", usuario.getSucursal().getNombreSucursal()
                    + " - " + usuario.getSucursal().getCiudad().getNombreCiudad());
            fila.put("estado", usuario.getEstado().getEstado());
            return fila;
        }).toList();
        return new DataTableResponseV2<>(draw, usuarioSistemaService.contar(), pagina.getTotalElements(), filas);
    }

    private String ordenUsuarioSistema(int columna) {
        return switch (columna) {
            case 1 -> "nombre";
            case 2 -> "sucursal.nombreSucursal";
            case 3 -> "estado.estado";
            default -> "codigoUsuarioSistema";
        };
    }

    @GetMapping("/agregar")
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        UsuarioSistema usuarioSistema = new UsuarioSistema();
        usuarioSistema.setSucursal(getUserSession().getSucursal());
        modelo.addAttribute("usuarioSistema", usuarioSistema);
        cargarSucursalesParaFormulario(modelo);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
    @Transactional
    public String guardar(UsuarioSistema usuarioSistema, RedirectAttributes flash) {
        String mensaje = "Usuario modificado correctamente!!";
        UsuarioSistema userSession = (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
        boolean alta = usuarioSistema.getCodigoUsuarioSistema() == null;
        Map<String, Object> datosAntes = alta ? null
                : auditoriaEntidad.usuarioSistema(usuarioSistema.getCodigoUsuarioSistema());
        usuarioSistema.setEstado(new Estado(1));
        if (usuarioSistema.getCodigoUsuarioSistema() == null) {
            usuarioSistema.setSucursal(resolverSucursalAlta(usuarioSistema, userSession));
            Integer codigoUsuario = usuarioSistemaService.getCodigoUsuarioSistema() + 1;
            usuarioSistema.setCodigoUsuarioSistema(codigoUsuario);
            mensaje = "Usuario agregado correctamente";
            String clave = encoder.encode(usuarioSistema.getClave());
            usuarioSistema.setClave(clave);
        } else {
            UsuarioSistema usuarioActual = usuarioSistemaService
                    .findById(usuarioSistema.getCodigoUsuarioSistema())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
            usuarioSistema.setClave(usuarioActual.getClave());
            usuarioSistema.setSucursal(usuarioActual.getSucursal());
        }
        usuarioSistemaService.saveAndFlush(usuarioSistema);
        auditoriaEntidad.registrar("USUARIO_SISTEMA", alta ? "ALTA" : "MODIFICACION",
                String.valueOf(usuarioSistema.getCodigoUsuarioSistema()), datosAntes,
                auditoriaEntidad.usuarioSistema(usuarioSistema.getCodigoUsuarioSistema()), null);
        flash.addFlashAttribute("mensaje", mensaje);
        return "redirect:/usuarioSistema/listar";
    }

    @PreAuthorize("hasAnyAuthority({'ROOT','ADMINISTRADOR'})")
    @GetMapping("/roles/{codigoUsuarioSistema}")
    public String asignarRol(UsuarioSistema usuarioSistema, Model model) {
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        model.addAttribute("usuarioSistema", usuarioSistema);

        List<Rol> roles = rolService.listar();
        model.addAttribute("roles", roles);

        return "usuarioSistema/agregarDetalleUsuarioSistema";
    }

    @GetMapping("/editar/{codigoUsuarioSistema}")
    public String editar(UsuarioSistema usuarioSistemaRequest, Model model) {
        model.addAttribute("titulo", "Usuario");
        UsuarioSistema usuarioSistema = usuarioSistemaService.findById(usuarioSistemaRequest.getCodigoUsuarioSistema())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
        model.addAttribute("usuarioSistema", usuarioSistema);
        cargarSucursalesParaFormulario(model);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @GetMapping("/editarClave/{codigoUsuarioSistema}")
    public String editarClave(UsuarioSistema usuarioSistema, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
        model.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/editarClave";
    }

    @PostMapping("/editarClave")
    @Transactional
    public String modifgicarContraseña(UsuarioSistema usuario, RedirectAttributes flash, HttpServletRequest request, HttpServletResponse response) {
        UsuarioSistema usuarioSistema = usuarioSistemaService.findById(usuario.getCodigoUsuarioSistema())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
        Map<String, Object> datosAntes = auditoriaEntidad.usuarioSistema(
                usuarioSistema.getCodigoUsuarioSistema());
        String clave = encoder.encode(usuario.getClave());
        usuarioSistema.setClave(clave);
        usuarioSistemaService.saveAndFlush(usuarioSistema);
        auditoriaEntidad.registrar("USUARIO_SISTEMA", "MODIFICACION",
                String.valueOf(usuarioSistema.getCodigoUsuarioSistema()), datosAntes,
                auditoriaEntidad.usuarioSistema(usuarioSistema.getCodigoUsuarioSistema()),
                "Cambio de contraseña");
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        flash.addFlashAttribute("info", "Contraseña modificada corectamente!!");
        return "redirect:/login";

    }

    @PostMapping("/passwordReset/{codigoUsuarioSistema}")
    @Transactional
    public ResponseEntity<?> passwordReset(UsuarioSistema usuarioSistema) {
        try {
            usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
            if (usuarioSistema == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }
            Map<String, Object> datosAntes = auditoriaEntidad.usuarioSistema(
                    usuarioSistema.getCodigoUsuarioSistema());
            String clave = encoder.encode("1234");
            usuarioSistema.setClave(clave);
            usuarioSistema = usuarioSistemaService.saveAndFlush(usuarioSistema);
            auditoriaEntidad.registrar("USUARIO_SISTEMA", "MODIFICACION",
                    String.valueOf(usuarioSistema.getCodigoUsuarioSistema()), datosAntes,
                    auditoriaEntidad.usuarioSistema(usuarioSistema.getCodigoUsuarioSistema()),
                    "Restablecimiento de contraseña");

            return ResponseEntity.ok("Clave reseteada al 1234, inicie sesion y modifique su clave!!");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al realizar reseteo de Clave "+ e);
        }

    }

    @Transactional
    @PostMapping("/eliminar/{codigoUsuarioSistema}")
    public ResponseEntity<?> modal(UsuarioSistema usuarioSistema) {
        try {
            Map<String, Object> datosAntes = auditoriaEntidad.usuarioSistema(
                    usuarioSistema.getCodigoUsuarioSistema());
            if (datosAntes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }
            detalleUsuarioSistemaDao.deleteByUsuarioSistema(usuarioSistema);
            usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);

            usuarioSistemaService.delete(usuarioSistema);
            auditoriaEntidad.registrar("USUARIO_SISTEMA", "ELIMINACION",
                    String.valueOf(datosAntes.get("codigo")), datosAntes, null, null);
            return ResponseEntity.ok("Usuario eliminado correctamente !!");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al eliminar usuario "+ e.getMessage());
        }

    }

    private void cargarSucursalesParaFormulario(Model modelo) {
        boolean root = esRoot();
        modelo.addAttribute("esRoot", root);
        if (root) {
            modelo.addAttribute("sucursales", sucursalService.listar());
        }
    }

    private Sucursal resolverSucursalAlta(UsuarioSistema usuarioSistema, UsuarioSistema userSession) {
        if (!esRoot()) {
            return userSession.getSucursal();
        }
        Integer codigoSucursal = usuarioSistema.getSucursal() == null
                ? null
                : usuarioSistema.getSucursal().getCodigoSucursal();
        if (codigoSucursal == null) {
            return userSession.getSucursal();
        }
        Sucursal sucursal = sucursalService.encontrar(new Sucursal(codigoSucursal));
        if (sucursal == null) {
            throw new RuntimeException("Sucursal no encontrada !!");
        }
        return sucursal;
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private boolean esRoot() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(autoridad -> "ROOT".equals(autoridad.getAuthority()));
    }
}
