package ama.controladorMVC;

import ama.dominio.*;
import ama.servicio.*;
import ama.utilerias.PageRender;
import ama.utilerias.ReportGenerator;
import ama.utilerias.Reporte;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/servicio")
public class ServicioController {

    @Autowired
    private HttpSession httpSession;
    @Autowired
    private ServicioService servicioServicio;
    @Autowired
    private CategoriaService servicioCategoria;
    @Autowired
    private ParametroService parametroService;

    @Autowired
    private SucursalService servicioSucursal;

    @Autowired
    private CiudadService servicioCiudad;

    @Autowired
    private EstadoService servicioEstado;

    @Autowired
    private ComprobanteService comprobanteService;

    @Autowired
    private UsuarioService servicioUsuario;
    @Autowired
    private ManzanaService servicioManzana;
    @Autowired
    private DataSource dataSource;
    private ReportGenerator reportGenerator;

    @GetMapping("/listar")
    public String listarServicios(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantidadRegistro", defaultValue = "10") int cantElemento,
            Model modelo) {
        modelo.addAttribute("titulo", "Cuenta");
        Pageable pageable = PageRequest.of(page, cantElemento);
        var servicios = servicioServicio.listar(pageable);
        PageRender pageRender = new PageRender("/servicio/listar", servicios);
        modelo.addAttribute("page", pageRender);
        modelo.addAttribute("cantElemento", cantElemento);
        modelo.addAttribute("servicios", servicios);

        List<Categoria> categorias = servicioCategoria.listar(getSucursalSession());
        modelo.addAttribute("categorias", categorias);

        List<Sucursal> sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursales", getSucursalSession());

        var estado = servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO"));
        modelo.addAttribute("estado", estado);

        return "servicio/servicio";
    }

    @PostMapping("/buscar")
    public @ResponseBody
    Page<Servicio> listarServicios(@RequestBody Paginador paginador) {
        Pageable pageable = PageRequest.of(paginador.getNumeroPagina(), paginador.getCatidadRegistro());
        Page<Servicio> servicios = servicioServicio.buscar(pageable, paginador.getFiltro());
        return servicios;
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("accion", "Agregar");
        modelo.addAttribute("titulo", "Cuenta");
        var servicio = new Servicio();
        servicio.setFechaInicio(LocalDate.now());
        modelo.addAttribute("servicio", servicio);

        var usuario = new Usuario();
        modelo.addAttribute("usuario", usuario);

        var categorias = servicioCategoria.listar();
        modelo.addAttribute("categorias", categorias);

        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursales);

        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudad", ciudades);

        return "servicio/modificarServicio";
    }

    @GetMapping("/agregar/{codigoUsuario}")
    public String agregarSercio(Usuario usuario, Model modelo) {
        modelo.addAttribute("accion", "Agregar");

        modelo.addAttribute("titulo", "Cuenta");
        Servicio servicio = new Servicio();
        servicio.setFechaInicio(LocalDate.now());
        modelo.addAttribute("servicio", servicio);

        usuario = servicioUsuario.encontrar(usuario);
        modelo.addAttribute("usuario", usuario);

        var categorias = servicioCategoria.listar();
        modelo.addAttribute("categorias", categorias);

        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursales);

        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudad", ciudades);

        return "servicio/modificarServicio";
    }

    @GetMapping("/estadoCuenta")
    public String getServiciosCuenta(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantidadRegistro", defaultValue = "5") int cantidadRegistro,
            @RequestParam(name = "filtro", defaultValue = "") String filtro,
            @RequestParam String cuentaCorriente,
            Model modelo) {
        modelo.addAttribute("titulo", "EstadoCuenta");
        Servicio servicio = servicioServicio.encontrar(cuentaCorriente);
        modelo.addAttribute("servicio", servicio);

        modelo.addAttribute("usuario", servicio.getUsuario());
        var categoria = servicio.getCategoria();
        modelo.addAttribute("categoria", categoria);
        modelo.addAttribute("estadoCuenta", getEstadoCuenta(servicio));

        var servicios = servicioServicio.listaServicioCuenta(servicio);
        var cuentas = new ArrayList<String>();
        servicios.forEach(cuenta -> {
            cuentas.add(cuenta);
        });
        modelo.addAttribute("cuentas", cuentas);

        Pageable pageable = PageRequest.of(page, cantidadRegistro);
        Page<Comprobante> comprobantes = comprobanteService.getComprobantesCuenta(pageable, servicio);
        PageRender pageRender = new PageRender("/servicio/estadoCuenta?cuentaCorriente=" + servicio.getCuentaCorriente(), comprobantes);
        modelo.addAttribute("page", pageRender);
        modelo.addAttribute("cantElemento", cantidadRegistro);

        modelo.addAttribute("comprobantes", comprobantes);

        Sucursal sucursal = servicio.getManzana().getSucursal();
        modelo.addAttribute("sucursal", sucursal);

        Ciudad ciudad = servicio.getManzana().getSucursal().getCiudad();
        modelo.addAttribute("ciudad", ciudad);

        var estado = servicio.getEstado();
        modelo.addAttribute("estado", estado);

        return "servicio/estadoCuenta";
    }

    @PostMapping("/guardar/{accion}")
    public ResponseEntity<?> guardar(@RequestBody Servicio servicio, @PathVariable String accion) {
        // TimeZone.setDefault(TimeZone.getTimeZone("America/Asuncion"));
        String mensaje = "Registro guardado correctamente ";
        // Verificar si la cuenta corriente ya se encuentra registrada
        Servicio servicioEncontrada = servicioServicio.encontrar(servicio.getCuentaCorriente());
        if (servicioEncontrada != null && !accion.equals("editar")) {
            String nombreUsuario = servicioEncontrada.getUsuario().getNombre() + " " + servicioEncontrada.getUsuario().getApellido();
            mensaje = "La cuenta ya se encuentra registrada a nombre de: " + nombreUsuario;
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }
        if (accion.equals("editar")) {
            mensaje = "Registro modificado correctamente";
        }
        String cuentaCorriente[] = servicio.getCuentaCorriente().split("-");
        Integer numeroManzana = Integer.parseInt(cuentaCorriente[1]);
        Integer codigoSucursal = servicio.getSucursal().getCodigoSucursal();
        Manzana manzana = servicioManzana.encontrar(new ManzanaPK(numeroManzana, codigoSucursal));
        if (manzana == null) {
            mensaje = "Manzana N° " + numeroManzana + " no se encuentra registrado ";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }
        servicio.setManzana(manzana);
        try {
            servicioServicio.guardar(servicio);
            return ResponseEntity.ok("" + mensaje);
        } catch (DataAccessException e) {
            mensaje = "Ocurrio un error";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje + " " + e.getMostSpecificCause().getMessage());
        }
    }

    @PostMapping("/editar")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMIN')")
    public ResponseEntity<?> editar(@RequestBody Servicio servicio) {
        var servicioEncontrado = servicioServicio.encontrar(servicio.getCuentaCorriente());
        if (servicioEncontrado == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Registro no encontrado");
        }
        return ResponseEntity.ok(servicioEncontrado);
    }

    @DeleteMapping("/eliminar")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
    public ResponseEntity<?> eliminar(@RequestParam("id") String cuentaCorriente) {
        try {
            var respuesta = servicioServicio.encontrar(cuentaCorriente);
            if (respuesta == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Registro no encontrado!!");
            }
            servicioServicio.eliminar(respuesta);
            return ResponseEntity.ok("Registro eliminado correctamente ");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al eliminar Cuenta: " + cuentaCorriente +" "+  e.getMessage());
        }

    }

    @PostMapping("/extractoCuenta")
    public ResponseEntity<?> getExtractoCuenta(
            @RequestParam String cuentaCorriente,
            @RequestParam int cantidadRegistro
    ) throws SQLException {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("cuentaCorriente", cuentaCorriente);
        parametros.put("cantidadRegistro", cantidadRegistro);
        parametros.put("codigoSucursal", 1);

        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .ruta("reportes/extractoCuenta.jasper")
                .nombre("ExtractoCuenta")
                .parametros(parametros)
                .build();
        return new ReportGenerator().getReporte(reporte);

    }

    private EstadoCuenta getEstadoCuenta(Servicio servicio) {
        List<Object[]> datos = comprobanteService.getPagoHastaAndSaldo(servicio.getCuentaCorriente());
        LocalDate pagoHasta = null;
        Double saldo = 0.0;
        if (datos.isEmpty()) {
            pagoHasta = servicio.getFechaInicio();
        } else {
            for (Object[] resul : datos) {
                pagoHasta = ((java.sql.Date) resul[0]).toLocalDate();
                saldo = (Double) resul[1];
            }
        }
        EstadoCuenta estadoCuenta = new EstadoCuenta(
                servicio.getCategoria().getTarifa(),
                getParametro(),
                pagoHasta);
        estadoCuenta.setSaldoAnterior(saldo);
        return estadoCuenta;
    }

    private Parametro getParametro() {
        return parametroService.encontrar(getUserSession().getSucursal());
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }
}
