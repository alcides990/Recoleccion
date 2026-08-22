package ama.controladorMVC;

import ama.dominio.*;
import ama.servicio.*;
import ama.utilerias.PageRender;
import ama.utilerias.ReportGenerator;
import ama.utilerias.Reporte;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UbicacionServicioService ubicacionServicioService;
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
            fila.put("ocupado", servicio.getOcupado());
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
            case 3 -> "ocupado";
            case 4 -> "categoria.nombreCategoria";
            case 5 -> "estado.estado";
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
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .body(resumen);
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
            if (servicio.getOcupado() == null) {
                servicio.setOcupado(servicioEncontrada.getOcupado());
            }
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

    @GetMapping("/historial/suspensiones")
    @ResponseBody
    public List<Map<String, Object>> historialSuspensiones(@RequestParam String cuentaCorriente) {
        validarCuentaSucursal(cuentaCorriente);
        return jdbcTemplate.queryForList("""
                SELECT hs.codigo_suspension AS codigoSuspension,
                       hs.fecha_desde AS fechaDesde, hs.fecha_hasta AS fechaHasta,
                       hs.cantidad_periodos_pendientes AS cantidadPeriodosPendientes,
                       hs.motivo, us.usuario, hs.fecha_registro AS fechaRegistro
                FROM historial_suspensiones_servicio hs
                JOIN usuarios_sistema us
                  ON us.codigo_usuario_sistema = hs.codigo_usuario_sistema
                WHERE hs.cuenta_corriente = ?
                ORDER BY hs.fecha_desde DESC, hs.codigo_suspension DESC
                """, cuentaCorriente);
    }

    @PostMapping("/historial/suspensiones")
    @ResponseBody
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
    public ResponseEntity<?> registrarSuspension(@RequestBody SuspensionSolicitud solicitud) {
        Servicio servicio = validarCuentaSucursal(solicitud.cuentaCorriente());
        if (solicitud.fechaDesde() == null || solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            return ResponseEntity.badRequest().body("Indique la fecha y el motivo de la suspensión");
        }
        Integer activas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM historial_suspensiones_servicio
                WHERE cuenta_corriente = ? AND fecha_hasta IS NULL
                """, Integer.class, solicitud.cuentaCorriente());
        if (activas != null && activas > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("La cuenta ya posee una suspensión activa");
        }
        Integer periodosPendientes = jdbcTemplate.queryForObject(
                "SELECT fn_mes_deuda(?, ?)", Integer.class,
                solicitud.cuentaCorriente(), getUserSession().getSucursal().getCodigoSucursal());
        jdbcTemplate.update("""
                INSERT INTO historial_suspensiones_servicio
                    (cuenta_corriente, fecha_desde, cantidad_periodos_pendientes,
                     motivo, codigo_usuario_sistema)
                VALUES (?, ?, ?, ?, ?)
                """, solicitud.cuentaCorriente(), solicitud.fechaDesde(),
                periodosPendientes == null ? 0 : periodosPendientes, solicitud.motivo().trim(),
                getUserSession().getCodigoUsuarioSistema());
        jdbcTemplate.update("UPDATE servicios SET ocupado = 'DESOCUPADO' WHERE cuenta_corriente = ?",
                servicio.getCuentaCorriente());
        return ResponseEntity.ok("Suspensión registrada correctamente");
    }

    @PostMapping("/historial/suspensiones/finalizar")
    @ResponseBody
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
    public ResponseEntity<?> finalizarSuspension(@RequestBody FinalizarSuspensionSolicitud solicitud) {
        validarCuentaSucursal(solicitud.cuentaCorriente());
        if (solicitud.fechaHasta() == null) {
            return ResponseEntity.badRequest().body("Indique la fecha de finalización");
        }
        int actualizadas = jdbcTemplate.update("""
                UPDATE historial_suspensiones_servicio
                SET fecha_hasta = ?
                WHERE cuenta_corriente = ? AND fecha_hasta IS NULL
                  AND fecha_desde <= ?
                """, solicitud.fechaHasta(), solicitud.cuentaCorriente(), solicitud.fechaHasta());
        if (actualizadas == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No existe una suspensión activa para finalizar");
        }
        jdbcTemplate.update("UPDATE servicios SET ocupado = 'OCUPADO' WHERE cuenta_corriente = ?",
                solicitud.cuentaCorriente());
        return ResponseEntity.ok("Suspensión finalizada; la cuenta quedó marcada como ocupada");
    }

    @GetMapping("/historial/exoneraciones")
    @ResponseBody
    public List<Map<String, Object>> historialExoneraciones(@RequestParam String cuentaCorriente) {
        validarCuentaSucursal(cuentaCorriente);
        return jdbcTemplate.queryForList("""
                SELECT he.codigo_exoneracion AS codigoExoneracion,
                       he.fecha_desde_anterior AS fechaDesdeAnterior,
                       he.fecha_desde_nueva AS fechaDesdeNueva,
                       he.cantidad_periodos AS cantidadPeriodos,
                       he.motivo, us.usuario, he.fecha_registro AS fechaRegistro
                FROM historial_exoneraciones_servicio he
                JOIN usuarios_sistema us
                  ON us.codigo_usuario_sistema = he.codigo_usuario_sistema
                WHERE he.cuenta_corriente = ?
                ORDER BY he.fecha_registro DESC, he.codigo_exoneracion DESC
                """, cuentaCorriente);
    }

    @PostMapping("/historial/exoneraciones")
    @ResponseBody
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
    public ResponseEntity<?> registrarExoneracion(@RequestBody ExoneracionSolicitud solicitud) {
        Servicio servicio = validarCuentaSucursal(solicitud.cuentaCorriente());
        if (solicitud.fechaDesdeNueva() == null || solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            return ResponseEntity.badRequest().body("Indique la nueva fecha desde y el motivo de la exoneración");
        }
        if (solicitud.fechaDesdeNueva().isBefore(servicio.getFechaInicio())) {
            return ResponseEntity.badRequest().body("La fecha desde no puede ser anterior al inicio del servicio");
        }
        LocalDate anterior = jdbcTemplate.queryForObject(
                "SELECT STR_TO_DATE(CONCAT(fn_pagar_desde(?), '-01'), '%m-%Y-%d')",
                LocalDate.class, solicitud.cuentaCorriente());
        if (anterior == null) anterior = servicio.getFechaInicio();
        long cantidadPeriodos = ChronoUnit.MONTHS.between(
                YearMonth.from(anterior), YearMonth.from(solicitud.fechaDesdeNueva()));
        if (cantidadPeriodos <= 0) {
            return ResponseEntity.badRequest().body(
                    "La nueva fecha desde debe avanzar al menos un período mensual");
        }
        jdbcTemplate.update("""
                INSERT INTO historial_exoneraciones_servicio
                    (cuenta_corriente, fecha_desde_anterior, fecha_desde_nueva,
                     cantidad_periodos, motivo, codigo_usuario_sistema)
                VALUES (?, ?, ?, ?, ?, ?)
                """, solicitud.cuentaCorriente(), anterior, solicitud.fechaDesdeNueva(),
                cantidadPeriodos, solicitud.motivo().trim(), getUserSession().getCodigoUsuarioSistema());
        return ResponseEntity.ok("Exoneración registrada correctamente");
    }

    @GetMapping("/historial/pagos")
    @ResponseBody
    public DataTableResponseV2<Map<String, Object>> historialPagos(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam String cuentaCorriente,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "1") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "desc") String direccion) {
        validarCuentaSucursal(cuentaCorriente);
        int limite = Math.min(Math.max(length, 1), 100);
        int inicio = Math.max(start, 0);
        String filtro = busqueda == null ? "" : busqueda.trim();
        String like = "%" + filtro + "%";
        String[] ordenes = {"c.numero_comprobante", "c.fecha_pago", "c.periodo_pago",
            "c.cantidad_pago", "c.total_importe", "e.estado"};
        String orden = ordenes[Math.max(0, Math.min(columna, ordenes.length - 1))];
        String sentido = "asc".equalsIgnoreCase(direccion) ? "ASC" : "DESC";
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comprobantes WHERE cuenta_corriente = ?", Long.class, cuentaCorriente);
        Long filtrado = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM comprobantes c
                JOIN estados e ON e.codigo_estado = c.codigo_estado
                WHERE c.cuenta_corriente = ?
                  AND (? = '' OR CAST(c.numero_comprobante AS CHAR) LIKE ?
                       OR COALESCE(c.periodo_pago, '') LIKE ? OR e.estado LIKE ?)
                """, Long.class, cuentaCorriente, filtro, like, like, like);
        String sqlPagos = """
                SELECT c.numero_comprobante AS numeroComprobante,
                       c.fecha_pago AS fechaPago, c.periodo_pago AS periodoPago,
                       c.cantidad_pago AS cantidadPago, c.total_importe AS totalImporte,
                       e.estado
                FROM comprobantes c
                JOIN estados e ON e.codigo_estado = c.codigo_estado
                WHERE c.cuenta_corriente = ?
                  AND (? = '' OR CAST(c.numero_comprobante AS CHAR) LIKE ?
                       OR COALESCE(c.periodo_pago, '') LIKE ? OR e.estado LIKE ?)
                ORDER BY %s %s
                LIMIT ? OFFSET ?
                """.formatted(orden, sentido);
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sqlPagos,
                cuentaCorriente, filtro, like, like, like, limite, inicio);
        return new DataTableResponseV2<>(draw, total == null ? 0 : total,
                filtrado == null ? 0 : filtrado, filas);
    }

    @GetMapping("/ubicacion")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public UbicacionServicioService.DetalleUbicacion consultarUbicacion(
            @RequestParam String cuentaCorriente) {
        validarCuentaSucursal(cuentaCorriente);
        return ubicacionServicioService.consultar(cuentaCorriente);
    }

    @PostMapping("/ubicacion")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> guardarUbicacion(@RequestBody UbicacionSolicitud solicitud) {
        if (solicitud == null || solicitud.cuentaCorriente() == null
                || solicitud.cuentaCorriente().isBlank()) {
            return ResponseEntity.badRequest().body("Indique la cuenta corriente");
        }
        validarCuentaSucursal(solicitud.cuentaCorriente());
        try {
            return ResponseEntity.ok(ubicacionServicioService.guardar(
                    solicitud.cuentaCorriente().trim(),
                    solicitud.latitud(), solicitud.longitud(),
                    solicitud.precisionMetros(), solicitud.metodo(),
                    getUserSession().getCodigoUsuarioSistema()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Servicio validarCuentaSucursal(String cuentaCorriente) {
        Servicio servicio = servicioServicio.encontrar(cuentaCorriente);
        if (servicio == null || !servicio.getSucursal().getCodigoSucursal()
                .equals(getSucursalSession().getCodigoSucursal())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        }
        return servicio;
    }

    public record SuspensionSolicitud(String cuentaCorriente, LocalDate fechaDesde, String motivo) {}
    public record FinalizarSuspensionSolicitud(String cuentaCorriente, LocalDate fechaHasta) {}
    public record ExoneracionSolicitud(String cuentaCorriente, LocalDate fechaDesdeNueva, String motivo) {}
    public record UbicacionSolicitud(String cuentaCorriente, BigDecimal latitud,
            BigDecimal longitud, BigDecimal precisionMetros, String metodo) {}

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
            @RequestParam String cuentaCorriente
    ) throws SQLException {
        validarCuentaSucursal(cuentaCorriente);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("cuentaCorriente", cuentaCorriente);
        parametros.put("codigoSucursal", getUserSession().getSucursal().getCodigoSucursal());

        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .ruta("reportes/extractoCuenta.jrxml")
                .nombre("ExtractoCuenta")
                .parametros(parametros)
                .build();
        return new ReportGenerator().getReporte(reporte);

    }

    private EstadoCuenta getEstadoCuenta(Servicio servicio) {
        List<Object[]> datos = comprobanteService.getEstadoCuentaMovil(
                servicio.getCuentaCorriente(), getSucursalSession().getCodigoSucursal());
        LocalDate pagoHasta = servicio.getFechaInicio();
        Double saldo = 0.0;
        Integer cantidadDeuda = null;
        if (!datos.isEmpty()) {
            for (Object[] resul : datos) {
                if (resul[0] != null) {
                    pagoHasta = YearMonth.parse(String.valueOf(resul[0]),
                            DateTimeFormatter.ofPattern("MM-yyyy")).atDay(1);
                }
                if (resul[1] instanceof Number) saldo = ((Number) resul[1]).doubleValue();
                if (resul[2] instanceof Number) cantidadDeuda = ((Number) resul[2]).intValue();
            }
        }
        EstadoCuenta estadoCuenta = new EstadoCuenta(
                servicio.getCategoria().getTarifa(),
                getParametro(),
                pagoHasta);
        estadoCuenta.setSaldoAnterior(saldo);
        if (cantidadDeuda != null) estadoCuenta.setCantidadDeuda(cantidadDeuda);
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
