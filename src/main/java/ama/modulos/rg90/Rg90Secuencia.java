package ama.modulos.rg90;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rg90_secuencias")
public class Rg90Secuencia {

    @Id
    @Column(name = "clave", length = 120)
    private String clave;

    @Basic(optional = false)
    @Column(name = "siguiente_lote", nullable = false)
    private Integer siguienteLote;
}
