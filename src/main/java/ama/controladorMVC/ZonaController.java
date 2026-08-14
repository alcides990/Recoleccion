package ama.controladorMVC;

import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.dominio.Zona;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ama.servicio.CobradorService;
import ama.servicio.ZonaService;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/zona")
public class ZonaController {

    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private ZonaService zonaService;
    @Autowired
    private CobradorService cobradorService;
    @Autowired
    private SucursalService servicioSucursal;
    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaZona(Model modelo) {
        modelo.addAttribute("titulo", "Zona");
        return "zona/zona";
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
                Sort.by(sentido, ordenZona(columna)));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Page<Zona> pagina = filtro.isBlank()
                ? zonaService.listarPorSucursal(pageable, codigoSucursal)
                : zonaService.buscarPorSucursal(pageable, codigoSucursal, filtro);
        List<Map<String, Object>> filas = pagina.getContent().stream().map(zona -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", zona.getCodigoZona());
            fila.put("zona", zona.getNombreZona());
            fila.put("sucursal", zona.getSucursal().getNombreSucursal());
            fila.put("ciudad", zona.getSucursal().getCiudad().getNombreCiudad());
            fila.put("cobrador", zona.getCobrador().getNombreCompleto());
            return fila;
        }).toList();
        return new DataTableResponseV2<>(draw, zonaService.contarPorSucursal(codigoSucursal),
                pagina.getTotalElements(), filas);
    }

    private String ordenZona(int columna) {
        return switch (columna) {
            case 0 -> "sucursal.nombreSucursal";
            case 2 -> "cobrador.nombre";
            default -> "nombreZona";
        };
    }

    @GetMapping("/agregar")
    public String agregar(Model model) {
        model.addAttribute("titulo", "zona");
        
        Zona zona = new Zona();
        model.addAttribute("zona", zona);
        
        var cobradores = cobradorService.listarIsEstadoActivo(getSucursalSession());
        model.addAttribute("cobradores", cobradores);

        var sucursales = servicioSucursal.listar();
        model.addAttribute("sucursales", sucursales);

        var ciudad = servicioCiudad.listarCiudad();
        model.addAttribute("ciudad", ciudad);
        return "zona/modificarZona";
    }

    @PostMapping("/guardar")
    public String guardar(Zona zona, RedirectAttributes redirectAttributes) {
        var cobrador = cobradorService.encontrar(zona.getCobrador());
        if (cobrador == null || !cobrador.getSucursal().getCodigoSucursal()
                .equals(getSucursalSession().getCodigoSucursal())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El cobrador no pertenece a su sucursal");
        }
        zona.setSucursal(getSucursalSession());
        zona.setCobrador(cobrador);
        if (zona.getCodigoZona() == null) {
            Integer codigoZona = zonaService.getCodigoZona() + 1;
            zona.setCodigoZona(codigoZona);
        }
        zonaService.guardar(zona);
        redirectAttributes.addFlashAttribute("mensaje", "Registro guardado corectamente!!");
        return "redirect:/zona/listar";
    }

    @GetMapping("/editar/{codigoZona}")
    public String editar(Zona zona, Model model) {
        model.addAttribute("titulo", "zona");
        zona = zonaService.encontrar(zona);
        if (zona == null || !zona.getSucursal().getCodigoSucursal()
                .equals(getSucursalSession().getCodigoSucursal())) {
            throw  new Error("Zona no encontrada ");
        }
        model.addAttribute("zona", zona);

        model.addAttribute("cobradores",cobradorService.listarIsEstadoActivo(getSucursalSession()));
        
        model.addAttribute("sucursales",zona.getSucursal());
        
        model.addAttribute("ciudad", zona.getSucursal().getCiudad());
        
        return "zona/modificarZona";
    }

    
    @PostMapping("/eliminar/{codigoZona}")
    public ResponseEntity<String> eliminar(Zona zona) {
        try {
            Zona zonaEncontrada = zonaService.encontrar(zona);
            if (zonaEncontrada == null || !zonaEncontrada.getSucursal().getCodigoSucursal()
                    .equals(getSucursalSession().getCodigoSucursal())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("La zona no pertenece a su sucursal");
            }
            zonaService.eliminar(zonaEncontrada);
            return ResponseEntity.ok("Zona Eliminado Correctamente !!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se pudo eliminar el usuario " + e.getMessage());
        }
    }
    
     private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }
}
