package ama.controladorMVC;

import ama.dominio.Manzana;
import ama.dominio.ManzanaPK;
import ama.errores.ClaseError;
import ama.servicio.ServicioCiudad;
import ama.servicio.ServicioCobrador;
import ama.servicio.ServicioDetalleZona;
import ama.servicio.ServicioManzana;
import ama.servicio.ServicioSucursal;
import ama.servicio.ServicioZona;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/manzana")
public class ManzanaController {

    @Autowired
    private ServicioManzana servicioManzana;
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

    @PostMapping("/listar")
    public ResponseEntity<?> listaDetalleZonaPorZona(@RequestBody Manzana manzana) {
        return ResponseEntity.ok(servicioManzana.listar(manzana.getCobrador(), manzana.getZona()));
    }

//    @GetMapping("/agregar/{codigoZona}")
//    public String agregar(Zona zona, Model model) {
//        model.addAttribute("titulo", "Detalle Zona");
//
//        Zona zonaEcontrada = servicioZona.encontrar(zona);
//        model.addAttribute("zona", zonaEcontrada);
//
//        model.addAttribute("sucursal", zonaEcontrada.getSucursal());
//
//        model.addAttribute("detalleZona", servicioDetalleZona.listar(zona));
//
//        model.addAttribute("cobradores", servicioCobrador.listar());
//
//        return "detalleZona/agregarDetalleZona";
//    }
//
    @PostMapping("/guardar")
    public ResponseEntity<String> guardar(Manzana manzana,
            @RequestParam int numeroManzana, Model model) {
        ManzanaPK manzanaPK = new ManzanaPK();
        manzanaPK.setCodigoSucursal(manzana.getSucursal().getCodigoSucursal());
        manzanaPK.setNumeroManzana(numeroManzana);
        manzana.setManzanaPK(manzanaPK);
        Manzana manzanaRecuperada = servicioManzana.encontrar(manzanaPK);
        if (manzanaRecuperada != null) {
            String mensaje = "Manzana N° " + manzanaPK.getNumeroManzana() + " ya se encueantra registrada en zona "+
                    manzanaRecuperada.getZona().getNombreZona();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }

        try {
            servicioManzana.guardar(manzana);
            return ResponseEntity.ok("Manzana agregada correctamente!!");

        } catch (DataAccessException e) {
            String mensaje = "Error al agregar manzana " + e.getMostSpecificCause().getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }

    }
    @Secured("hasAuthority('ADMIN')")
    @PostMapping("/eliminar/{numeroManzana}/{codigoSucursal}")
    public ResponseEntity<String> eliminar(ManzanaPK manzanaPK) {
        try {
            servicioManzana.eliminar(new Manzana(manzanaPK));
            return ResponseEntity.ok().body("Registro Eliminado Correctamente!!");

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al Elimar Registro: " + ClaseError.excepcion("Error al eliminar manzana, ", e));
        }
    }
}
