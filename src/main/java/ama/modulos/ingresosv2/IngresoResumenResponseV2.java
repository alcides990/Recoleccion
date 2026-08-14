package ama.modulos.ingresosv2;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngresoResumenResponseV2 {
    private LocalDate desde;
    private LocalDate hasta;
    private Double total;
    private Long comprobantes;
    private List<IngresoResumenProjectionV2> porCobrador;
    private List<IngresoResumenProjectionV2> porMedioPago;
    private List<IngresoCobradorMedioProjectionV2> detallePorCobrador;
}
