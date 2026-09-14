package ama.modulos.ingresosv2;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngresoComparativoResponseV2 {
    private String escala;
    private String agrupacion;
    private LocalDate desdeA;
    private LocalDate hastaA;
    private LocalDate desdeB;
    private LocalDate hastaB;
    private List<IngresoComparativoFilaV2> filas;
}
