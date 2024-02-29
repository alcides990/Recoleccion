package ama.servicio;

import ama.dominio.Servicio;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicioService {

    public Page<Servicio> listar(Pageable pageable);

    public Page<Servicio> buscar(Pageable pageable, String filtro);


    public List<String> listaServicioCuenta(Servicio servicio);

    public void guardar(Servicio servicio);

    public void eliminar(Servicio servicio);

    public Servicio encontrar(String cuentaCorriente);

}
