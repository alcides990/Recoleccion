/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ama.dominio;

import ama.dominio.Estado;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;
@Data
@Entity
@Table(name = "puntoexpedicion")
public class PuntoExpedicion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_punto_expedicion")
    private Integer codigoPuntoExpedicion;
    @Column(name = "punto_expedicion")
    private String nombrePuntoExpedicion;
    @JoinColumn(name = "codigo_empresa", referencedColumnName = "codigo_empresa")
    @ManyToOne(fetch = FetchType.LAZY)
    private Empresa empresa;
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(fetch = FetchType.LAZY)
    private Estado estado;
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(fetch = FetchType.LAZY)
    private Sucursal sucursal;
  
}
