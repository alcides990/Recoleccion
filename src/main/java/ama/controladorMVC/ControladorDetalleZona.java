package ama.controladorMVC;

import ama.dominio.Cobrador;
import ama.dominio.DetalleZona;
import ama.dominio.DetalleZonaPK;
import ama.dominio.Zona;
import ama.errores.ClaseError;
import ama.servicio.ServicioCiudad;
import ama.servicio.ServicioCobrador;
import ama.servicio.ServicioDetalleZona;
import ama.servicio.ServicioSucursal;
import ama.servicio.ServicioZona;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/detalleZona")
public class ControladorDetalleZona {

    @Autowired
    private ServicioDetalleZona servicioDetalleZona;
    @Autowired
    private ServicioCobrador servicioCobrador;
    @Autowired
    private ServicioZona servicioZona;
    @Autowired
    private ServicioSucursal servicioSucursal;
    @Autowired
    private ServicioCiudad servicioCiudad;

    @GetMapping("/listar/{codigoZona}")
    public String listaDetalleZonaPorZona(Zona zona, Model modelo) {
        modelo.addAttribute("titulo", "Detalle Zona");

        modelo.addAttribute("detalleZona", servicioDetalleZona.listar(zona));

        return "detalleZona/detalleZona";
    }

    @GetMapping("/agregar/{codigoZona}")
    public String agregar(Zona zona, Model model) {
        model.addAttribute("titulo", "Detalle Zona");

        Zona zonaEcontrada = servicioZona.encontrar(zona);
        model.addAttribute("zona", zonaEcontrada);

        model.addAttribute("sucursal", zonaEcontrada.getSucursal());

        model.addAttribute("detalleZona", servicioDetalleZona.listar(zona));

        model.addAttribute("cobradores", servicioCobrador.listar());

        return "detalleZona/agregarDetalleZona";
    }

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(DetalleZona detalleZona) {
        DetalleZonaPK detalleZonaPK = new DetalleZonaPK();
        detalleZonaPK.setCodigoCobrador(detalleZona.getCobrador().getCodigoCobrador());
        detalleZonaPK.setCodigoZona(detalleZona.getZona().getCodigoZona());
        detalleZonaPK.setCodigoSucursal(detalleZona.getSucursal().getCodigoSucursal());
        detalleZona.setDetalleZonaPK(detalleZonaPK);

        DetalleZona dtz = servicioDetalleZona.encontrar(detalleZona);
        if (dtz != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(dtz.getCobrador().getNombreCompleto() + " YA ESTA ASIGNADA EN ESTA ZONA");
        }
        try {
            servicioDetalleZona.guardar(detalleZona);
            return ResponseEntity.ok("Cobrador agregado corectamente ");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ClaseError.excepcion("Error asignar cobrador ", e));
        }

    }

    @PostMapping("/modificar")
    public ResponseEntity<String> modificar(DetalleZona detalleZona,
            @RequestParam int codigoCobradorActual) {
        String mensaje = "";
        Cobrador cobradorNuevo = detalleZona.getCobrador();

        DetalleZonaPK detalleZonaPK = new DetalleZonaPK();
        detalleZonaPK.setCodigoCobrador(codigoCobradorActual);
        detalleZonaPK.setCodigoZona(detalleZona.getZona().getCodigoZona());
        detalleZonaPK.setCodigoSucursal(detalleZona.getSucursal().getCodigoSucursal());
        detalleZona.setDetalleZonaPK(detalleZonaPK);
        try {
            servicioDetalleZona.modificar(detalleZonaPK, cobradorNuevo);
            return ResponseEntity.ok("Cobrador de la zona modificado!!");
        } catch (DataAccessException e) {
            mensaje = "Error al cambiar cobrador " + e.getMostSpecificCause().getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error " + e.getMessage());
        }
    }

    @PostMapping("/editar")
    public ResponseEntity<?> editar(@RequestBody DetalleZonaPK detalleZonaPK) {
        DetalleZona detalleZona = new DetalleZona(detalleZonaPK);
        detalleZona.setZona(new Zona(detalleZonaPK.getCodigoZona()));
        detalleZona.setCobrador(new Cobrador(detalleZonaPK.getCodigoCobrador()));
        DetalleZona detalleZonaRecuperado = servicioDetalleZona.encontrar(detalleZona);
        return ResponseEntity.ok(detalleZonaRecuperado);
    }

    @PostMapping("/encontrar")
    @ResponseBody
    public ResponseEntity<?> encontrar(@RequestBody DetalleZona detalleZona) {
        DetalleZona detalleZonaRecuperado = servicioDetalleZona.encontrar(detalleZona);
        return ResponseEntity.ok(detalleZonaRecuperado);
    }

//    @GetMapping("/editar/{codigoZona}/{codigoCobrador}")
//    public String editar(DetalleZonaPK detalleZonaPK, DetalleZona detalleZona, Model modelo) {
//        modelo.addAttribute("titulo", "Detalle Zona");
//        detalleZona = new DetalleZona(detalleZonaPK);
//        detalleZona.setZona(new Zona(detalleZonaPK.getCodigoZona()));
//        detalleZona.setCobrador(new Cobrador(detalleZonaPK.getCodigoCobrador()));
//        DetalleZona detalleZonaRecuperado = servicioDetalleZona.encontrar(detalleZona);
//
//        Zona zona = detalleZonaRecuperado.getZona();
//        modelo.addAttribute("zona", zona);
//
//        modelo.addAttribute("cobradores", servicioCobrador.listar());
//
//        modelo.addAttribute("codigoCobradorActual", detalleZonaRecuperado.getCobrador().getCodigoCobrador());
//
//        modelo.addAttribute("sucursal", zona.getSucursal());
//
//        return "detalleZona/modificarDetalleZona";
//    }
    @PostMapping("/eliminar/{codigoCobrador}/{codigoZona}/{codigoSucursal}")
    public ResponseEntity<String> eliminar(DetalleZonaPK detalleZonaPK) {
        try {
            servicioDetalleZona.eliminar(detalleZonaPK);
            return ResponseEntity.ok().body("Registro Eliminado Correctamente!!");

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al Elimar Registro: " + ClaseError.excepcion("Error al eliminar manzana. ", e));
        }
    }
}
