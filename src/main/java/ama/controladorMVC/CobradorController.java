package ama.controladorMVC;

import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import ama.validador.Mayuscula;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ama.servicio.CobradorService;
import ama.servicio.EliminacionCobradorService;
import ama.servicio.EliminacionCobradorService.CobradorConRegistrosRelacionadosException;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import ama.servicio.EstadoService;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Controller
@SessionAttributes(names = {"sucursal", "ciudad", "estado"})
@RequestMapping("/cobrador")
public class CobradorController {
 
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private SucursalService servicioSucursal;
    @Autowired
    private EliminacionCobradorService eliminacionCobradorService;

    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private CobradorService servicioCobrador;
    @Autowired
    private EstadoService servicioEstado;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaCobradores(Model modelo) {
        modelo.addAttribute("titulo", "Cobrador");

        return "cobrador/cobrador";
    }

    @PostMapping("/tabla")
    @ResponseBody
    public DataTableResponseV2<Map<String, Object>> tabla(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "1") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "asc") String direccion) {
        int limite = Math.min(Math.max(length, 1), 100);
        Integer codigoSucursal = getSucursalSession().getCodigoSucursal();
        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(start, 0) / limite, limite,
                Sort.by(sentido, ordenCobrador(columna)));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Page<Cobrador> pagina = filtro.isBlank()
                ? servicioCobrador.listarPorSucursal(pageable, codigoSucursal)
                : servicioCobrador.buscarPorSucursal(pageable, codigoSucursal, filtro);
        List<Map<String, Object>> filas = pagina.getContent().stream().map(cobrador -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", cobrador.getCodigoCobrador());
            fila.put("nombre", cobrador.getNombreCompleto());
            fila.put("celular", cobrador.getCelular());
            fila.put("direccion", cobrador.getDireccion());
            fila.put("estado", cobrador.getEstado().getEstado());
            fila.put("sucursal", cobrador.getSucursal().getNombreSucursal());
            fila.put("ciudad", cobrador.getSucursal().getCiudad().getNombreCiudad());
            return fila;
        }).toList();
        return new DataTableResponseV2<>(draw, servicioCobrador.contarPorSucursal(codigoSucursal),
                pagina.getTotalElements(), filas);
    }

    private String ordenCobrador(int columna) {
        return switch (columna) {
            case 0 -> "codigoCobrador";
            case 2 -> "celular";
            case 3 -> "direccion";
            case 4 -> "estado.estado";
            default -> "nombre";
        };
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        var cobrador = new Cobrador();
        cobrador.setSucursal(getSucursalSession());
        modelo.addAttribute("cobrador", cobrador);
        cargarFormulario(modelo);

        return "cobrador/modificarCobrador";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid Cobrador cobrador,
            BindingResult resul, SessionStatus status,
            RedirectAttributes flash, Model model) {
        if (resul.hasFieldErrors()) {
            cargarFormulario(model);
            return "cobrador/modificarCobrador";
        }
        if (cobrador.getCodigoCobrador() == null) {
            Integer codigoCobrador = servicioCobrador.getCodigoCobrador() + 1;
            cobrador.setCodigoCobrador(codigoCobrador);
        }
        cobrador.setSucursal(resolverSucursal(cobrador));
        flash.addFlashAttribute("info", "Registro guardado correctamente!!");
        status.setComplete();
        servicioCobrador.guardar(cobrador);
        return "redirect:/cobrador/listar";
    }

    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR')")
    @GetMapping("/editar/{codigoCobrador}")
    public String editar(Cobrador cobrador, Model model) {
        cobrador = servicioCobrador.encontrar(cobrador);
        if (cobrador == null || !cobrador.getSucursal().getCodigoSucursal()
                .equals(getSucursalSession().getCodigoSucursal())) {
            throw new Error("Cobrar no encontrado ");
        }
        model.addAttribute("cobrador", cobrador);
        cargarFormulario(model);

        return "cobrador/modificarCobrador";
    }

    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
    @PostMapping("/eliminar/{codigoCobrador}")
    public ResponseEntity<?> eliminar(Cobrador cobrador) {
        try {
            Cobrador cobradorEncontrado = servicioCobrador.encontrar(cobrador);
            if (cobradorEncontrado == null || !cobradorEncontrado.getSucursal().getCodigoSucursal()
                    .equals(getSucursalSession().getCodigoSucursal())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("El cobrador no pertenece a su sucursal");
            }
            eliminacionCobradorService.eliminar(cobradorEncontrado);
            return ResponseEntity.ok("Cobrador eliminado correctamente.");
        } catch (CobradorConRegistrosRelacionadosException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar el cobrador. " + e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar el cobrador porque tiene registros relacionados.");
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }

    private void cargarFormulario(Model modelo) {
        boolean root = esRoot();
        modelo.addAttribute("esRoot", root);
        modelo.addAttribute("sucursalSesion", getSucursalSession());
        modelo.addAttribute("sucursal", root ? servicioSucursal.listar() : List.of(getSucursalSession()));
        modelo.addAttribute("ciudad", servicioCiudad.listarCiudad());
        modelo.addAttribute("estado", servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO")));
        modelo.addAttribute("titulo", "Cobrador");
    }

    private Sucursal resolverSucursal(Cobrador cobrador) {
        if (!esRoot()) {
            return getSucursalSession();
        }
        Integer codigoSucursal = cobrador.getSucursal() == null
                ? null
                : cobrador.getSucursal().getCodigoSucursal();
        if (codigoSucursal == null) {
            return getSucursalSession();
        }
        Sucursal sucursal = servicioSucursal.encontrar(new Sucursal(codigoSucursal));
        if (sucursal == null) {
            throw new RuntimeException("Sucursal no encontrada");
        }
        return sucursal;
    }

    private boolean esRoot() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(autoridad -> "ROOT".equals(autoridad.getAuthority()));
    }
}
