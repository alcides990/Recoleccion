 
package ama.controladorMVC;
 
import ama.dominio.Sucursal;
import ama.servicio.ServicioSucursal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public class ControladorApi {
    
    @Autowired
    private ServicioSucursal servicioSucursal;

//    @GetMapping("/sucursal")
//    @ResponseBody
//    public List<Sucursal> listaSucursal() {
//        var sucursales = servicioSucursal.listarSucursal();
//        var sucursaleSDTO = new ArrayList();
//        for (Sucursal sucursal : sucursales) {
//        var sucursalDto=new sucursalDTO();
//            sucursalDto.setCodigoSucursal(sucursal.getCodigoSursal());
//            sucursalDto.setSucursal(sucursal.getSucursal());
//            sucursalDto.setCiudad(sucursal.getCiudad().getCiudad());
//            sucursalDto.setEmpresa(sucursal.getEmpresa().getEmpresa());
//            sucursaleSDTO.add(sucursalDto);
//        }
//        return sucursales;

//        log.info("Ejecutando Controlador Spring MVC");
////        model.addAttribute("personas", personas);
//        return "index";
//    }

    @GetMapping("/agregar")
    public String agregar() {
        return "modificar";
    }

    @PostMapping("/guardar")
    public ResponseEntity<Sucursal> guardar(@RequestBody Sucursal sucursal) {
        Sucursal sucu= servicioSucursal.guardar(sucursal);
        return new ResponseEntity<Sucursal> (sucu, HttpStatus.OK);
    }
//    
//    @GetMapping("/editar/{idPersona}")
//    public String editar(Persona persona, Model model){
//        persona = servicioPersona.encontrarPersona(persona);
//        model.addAttribute("persona", persona);
//        return "modificar";
//    }
//    
    @GetMapping("/eliminar/{id}")
    public ResponseEntity<Sucursal> eliminar(@PathVariable Integer id)  {
        Sucursal sucu=servicioSucursal.encontrar(new Sucursal(id));
        if(sucu !=null){
            servicioSucursal.eliminar(sucu);
        }else{
        return new ResponseEntity<Sucursal>(sucu, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }
    
}
