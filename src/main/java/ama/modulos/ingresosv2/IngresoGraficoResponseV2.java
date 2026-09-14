package ama.modulos.ingresosv2;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngresoGraficoResponseV2 {
    private String periodo;
    private List<IngresoPeriodoV2> ingresos;
    private List<IngresoCobradorV2> ingresosPorCobrador;
    private List<IngresoMedioPeriodoProjectionV2> ingresosPorMedioPago;
    private Double total;
    private Double promedio;
    private Double maximo;
}
