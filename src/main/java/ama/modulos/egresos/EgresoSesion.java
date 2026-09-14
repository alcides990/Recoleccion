package ama.modulos.egresos;

import ama.dominio.UsuarioSistema;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EgresoSesion {
    public record Contexto(int sucursal, int usuario, String nombreSucursal) {}

    private final HttpSession session;

    public EgresoSesion(HttpSession session) {
        this.session = session;
    }

    public Contexto actual() {
        Object atributo = session.getAttribute("usuarioSistema");
        if (!(atributo instanceof UsuarioSistema usuario) || usuario.getSucursal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión no está disponible.");
        }
        return new Contexto(usuario.getSucursal().getCodigoSucursal(), usuario.getCodigoUsuarioSistema(),
                usuario.getSucursal().getNombreSucursal());
    }
}
