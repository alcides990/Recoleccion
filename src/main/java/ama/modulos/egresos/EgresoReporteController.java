package ama.modulos.egresos;

import java.io.IOException;
import java.time.LocalDate;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/egresos/reporte")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR','SECRETARIO')")
public class EgresoReporteController {
    private final EgresoReporteService reportes;
    private final EgresoSesion sesion;

    public EgresoReporteController(EgresoReporteService reportes, EgresoSesion sesion) {
        this.reportes = reportes;
        this.sesion = sesion;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> reporte(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "RESUMIDO") TipoReporteEgreso formato,
            @RequestParam(required = false) Long categoriaId)
            throws JRException, IOException {
        var contexto = sesion.actual();
        byte[] contenido = reportes.generar(contexto.sucursal(), contexto.nombreSucursal(), desde, hasta, formato, categoriaId);
        String archivo = "egresos-" + formato.name().toLowerCase() + "-" + desde + "-" + hasta + ".pdf";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(archivo).build().toString())
                .body(contenido);
    }
}
