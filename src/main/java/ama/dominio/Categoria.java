package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.reflect.Field;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Entity
@Table(name = "categorias")
public class Categoria implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_categoria")
    private Integer codigoCategoria;
    
    @NotEmpty(message = "Nombre de la categoria no puede estar vacio")
    @Column(name = "categoria")
    private String nombreCategoria;

    @NotNull(message = "Tarifa no puede estar vacio")
    @Min(value = 1, message = "El monto de la tarifa debe ser mayor a cero")
    private Double tarifa;
    
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull
    private Sucursal sucursal;

    public Categoria() {
    }

    
    
    @Transient
    private String nombreEstado;

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    @PrePersist
    @PreUpdate
    private void getMayuscula() {
        Class clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(String.class)) {
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(this);
                    if (value != null) {
                        field.set(this, value.toUpperCase());
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    @Override
    public String toString() {
        return "Categoria{" + "codigoCategoria=" + codigoCategoria + ", nombreCategoria=" + nombreCategoria + ", tarifa=" + tarifa + ", nombreEstado=" + nombreEstado + '}';
    }
    
    

}
