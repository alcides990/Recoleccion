package ama.modulos.comprobantesv2;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ComprobanteFiltroV2 {
    private Integer sucursal;
    private Integer puntoExpedicion;
    private Integer serie;
    private Integer numero;
    private Integer estado;
    private String cuenta;
    private String documento;
    private String nombre;
    private LocalDate desde;
    private LocalDate hasta;
    private int offset;
    private int limit = 10;
    private String orden = "fecha_pago";
    private String direccion = "desc";
}
