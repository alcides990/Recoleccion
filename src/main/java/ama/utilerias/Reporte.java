package ama.utilerias;

import java.sql.Connection;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Reporte {

    Connection conexion;
    private String nombre;
    private String ruta;
    private Map<String, Object> parametros;

}
