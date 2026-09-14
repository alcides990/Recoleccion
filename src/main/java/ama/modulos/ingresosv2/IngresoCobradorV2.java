package ama.modulos.ingresosv2;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngresoCobradorV2 {
    private String cobrador;
    private Double importe;
    private Long cantidad;
}
