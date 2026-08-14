package ama.modulos.comprobantesv2;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetallePagoV2 {
    @NotNull
    private Integer codigoMetodoPago;
    private String metodoPago;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal importe;
}
