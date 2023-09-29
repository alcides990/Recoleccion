/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;

@Entity
@Table(name = "metodospagos")
public class MetodoPago implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_metodo_pago")
    private Integer cdigoMetodoPago;
    @Column(name = "metodo_pago")
    private String metodoPago;

    public MetodoPago() {
    }

    public MetodoPago(Integer cdigoMetodoPago) {
        this.cdigoMetodoPago = cdigoMetodoPago;
    }
    
    
}
