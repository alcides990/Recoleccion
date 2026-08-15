package ama.facturaelectronica;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/factura-electronica")
public class FacturaElectronicaController {
    private final FacturaElectronicaService service;
    public FacturaElectronicaController(FacturaElectronicaService service) { this.service = service; }

    @GetMapping("/estado")
    public Map<String,Object> estado(@RequestParam Integer sucursal, @RequestParam Integer punto,
            @RequestParam Integer tipo, @RequestParam Integer serie, @RequestParam Integer numero) {
        return service.estado(clave(sucursal,punto,tipo,serie,numero));
    }

    @PostMapping(value="/enviar", consumes={MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
    public ResponseEntity<?> enviar(@RequestParam Integer sucursal, @RequestParam Integer punto,
            @RequestParam Integer tipo, @RequestParam Integer serie, @RequestParam Integer numero,
            @RequestBody String xml) {
        var resultado = service.enviarXml(clave(sucursal,punto,tipo,serie,numero), xml);
        return resultado.aprobado() ? ResponseEntity.ok(resultado)
                : ResponseEntity.unprocessableEntity().body(resultado);
    }

    @GetMapping("/documento/{documento:xml|respuesta|kude}")
    public ResponseEntity<?> documento(@PathVariable String documento,
            @RequestParam Integer sucursal, @RequestParam Integer punto,
            @RequestParam Integer tipo, @RequestParam Integer serie, @RequestParam Integer numero) {
        try {
            byte[] contenido = service.documento(clave(sucursal,punto,tipo,serie,numero), documento);
            MediaType tipoContenido = "kude".equals(documento) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_XML;
            String extension = "kude".equals(documento) ? ".pdf" : ".xml";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.inline().filename(documento + "-" + numero + extension).build());
            return ResponseEntity.ok().headers(headers).contentType(tipoContenido).contentLength(contenido.length).body(contenido);
        } catch (Exception ex) {
            return ResponseEntity.status(404).body(Map.of("mensaje", ex.getMessage()));
        }
    }

    private ClaveComprobanteElectronico clave(Integer sucursal,Integer punto,Integer tipo,Integer serie,Integer numero) {
        return new ClaveComprobanteElectronico(sucursal,punto,tipo,serie,numero);
    }
}
