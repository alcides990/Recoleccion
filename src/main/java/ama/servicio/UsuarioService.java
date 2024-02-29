package ama.servicio;

import ama.dominio.Usuario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    public List<Usuario> listar();
    
    public Page<Usuario> listar(Pageable pageable, String filtro);
    
    public List<Usuario> Buscar(String filtro);
    
    public void guardar(Usuario usuario);

    public void eliminar(Usuario usuario);

    public Usuario encontrar(Usuario usuario);

    public Integer getCodigoUsuario();
}
