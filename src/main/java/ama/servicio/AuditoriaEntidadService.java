package ama.servicio;

import ama.dominio.Categoria;
import ama.dominio.Servicio;
import ama.dominio.Usuario;
import ama.dominio.UsuarioSistema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaEntidadService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final HttpSession session;

    public AuditoriaEntidadService(JdbcTemplate jdbc, ObjectMapper json, HttpSession session) {
        this.jdbc = jdbc;
        this.json = json;
        this.session = session;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String entidad, String accion, String identificador,
            Map<String, Object> antes, Map<String, Object> despues, String motivo) {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) {
            throw new IllegalStateException("No existe una sesión válida para registrar la auditoría.");
        }
        jdbc.update("""
            INSERT INTO auditoria_entidades
                (entidad, accion, identificador, codigo_sucursal,
                 codigo_usuario_sistema, datos_antes, datos_despues, motivo)
            VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?)
            """, entidad, accion, identificador,
                usuario.getSucursal().getCodigoSucursal(),
                usuario.getCodigoUsuarioSistema(), serializar(antes),
                serializar(despues), vacioANulo(motivo));
    }

    public Map<String, Object> usuario(Usuario valor) {
        if (valor == null) return null;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", valor.getCodigoUsuario());
        datos.put("documento", valor.getNumeroDocumento());
        datos.put("nombre", valor.getNombre());
        datos.put("apellido", valor.getApellido());
        datos.put("celular", valor.getCelular());
        datos.put("correo", valor.getCorreo());
        datos.put("barrio", valor.getBarrio());
        datos.put("direccion", valor.getDireccion());
        datos.put("observacion", valor.getObservacion());
        datos.put("tipoDocumento", valor.getTipoDocumento() == null ? null
                : valor.getTipoDocumento().getCodigoTipoDocumento());
        datos.put("estado", valor.getEstado() == null ? null
                : valor.getEstado().getCodigoEstado());
        datos.put("sucursal", valor.getSucursal() == null ? null
                : valor.getSucursal().getCodigoSucursal());
        return datos;
    }

    public Map<String, Object> cuenta(Servicio valor) {
        if (valor == null) return null;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("cuentaCorriente", valor.getCuentaCorriente());
        datos.put("direccion", valor.getDireccion());
        datos.put("fechaInicio", valor.getFechaInicio());
        datos.put("ocupado", valor.getOcupado());
        datos.put("observacion", valor.getObservacion());
        datos.put("categoria", valor.getCategoria() == null ? null
                : valor.getCategoria().getCodigoCategoria());
        datos.put("estado", valor.getEstado() == null ? null
                : valor.getEstado().getCodigoEstado());
        datos.put("usuario", valor.getUsuario() == null ? null
                : valor.getUsuario().getCodigoUsuario());
        datos.put("sucursal", valor.getSucursal() == null ? null
                : valor.getSucursal().getCodigoSucursal());
        datos.put("manzana", valor.getManzana() == null
                || valor.getManzana().getManzanaPK() == null ? null
                : valor.getManzana().getManzanaPK().getNumeroManzana());
        return datos;
    }

    public Map<String, Object> categoria(Categoria valor) {
        if (valor == null) return null;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", valor.getCodigoCategoria());
        datos.put("nombre", valor.getNombreCategoria());
        datos.put("tarifa", valor.getTarifa());
        datos.put("sucursal", valor.getSucursal() == null ? null
                : valor.getSucursal().getCodigoSucursal());
        return datos;
    }

    public Map<String, Object> usuarioSistema(Integer codigoUsuarioSistema) {
        if (codigoUsuarioSistema == null) return null;
        List<Map<String, Object>> usuarios = jdbc.query("""
                SELECT us.codigo_usuario_sistema, us.usuario,
                       e.estado, s.sucursal
                  FROM usuarios_sistema us
                  JOIN estados e ON e.codigo_estado = us.codigo_estado
                  JOIN sucursales s ON s.codigo_sucursal = us.codigo_sucursal
                 WHERE us.codigo_usuario_sistema = ?
                """, (rs, fila) -> {
                    Map<String, Object> datos = new LinkedHashMap<>();
                    datos.put("codigo", rs.getInt("codigo_usuario_sistema"));
                    datos.put("nombre", rs.getString("usuario"));
                    datos.put("estado", rs.getString("estado"));
                    datos.put("sucursal", rs.getString("sucursal"));
                    return datos;
                }, codigoUsuarioSistema);
        if (usuarios.isEmpty()) return null;
        usuarios.get(0).put("roles", jdbc.queryForList("""
                SELECT r.rol
                  FROM detalle_usuario_sistema dus
                  JOIN roles r ON r.codigo_rol = dus.codigo_rol
                 WHERE dus.codigo_usuario_sistema = ?
                 ORDER BY r.rol
                """, String.class, codigoUsuarioSistema));
        return usuarios.get(0);
    }

    private String serializar(Map<String, Object> valor) {
        if (valor == null) return null;
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo generar la foto de auditoría.", ex);
        }
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
