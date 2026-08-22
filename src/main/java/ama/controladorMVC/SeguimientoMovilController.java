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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movil/seguimiento")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SeguimientoMovilController {
    private final HttpSession session; private final RecorridoCobradorService service;
    @PostMapping("/registrar") public ResponseEntity<?> registrar(@RequestBody Dispositivo r){try{return ResponseEntity.ok(service.registrarDispositivo(r.idDispositivo(),r.nombreDispositivo(),sucursal()));}catch(Exception e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}}
    @GetMapping("/activo") public ResponseEntity<?> activo(@RequestParam String idDispositivo){try{return ResponseEntity.ok(service.recorridoActivoDispositivo(idDispositivo,sucursal()));}catch(Exception e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}}
    @PostMapping("/{recorrido}/puntos") public ResponseEntity<?> punto(@PathVariable Long recorrido,@RequestBody Punto r){try{boolean ok=service.registrarPuntoDispositivo(r.idDispositivo(),recorrido,r.idSincronizacion(),r.latitud(),r.longitud(),r.precisionMetros(),r.velocidadMetrosSegundo(),r.fechaDispositivo(),sucursal());return ResponseEntity.ok(Map.of("registrado",ok));}catch(Exception e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}}
    private Integer sucursal(){return ((UsuarioSistema)session.getAttribute("usuarioSistema")).getSucursal().getCodigoSucursal();}
    public record Dispositivo(String idDispositivo,String nombreDispositivo){}
    public record Punto(String idDispositivo,String idSincronizacion,BigDecimal latitud,BigDecimal longitud,BigDecimal precisionMetros,BigDecimal velocidadMetrosSegundo,LocalDateTime fechaDispositivo){}
}
