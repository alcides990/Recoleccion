package ama.controladorMVC;

import ama.dominio.*;
import ama.servicio.*;
import ama.utilerias.PageRender;
import ama.utilerias.ReportGenerator;
import ama.utilerias.Reporte;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        modelo.addAttribute("versionDos", false);
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

    @GetMapping("/listar-v2")
    public String listarServiciosV2(Model modelo) {
        modelo.addAttribute("titulo", "Cuentas - V2");
        modelo.addAttribute("versionDos", true);
        modelo.addAttribute("servicios", Page.empty());
        modelo.addAttribute("categorias", servicioCategoria.listar(getSucursalSession()));
        modelo.addAttribute("sucursales", getSucursalSession());
        modelo.addAttribute("estado", servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO")));
        return "servicio/servicio";
    }

    @PostMapping("/tabla-v2")
    @ResponseBody
    public DataTableResponseV2<Map<String, Object>> listarServiciosV2(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "0") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "asc") String direccion) {
        int limite = Math.min(Math.max(length, 1), 100);
        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(start, 0) / limite, limite,
                Sort.by(sentido, getOrdenServicio(columna)));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Integer codigoSucursal = getSucursalSession().getCodigoSucursal();
        Page<Servicio> pagina = filtro.isBlank()
                ? servicioServicio.listarPorSucursal(pageable, codigoSucursal)
                : servicioServicio.buscarPorSucursal(pageable, codigoSucursal, filtro);
        long total = servicioServicio.contarPorSucursal(codigoSucursal);
        List<Map<String, Object>> filas = pagina.getContent().stream().map(servicio -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("cuentaCorriente", servicio.getCuentaCorriente());
            fila.put("usuario", servicio.getUsuario().getNombre() + " "
                    + (servicio.getUsuario().getApellido() == null ? "" : servicio.getUsuario().getApellido()));
            fila.put("fechaInicio", servicio.getFechaInicio());
            fila.put("categoria", servicio.getCategoria().getTarifa() + "-" + servicio.getCategoria().getNombreCategoria());
            fila.put("estado", servicio.getEstado().getEstado());
            return fila;
        }).toList();
        return new DataTableResponseV2<>(draw, total, pagina.getTotalElements(), filas);
    }

    private String getOrdenServicio(int columna) {
        return switch (columna) {
            case 1 -> "usuario.nombre";
            case 2 -> "fechaInicio";
            case 3 -> "categoria.nombreCategoria";
            case 4 -> "estado.estado";
            default -> "cuentaCorriente";
        };
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

    @GetMapping("/estadoCuenta/resumen")
    public ResponseEntity<?> getResumenEstadoCuenta(@RequestParam String cuentaCorriente) {
        Servicio servicio = servicioServicio.encontrar(cuentaCorriente);
        if (servicio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cuenta no encontrada");
        }

        EstadoCuenta estadoCuenta = getEstadoCuenta(servicio);
        int cantidadPeriodos = estadoCuenta.getCantidadDeuda();
        LocalDate ultimoPago = comprobanteService
                .getUltimoComprobanteCuentaActivo(cuentaCorriente)
                .map(Comprobante::getFechaPago)
                .orElse(null);
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("cuentaCorriente", servicio.getCuentaCorriente());
        resumen.put("usuario", servicio.getUsuario().getNombre() + " "
                + (servicio.getUsuario().getApellido() == null ? "" : servicio.getUsuario().getApellido()));
        resumen.put("ultimoPago", ultimoPago);
        resumen.put("pagoDesde", estadoCuenta.getPagoHasta() == null
                ? ""
                : estadoCuenta.getPagoHasta().format(DateTimeFormatter.ofPattern("MM-yyyy")));
        resumen.put("cantidadPeriodos", cantidadPeriodos);
        resumen.put("tarifa", estadoCuenta.getTarifa());
        resumen.put("subTotal", estadoCuenta.getSubTotal());
        resumen.put("saldoAnterior", estadoCuenta.getSaldoAnterior());
        resumen.put("recargo", estadoCuenta.getRecargo());
        resumen.put("totalDeuda", estadoCuenta.getTotalDeuda());
        return ResponseEntity.ok(resumen);
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
        List<Object[]> datos = comprobanteService.getPagoDesdeAndSaldo(servicio.getCuentaCorriente());
        LocalDate pagoHasta = null;
        Double saldo = 0.0;
        if (datos.isEmpty()) {
            pagoHasta = servicio.getFechaInicio();
        } else {
            for (Object[] resul : datos) {
                pagoHasta = YearMonth.parse(
                        (String) resul[0], DateTimeFormatter.ofPattern("MM-yyyy"))
                        .atDay(1);
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
