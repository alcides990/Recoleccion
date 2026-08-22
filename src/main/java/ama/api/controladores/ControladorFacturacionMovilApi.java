package ama.api.controladores;

import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.ComprobanteService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
