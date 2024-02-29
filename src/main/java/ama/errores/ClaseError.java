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

    public ClaseError(String mensje, Throwable e) {

        super(mensje, e);
    }

    public static ClaseError excepcion(String mensaje, Throwable e) {
        mensajeError = mensaje;
        Throwable rootCause = ((DataAccessException) e).getRootCause();
        if (e instanceof DataAccessException) {

            if (((DataAccessException) e).getRootCause() instanceof SQLIntegrityConstraintViolationException) {
                SQLIntegrityConstraintViolationException sqlEx = (SQLIntegrityConstraintViolationException) rootCause;
                String sqlState = sqlEx.getSQLState();

                if (sqlEx.getErrorCode() == 1062) {
                    throw new ClaseError(" registro ya se encuentra en la base de datos", e);
                } else if (sqlEx.getErrorCode() == 1451) {
                    mensajeError += ", se encuentra asociado con otro resgistro!";
                }
            }
            throw new ClaseError(mensajeError, e);

        } else if (e instanceof SQLException) {
            mensajeError += "Error de SQL al insertar en la base de datos. ";
            throw new ClaseError(mensajeError, e);
        } else {
            mensajeError += "Error general al insertar en la base de datos. ";
            throw new ClaseError(mensajeError, e);
        }
    }

}
