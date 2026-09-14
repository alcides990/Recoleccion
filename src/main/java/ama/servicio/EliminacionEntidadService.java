package ama.servicio;

import ama.dominio.Servicio;
import ama.dominio.Usuario;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminacionEntidadService {

    private final UsuarioService usuarioService;
    private final ServicioService servicioService;
    private final AuditoriaEntidadService auditoriaEntidadService;

    public EliminacionEntidadService(UsuarioService usuarioService,
            ServicioService servicioService,
            AuditoriaEntidadService auditoriaEntidadService) {
        this.usuarioService = usuarioService;
        this.servicioService = servicioService;
        this.auditoriaEntidadService = auditoriaEntidadService;
    }

    @Transactional
    public void eliminarUsuario(Usuario usuario, Map<String, Object> datosAntes) {
        usuarioService.eliminar(usuario);
        auditoriaEntidadService.registrar("USUARIO", "ELIMINACION",
                String.valueOf(usuario.getCodigoUsuario()), datosAntes, null, null);
    }

    @Transactional
    public void eliminarCuenta(Servicio servicio, Map<String, Object> datosAntes) {
        servicioService.eliminar(servicio);
        auditoriaEntidadService.registrar("CUENTA", "ELIMINACION",
                servicio.getCuentaCorriente(), datosAntes, null, null);
    }
}
