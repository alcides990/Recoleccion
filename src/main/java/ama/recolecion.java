package ama;

import ama.dao.DetalleUsuarioSistemaDao;
import ama.servicio.UsuarioSistemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@SpringBootApplication
@Slf4j
public class recolecion implements CommandLineRunner {
// @Autowired
//    private  BCryptPasswordEncoder encoder;

    @Autowired
    UsuarioSistemaService usuarioSistemaService;
    @Autowired
    DetalleUsuarioSistemaDao detalleUsuarioSistemaDao;

    public static void main(String[] args) {

        SpringApplication.run(recolecion.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
//        var resul = usuarioSistemaDao.findByNombre("irene");
//       resul.forEach(c->{
//        System.out.println("manzana esta en nulo : " + resul);
//        System.out.println("manzana : " + resul);
//    });
//        var servicio=new Servicio();
//        servicio.setCuentaCorriente("24-0001-01");
//        var comprobante=comprobanteDao.getAllComprobantes(pageable);
//        resul.getDetalleUsuarioSistema().forEach(dtu
//                -> {
//        log.info("Total: " +  dtu.getRol());
//        log.info("Total: " +  dtu.getUsuarioSistema().getNombre()); 
//        });
//        comprobante.forEach(System.out::println);

//       
    }

}
