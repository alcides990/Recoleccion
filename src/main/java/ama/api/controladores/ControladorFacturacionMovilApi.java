package ama.api.controladores;

import ama.DTO.NumeroComprobanteDTO;
import ama.dominio.ComprobantePK;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.ComprobanteService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador API para operaciones de facturación móvil
 * Proporciona endpoints específicos para la aplicación móvil Android
 */
@Slf4j
@RestController
@RequestMapping("/api/movil/facturacion")
public class ControladorFacturacionMovilApi {

    @Autowired
    private ComprobanteService comprobanteService;

    @Autowired
    private HttpSession httpSession;

    /**
     * Obtiene el siguiente número de comprobante disponible
     * Endpoint específico para móvil que devuelve estructura JSON completa
     * 
     * @param comprobantePK Información del comprobante (punto expedición, tipo, serie)
     * @return NumeroComprobanteDTO con todos los detalles del número asignado
     */
    @PostMapping("/numero-siguiente")
    @ResponseBody
    public ResponseEntity<?> obtenerNumeroSiguiente(@RequestBody ComprobantePK comprobantePK) {
        try {
            if (comprobantePK == null || comprobantePK.getPuntoExpedicionPK() == null) {
                return ResponseEntity.badRequest()
                        .body(new NumeroComprobanteDTO(
                                null, null, null, null, null, false,
                                "Datos incompletos: punto de expedición requerido"
                        ));
            }

            Sucursal sucursal = getSucursalSession();
            if (sucursal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new NumeroComprobanteDTO(
                                null, null, null, null, null, false,
                                "Sesión expirada: autentifique nuevamente"
                        ));
            }

            // Establecer código de sucursal desde la sesión
            PuntoExpedicionPK puntoExpedicionPK = comprobantePK.getPuntoExpedicionPK();
            puntoExpedicionPK.setCodigoSucursal(sucursal.getCodigoSucursal());
            comprobantePK.setPuntoExpedicionPK(puntoExpedicionPK);

            // Obtener siguiente número de comprobante
            Integer numeroComprobante = comprobanteService.getNumeroComprobante(comprobantePK);

            if (numeroComprobante == null || numeroComprobante <= 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new NumeroComprobanteDTO(
                                null, 
                                puntoExpedicionPK.getCodigoPuntoExpedicion(),
                                comprobantePK.getCodigoTipoComprobante(),
                                comprobantePK.getCodigoSerie(),
                                sucursal.getCodigoSucursal(),
                                false,
                                "No hay número de comprobante disponible para este punto de expedición"
                        ));
            }

            // Construir respuesta exitosa
            NumeroComprobanteDTO response = NumeroComprobanteDTO.builder()
                    .numeroComprobante(numeroComprobante)
                    .codigoPuntoExpedicion(puntoExpedicionPK.getCodigoPuntoExpedicion())
                    .codigoTipoComprobante(comprobantePK.getCodigoTipoComprobante())
                    .codigoSerie(comprobantePK.getCodigoSerie())
                    .codigoSucursal(sucursal.getCodigoSucursal())
                    .disponible(true)
                    .mensaje("Número de comprobante asignado correctamente")
                    .build();

            log.info("Número de comprobante generado: {} para punto: {}", 
                    numeroComprobante, puntoExpedicionPK.getCodigoPuntoExpedicion());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener número de comprobante", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new NumeroComprobanteDTO(
                            null, null, null, null, null, false,
                            "Error al procesar solicitud: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtiene la información de la sesión del usuario
     */
    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    /**
     * Obtiene la sucursal de la sesión actual
     */
    private Sucursal getSucursalSession() {
        UsuarioSistema usuarioSistema = getUserSession();
        return usuarioSistema != null ? usuarioSistema.getSucursal() : null;
    }
}
