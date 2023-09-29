package ama.servicios.implementaciones;

import ama.dao.UsuarioDao;
import ama.dominio.Usuario;
import ama.servicio.ServicioUsuario;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImplUsuario implements ServicioUsuario {

    @Autowired
    UsuarioDao usuarioDao;

    @Transactional(readOnly = true)
    @Override
    public List<Usuario> listar() {
        return (List<Usuario>) usuarioDao.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Usuario> listar(Pageable pageable, String filtro) {
        return usuarioDao.getUsuarios(pageable, filtro);
    }

    @Transactional
    @Override
    public void guardar(Usuario usuario) {
        usuarioDao.save(usuario);
    }

    @Transactional
    @Override
    public void eliminar(Usuario usuario) {
        usuarioDao.delete(usuario);
    }

    @Transactional(readOnly = true)
    @Override
    public Usuario encontrar(Usuario usuario) {
        return usuarioDao.encontrar(usuario.getCodigoUsuario());
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCodigoUsuario() {
        Integer codigoUsuario = 0;
        if (usuarioDao.getCodigoUsuario() != null) {
            codigoUsuario = usuarioDao.getCodigoUsuario();
        }
        return codigoUsuario;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Usuario> Buscar(String filtro) {
        return usuarioDao.buscarUsuario(filtro);
    }

}
