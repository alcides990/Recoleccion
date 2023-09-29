package ama.errores;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ClaseError extends RuntimeException {

    private static String mensajeError;

    public ClaseError(String mensje, Throwable ex) {

        super(mensje, ex);
    }

    public static ClaseError excepcion(String mensaje, Throwable ex) {
        mensajeError = mensaje;
        Throwable rootCause = ((DataAccessException) ex).getRootCause();
        if (ex instanceof DataAccessException) {

            if (((DataAccessException) ex).getRootCause() instanceof SQLIntegrityConstraintViolationException) {
                SQLIntegrityConstraintViolationException sqlEx = (SQLIntegrityConstraintViolationException) rootCause;
                String sqlState = sqlEx.getSQLState();

                if (sqlEx.getErrorCode() == 1062) {
                    throw new ClaseError(" registro ya se encuentra en la base de datos", ex);
                } else if (sqlEx.getErrorCode() == 1451) {
                    mensajeError += ", se encuentra asociado con otro resgistro!";
                }
            }
            throw new ClaseError(mensajeError, ex);

        } else if (ex instanceof SQLException) {
            mensajeError += "Error de SQL al insertar en la base de datos. ";
            throw new ClaseError(mensajeError, ex);
        } else {
            mensajeError += "Error general al insertar en la base de datos. ";
            throw new ClaseError(mensajeError, ex);
        }
    }

}
