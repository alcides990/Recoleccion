package ama.servicio;

import ama.dao.UsuarioSistemaDao;
import ama.dominio.UsuarioSistema;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.SessionAttributes;

@Slf4j
@Service("userDetailsServise")
@SessionAttributes("usuarioSistema")
public class UsuarioSistemaService implements UserDetailsService {

    @Autowired
    private UsuarioSistemaDao UsuarioSistemaDao;

    @Autowired
    private HttpSession httpSession;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsuarioSistema usuarioSistema = UsuarioSistemaDao.findByNombre(username);
        var roles = new ArrayList<GrantedAuthority>();
        if (usuarioSistema != null) {
            httpSession.setAttribute("usuarioSistema", usuarioSistema);
            usuarioSistema.getDetalleUsuarioSistema().forEach(detalleUsuario -> {
                roles.add(new SimpleGrantedAuthority(detalleUsuario.getRol().getNombre()));
            });
            if (usuarioSistema.getDetalleUsuarioSistema().isEmpty()) {
                throw new UsernameNotFoundException("Usuario no tiene roles");
            }
        } else {
            throw new UsernameNotFoundException("Usuario no se encuentra registrado");
        }

        return new User(usuarioSistema.getNombre(), usuarioSistema.getClave(), roles);
    }

    public Page<UsuarioSistema> findAll(Pageable pageable) {
        return UsuarioSistemaDao.findAll(pageable);
    }

    public <S extends UsuarioSistema> S save(S entity) {
        return UsuarioSistemaDao.save(entity);
    }

    public Optional<UsuarioSistema> findById(Integer id) {
        return UsuarioSistemaDao.findById(id);
    }
    @Transactional
    public void deleteById(Integer id) {
        UsuarioSistemaDao.deleteById(id);
    }
    @Transactional
    public void delete(UsuarioSistema entity) {
        UsuarioSistemaDao.delete(entity);
    }

    public Integer getCodigoUsuarioSistema() {
        return UsuarioSistemaDao.getCodigoUsuarioSistema();
    }

}
