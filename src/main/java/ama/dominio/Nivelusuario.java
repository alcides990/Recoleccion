/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 *
 * @author Alcides
 */
@Entity
@Table(name = "nivelusuarios")
public class Nivelusuario implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_nivel_usuario")
    private Integer codigoNivelUsuario;
    @Column(name = "nivel_usuario")
    private String nivelUsuario;
}
