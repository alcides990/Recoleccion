package ama;

import ama.dao.PuntoExpedicionDao;
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

    @Autowired
    PuntoExpedicionDao puntoExpedicionDao;
//    @Autowired
//    ComprobanteDao comprobanteDao;

    public static void main(String[] args) {

        SpringApplication.run(recolecion.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
//        var resul = puntoExpedicionDao.findAll();
//       resul.forEach(c->
//        System.out.println("manzana esta en nulo : " + resul);
//        System.out.println("manzana : " + resul);
//       );

//var resul= comprobanteDao.getAllComprobantes(pageable);
    }

}
