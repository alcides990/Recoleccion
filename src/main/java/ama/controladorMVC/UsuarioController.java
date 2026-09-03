package ama.controladorMVC;

import ama.DTO.UsuarioDTO;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import ama.dominio.Paginador;
import ama.dominio.Usuario;
import ama.utilerias.PageRender;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ama.servicio.CobradorService;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import ama.servicio.EstadoService;
import ama.servicio.TipoDocumentoService;
import ama.servicio.UsuarioService;
import ama.servicio.AuditoriaEntidadService;
import ama.servicio.EliminacionEntidadService;
import java.util.Arrays;
import ama.dominio.UsuarioSistema;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

@Slf4j
@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService servicioUsuario;

    @Autowired
    private SucursalService servicioSucursal;

    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private CobradorService servicioCobrador;
    @Autowired
    private EstadoService servicioEstado;

    @Autowired
    private TipoDocumentoService servicioTipoDocumento;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Vadidador validar;

    @Autowired
    private HttpSession httpSession;
    @Autowired
    private AuditoriaEntidadService auditoriaEntidad;
    @Autowired
    private EliminacionEntidadService eliminacionEntidadService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @GetMapping("/listar")
    public String listaUsuario(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantElemento", defaultValue = "10") int cantElemento,
            @RequestParam(name = "filtro", defaultValue = "") String filtro, Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        return "usuario/usuario";
    }

    @PostMapping("/tabla")
    @ResponseBody
    public DataTableResponseV2<Map<String, Object>> tabla(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "0") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "desc") String direccion) {
        int limite = Math.min(Math.max(length, 1), 100);
        Integer codigoSucursal = getUserSession().getSucursal().getCodigoSucursal();
        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(start, 0) / limite, limite,
                Sort.by(sentido, ordenUsuario(columna)));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Page<Usuario> pagina = filtro.isBlank()
                ? servicioUsuario.listarPorSucursal(pageable, codigoSucursal)
                : servicioUsuario.buscarPorSucursal(pageable, codigoSucursal, filtro);
        List<Map<String, Object>> filas = pagina.getContent().stream().map(usuario -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", usuario.getCodigoUsuario());
            fila.put("documento", usuario.getNumeroDocumento());
            fila.put("nombre", usuario.getNombre() + " " + (usuario.getApellido() == null ? "" : usuario.getApellido()));
            fila.put("celular", usuario.getCelular());
            fila.put("correo", usuario.getCorreo());
            fila.put("barrio", usuario.getBarrio());
            fila.put("direccion", usuario.getDireccion());
            fila.put("sucursal", usuario.getSucursal().getNombreSucursal());
            fila.put("ciudad", usuario.getSucursal().getCiudad().getNombreCiudad());
            fila.put("estado", usuario.getEstado().getEstado());
            return fila;
        }).toList();
        return new DataTableResponseV2<>(draw, servicioUsuario.contarPorSucursal(codigoSucursal),
                pagina.getTotalElements(), filas);
    }

    private String ordenUsuario(int columna) {
        return switch (columna) {
            case 0 -> "codigoUsuario";
            case 1 -> "numeroDocumento";
            case 3 -> "celular";
            case 4 -> "barrio";
            case 5 -> "sucursal.nombreSucursal";
            case 6 -> "estado.estado";
            default -> "nombre";
        };
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    @PostMapping("/listar/pagina")
    public @ResponseBody
    Page<UsuarioDTO> listarServicios(@RequestBody Paginador paginador) {
        Pageable pageable = PageRequest.of(paginador.getNumeroPagina(), paginador.getCatidadRegistro());
        var usuarios = servicioUsuario.listar(pageable, paginador.getFiltro());
        return usuarios.map(usuario -> modelMapper.map(usuario, UsuarioDTO.class));
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        var usuario = new Usuario();
        modelo.addAttribute("usuario", usuario);

        var tipoDocumento = servicioTipoDocumento.listar();
        modelo.addAttribute("tipoDocumento", tipoDocumento);

        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursales);

        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudad", ciudades);

        var estado = servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO"));
        modelo.addAttribute("estado", estado);

        return "usuario/modificarUsuario";
    }

    @GetMapping(value = "/buscar/{filtro}", produces = {"application/json"})
    public @ResponseBody
    List<UsuarioDTO> buscarusuario(@PathVariable String filtro) {
        UsuarioSistema usuarioSesion = getUserSession();
        if (usuarioSesion == null || usuarioSesion.getSucursal() == null) {
            return List.of();
        }
        var usuarios = servicioUsuario.Buscar(
                filtro, usuarioSesion.getSucursal().getCodigoSucursal());
        return usuarios.stream()
                .map(usuario -> modelMapper
                .map(usuario, UsuarioDTO.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/guardar")
    @Transactional
    public String guardar(@Valid Usuario usuario, BindingResult resultado, Model modelo) {
        if (usuario.getCorreo() != null) {
            String correo = usuario.getCorreo().trim().toLowerCase();
            usuario.setCorreo(correo.isBlank() ? null : correo);
        }
        if (resultado.hasErrors()) {
            modelo.addAttribute("titulo", "Usuario");
            modelo.addAttribute("tipoDocumento", servicioTipoDocumento.listar());
            modelo.addAttribute("sucursal", servicioSucursal.listar());
            modelo.addAttribute("ciudad", servicioCiudad.listarCiudad());
            modelo.addAttribute("estado", servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO")));
            return "usuario/modificarUsuario";
        }
        boolean alta = usuario.getCodigoUsuario() == null;
        Usuario anterior = alta ? null : servicioUsuario.encontrar(usuario);
        Map<String, Object> datosAntes = auditoriaEntidad.usuario(anterior);
        if (alta) {
            Integer codigoUsuario = servicioUsuario.getCodigoUsuario() + 1;
            usuario.setCodigoUsuario(codigoUsuario);
        }
        servicioUsuario.guardar(usuario);
        auditoriaEntidad.registrar("USUARIO", alta ? "ALTA" : "MODIFICACION",
                String.valueOf(usuario.getCodigoUsuario()), datosAntes,
                auditoriaEntidad.usuario(usuario), null);
        return "redirect:/usuario/listar";
    }

    @GetMapping("/editar/{codigoUsuario}")
    public String editar(Usuario usuario, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuario = servicioUsuario.encontrar(usuario);
        if(usuario==null){
            throw  new Error("Usuario no encontrado");
        }
        model.addAttribute("usuario", usuario);
        var tipoDocumento = servicioTipoDocumento.listar();
        model.addAttribute("tipoDocumento", tipoDocumento);
        var sucursal = usuario.getSucursal();
        model.addAttribute("sucursal", sucursal);
        var ciudad = usuario.getSucursal().getCiudad();
        model.addAttribute("ciudad", ciudad);

        var estado =servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO"));
        model.addAttribute("estado", estado);

        return "usuario/modificarUsuario";
    }


    @PostMapping("/eliminar/{codigoUsuario}")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
    public ResponseEntity<?> modal(Usuario usuario) {
        Usuario usuarioRecuperado = servicioUsuario.encontrar(usuario);
        try {
            if (usuarioRecuperado == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado.");
            }
            if (!usuarioRecuperado.getSucursal().getCodigoSucursal()
                    .equals(getUserSession().getSucursal().getCodigoSucursal())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No puede eliminar un usuario de otra sucursal.");
            }
            Map<String, Object> datosAntes = auditoriaEntidad.usuario(usuarioRecuperado);
            eliminacionEntidadService.eliminarUsuario(usuarioRecuperado, datosAntes);
            return ResponseEntity.ok("Usuario eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {
            String nombreUsuario = usuarioRecuperado == null ? ""
                    : usuarioRecuperado.getNombre() + " " + usuarioRecuperado.getApellido();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar el usuario " + nombreUsuario.trim()
                            + " porque tiene cuentas u otros registros relacionados.");
        }
    }
}
