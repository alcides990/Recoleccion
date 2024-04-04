package ama.utilerias;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Reporte {

    private String nombre;
    private String ruta;
    private Map<String, Object> parametros;

}
