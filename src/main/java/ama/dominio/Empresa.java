package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "empresas")
public class Empresa implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_empresa")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer codigoEmpresa;
    private String empresa;
    private String telefono;
    private String celular;
    private String ruc;
    private String direccion;
 
     

}
