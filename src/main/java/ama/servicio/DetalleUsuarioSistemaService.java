package ama.servicio;

import ama.dominio.DetalleUsuarioSistema;
import ama.dominio.DetalleUsuarioSistemaPK;
import ama.dominio.UsuarioSistema;
import java.util.List;

public interface DetalleUsuarioSistemaService {

    public List<DetalleUsuarioSistema> listar(UsuarioSistema suaroSistema);


    public void guardar(DetalleUsuarioSistema detalleUsuaroSistema);

    public void eliminar(DetalleUsuarioSistemaPK detalleUsuaroSistemaPK);


   
}
