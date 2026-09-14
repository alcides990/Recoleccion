package ama.controladorMVC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/documentacion")
public class DocumentacionController {

    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @GetMapping
    public String inicio(Model model) {
        model.addAttribute("titulo", "Ayuda y documentación");
        return "documentacion/inicio";
    }

    @GetMapping("/manual/pdf")
    public ResponseEntity<Resource> verManual() throws IOException {
        return responder("documentacion/manual-de-usuario.pdf",
                "manual-de-usuario.pdf", MediaType.APPLICATION_PDF, true);
    }

    @GetMapping("/manual/word")
    public ResponseEntity<Resource> descargarManual() throws IOException {
        return responder("documentacion/manual-de-usuario.docx",
                "manual-de-usuario.docx", DOCX_MEDIA_TYPE, false);
    }

    @PreAuthorize("hasAuthority('ROOT')")
    @GetMapping("/tecnica/pdf")
    public ResponseEntity<Resource> verDocumentacionTecnica() throws IOException {
        return responder("documentacion/documentacion-tecnica-sistema.pdf",
                "documentacion-tecnica-sistema.pdf", MediaType.APPLICATION_PDF, true);
    }

    @PreAuthorize("hasAuthority('ROOT')")
    @GetMapping("/tecnica/word")
    public ResponseEntity<Resource> descargarDocumentacionTecnica() throws IOException {
        return responder("documentacion/documentacion-tecnica-sistema.docx",
                "documentacion-tecnica-sistema.docx", DOCX_MEDIA_TYPE, false);
    }

    private ResponseEntity<Resource> responder(String ruta, String nombre,
            MediaType mediaType, boolean mostrarEnNavegador) throws IOException {
        Resource recurso = new ClassPathResource(ruta);
        ContentDisposition disposicion = (mostrarEnNavegador
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(nombre, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(recurso.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(recurso);
    }
}
