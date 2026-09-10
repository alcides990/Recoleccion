package ama.controladorMVC;

import ama.dominio.UsuarioSistema;
import ama.servicio.RecorridoCobradorService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movil/seguimiento")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SeguimientoMovilController {

    private final HttpSession httpSession;
    private final RecorridoCobradorService recorridoCobradorService;

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody Dispositivo solicitud) {
        return ResponseEntity.ok(recorridoCobradorService.registrarDispositivo(
                solicitud.idDispositivo(),
                solicitud.nombreDispositivo(),
                codigoSucursalSesion()));
    }

    @GetMapping("/activo")
    public ResponseEntity<Map<String, Object>> activo(@RequestParam String idDispositivo) {
        return ResponseEntity.ok(recorridoCobradorService.recorridoActivoDispositivo(
                idDispositivo,
                codigoSucursalSesion()));
    }

    @PostMapping("/{recorrido}/puntos")
    public ResponseEntity<Map<String, Boolean>> registrarPunto(
            @PathVariable Long recorrido,
            @RequestBody Punto solicitud) {
        boolean registrado = recorridoCobradorService.registrarPuntoDispositivo(
                solicitud.idDispositivo(),
                recorrido,
                solicitud.idSincronizacion(),
                solicitud.latitud(),
                solicitud.longitud(),
                solicitud.precisionMetros(),
                solicitud.velocidadMetrosSegundo(),
                solicitud.fechaDispositivo(),
                codigoSucursalSesion());

        return ResponseEntity.ok(Map.of("registrado", registrado));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> solicitudInvalida(RuntimeException excepcion) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", excepcion.getMessage()));
    }

    private Integer codigoSucursalSesion() {
        UsuarioSistema usuario = (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
        return usuario.getSucursal().getCodigoSucursal();
    }

    public record Dispositivo(String idDispositivo, String nombreDispositivo) {
    }

    public record Punto(
            String idDispositivo,
            String idSincronizacion,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal precisionMetros,
            BigDecimal velocidadMetrosSegundo,
            LocalDateTime fechaDispositivo) {
    }
}

