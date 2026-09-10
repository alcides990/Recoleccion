package ama.modulos.egresos;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {EgresoController.class, EgresoReporteController.class})
public class EgresoErrores {
    @ExceptionHandler(EgresoException.class)
    public ResponseEntity<EgresoController.Mensaje> negocio(EgresoException error) {
        HttpStatus estado = switch (error.getMotivo()) {
            case DATOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case SIN_DATOS -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EgresoController.Mensaje(error.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<EgresoController.Mensaje> sesion(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EgresoController.Mensaje(error.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<EgresoController.Mensaje> integridad() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EgresoController.Mensaje(
                "El registro ya existe, tiene gastos asociados o los datos no son válidos."));
    }
}
