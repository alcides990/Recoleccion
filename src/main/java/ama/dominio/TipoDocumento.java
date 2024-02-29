/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipos_documento")
public class TipoDocumento implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_tipo_documento")
    private Integer codigoTipoDocumento;
    @Column(name = "tipo_documento")
    private String tipoDocumento;

}
