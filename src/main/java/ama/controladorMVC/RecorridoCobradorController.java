package ama.controladorMVC;

import ama.dominio.UsuarioSistema;
import ama.servicio.CobradorService;
import ama.servicio.RecorridoCobradorService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/recorridos")
@PreAuthorize("hasAuthority('ROOT')")
@RequiredArgsConstructor
public class RecorridoCobradorController {

    private final HttpSession httpSession;
    private final CobradorService cobradorService;
    private final RecorridoCobradorService recorridoService;

    @Value("${recoleccion.mapas.tiles-url}")
    private String mapaTilesUrl;

    @Value("${recoleccion.mapas.attribution}")
    private String mapaAttribution;

    @GetMapping
    public String pagina(Model modelo) {
        UsuarioSistema usuario = usuarioActual();
        modelo.addAttribute("titulo", "Recorridos de cobradores");
        modelo.addAttribute("cobradores",
                cobradorService.listarIsEstadoActivo(usuario.getSucursal()));
        modelo.addAttribute("mapaTilesUrl", mapaTilesUrl);
        modelo.addAttribute("mapaAttribution", mapaAttribution);
        return "recorrido/recorridos";
    }

    @GetMapping("/lista")
    @ResponseBody
    public List<Map<String, Object>> listar() {
        return recorridoService.listar(codigoSucursal());
    }

    @GetMapping("/dispositivos") @ResponseBody public List<Map<String,Object>> dispositivos(){return recorridoService.listarDispositivos(codigoSucursal());}
    @PostMapping("/dispositivos/{id}/asignar") @ResponseBody public ResponseEntity<?> asignar(@PathVariable String id,@RequestBody AsignarDispositivoSolicitud r){try{return ResponseEntity.ok(recorridoService.asignarDispositivo(id,r.codigoCobrador(),codigoSucursal()));}catch(Exception e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}}
    @PostMapping("/dispositivos/{id}/estado") @ResponseBody
    public ResponseEntity<?> cambiarEstadoDispositivo(@PathVariable String id,
            @RequestBody EstadoDispositivoSolicitud solicitud) {
        if (solicitud == null || solicitud.activo() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "Debe indicar el estado del dispositivo"));
        }
        try {
            return ResponseEntity.ok(recorridoService.cambiarEstadoDispositivo(
                    id, solicitud.activo(), codigoSucursal()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @GetMapping("/{codigoRecorrido}/puntos")
    @ResponseBody
    public List<Map<String, Object>> puntos(@PathVariable Long codigoRecorrido) {
        return recorridoService.listarPuntos(codigoRecorrido, codigoSucursal());
    }

    @GetMapping("/{codigoRecorrido}/puntos-pagina") @ResponseBody
    public Map<String,Object> puntosPagina(@PathVariable Long codigoRecorrido,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="100") int tamano){return recorridoService.listarPuntosPagina(codigoRecorrido,codigoSucursal(),pagina,tamano);}
    @GetMapping("/{codigoRecorrido}/trazo") @ResponseBody
    public List<Map<String,Object>> trazo(@PathVariable Long codigoRecorrido){return recorridoService.listarTrazoLimitado(codigoRecorrido,codigoSucursal());}

    @GetMapping("/{codigoRecorrido}/permanencias")
    @ResponseBody
    public List<Map<String, Object>> permanencias(@PathVariable Long codigoRecorrido) {
        return recorridoService.listarPermanencias(codigoRecorrido, codigoSucursal());
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> crear(@RequestBody CrearRecorridoSolicitud solicitud,
            @RequestHeader(name = "X-Origen-Cliente", defaultValue = "WEB") String origen) {
        if (solicitud == null || solicitud.codigoCobrador() == null) {
            return ResponseEntity.badRequest().body("Seleccione un cobrador");
        }
        try {
            return ResponseEntity.ok(recorridoService.crear(
                    solicitud.codigoCobrador(), solicitud.observacion(),
                    usuarioActual().getCodigoUsuarioSistema(), codigoSucursal(), origen));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{codigoRecorrido}/iniciar")
    @ResponseBody
    public ResponseEntity<?> iniciar(@PathVariable Long codigoRecorrido,
            @RequestHeader(name = "X-Origen-Cliente", defaultValue = "WEB") String origen) {
        try {
            return ResponseEntity.ok(recorridoService.iniciar(
                    codigoRecorrido, usuarioActual().getCodigoUsuarioSistema(),
                    codigoSucursal(), origen));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{codigoRecorrido}/finalizar")
    @ResponseBody
    public ResponseEntity<?> finalizar(@PathVariable Long codigoRecorrido,
            @RequestHeader(name = "X-Origen-Cliente", defaultValue = "WEB") String origen) {
        try {
            return ResponseEntity.ok(recorridoService.finalizar(
                    codigoRecorrido, usuarioActual().getCodigoUsuarioSistema(),
                    codigoSucursal(), origen));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{codigoRecorrido}/puntos")
    @ResponseBody
    public ResponseEntity<?> registrarPunto(@PathVariable Long codigoRecorrido,
            @RequestBody PuntoRecorridoSolicitud solicitud,
            @RequestHeader(name = "X-Origen-Cliente", defaultValue = "APP") String origen) {
        try {
            boolean registrado = recorridoService.registrarPunto(codigoRecorrido,
                    solicitud.idSincronizacion(),
                    solicitud.latitud(), solicitud.longitud(),
                    solicitud.precisionMetros(), solicitud.velocidadMetrosSegundo(),
                    solicitud.fechaDispositivo(), codigoSucursal(), origen);
            return ResponseEntity.ok(Map.of(
                    "mensaje", registrado ? "Punto registrado" : "Punto ya sincronizado",
                    "registrado", registrado));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private UsuarioSistema usuarioActual() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Integer codigoSucursal() {
        return usuarioActual().getSucursal().getCodigoSucursal();
    }

    public record CrearRecorridoSolicitud(Integer codigoCobrador, String observacion) {}
    public record AsignarDispositivoSolicitud(Integer codigoCobrador) {}
    public record EstadoDispositivoSolicitud(Boolean activo) {}
    public record PuntoRecorridoSolicitud(String idSincronizacion,
            BigDecimal latitud, BigDecimal longitud,
            BigDecimal precisionMetros, BigDecimal velocidadMetrosSegundo,
            LocalDateTime fechaDispositivo) {}
}
