package ama.controladorMVC;

import ama.DTO.UsuarioDTO;
import ama.dominio.Paginador;
import ama.dominio.Usuario;
import ama.errores.ClaseError;
import ama.utilerias.PageRender;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ama.servicio.CobradorService;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import ama.servicio.EstadoService;
import ama.servicio.TipoDocumentoService;
import ama.servicio.UsuarioService;
import java.util.Arrays;

@Slf4j
@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService servicioUsuario;

    @Autowired
    private SucursalService servicioSucursal;

    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private CobradorService servicioCobrador;
    @Autowired
    private EstadoService servicioEstado;

    @Autowired
    private TipoDocumentoService servicioTipoDocumento;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @GetMapping("/listar")
    public String listaUsuario(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantElemento", defaultValue = "10") int cantElemento,
            @RequestParam(name = "filtro", defaultValue = "") String filtro, Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        Pageable pageable = PageRequest.of(page, cantElemento);
        Page<Usuario> usuarios = servicioUsuario.listar(pageable, filtro);
        PageRender pageRender = new PageRender("/usuario/listar", usuarios);
        modelo.addAttribute("page", pageRender);
        modelo.addAttribute("usuarios", usuarios);

        return "usuario/usuario";
    }

    @PostMapping("/listar/pagina")
    public @ResponseBody
    Page<UsuarioDTO> listarServicios(@RequestBody Paginador paginador) {
        Pageable pageable = PageRequest.of(paginador.getNumeroPagina(), paginador.getCatidadRegistro());
        var usuarios = servicioUsuario.listar(pageable, paginador.getFiltro());
        return usuarios.map(usuario -> modelMapper.map(usuario, UsuarioDTO.class));
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        var usuario = new Usuario();
        modelo.addAttribute("usuario", usuario);

        var tipoDocumento = servicioTipoDocumento.listar();
        modelo.addAttribute("tipoDocumento", tipoDocumento);

        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursales);

        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudad", ciudades);

        var estado = servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO"));
        modelo.addAttribute("estado", estado);

        return "usuario/modificarUsuario";
    }

    @GetMapping(value = "/buscar/{filtro}", produces = {"application/json"})
    public @ResponseBody
    List<UsuarioDTO> buscarusuario(@PathVariable String filtro) {
        var usuarios = servicioUsuario.Buscar(filtro);
        return usuarios.stream()
                .map(usuario -> modelMapper
                .map(usuario, UsuarioDTO.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/guardar")
    public String guardar(Usuario usuario) {
        if (usuario.getCodigoUsuario() == null) {
            Integer codigoUsuario = servicioUsuario.getCodigoUsuario() + 1;
            usuario.setCodigoUsuario(codigoUsuario);
        }
        servicioUsuario.guardar(usuario);
        return "redirect:/usuario/listar";
    }

    @GetMapping("/editar/{codigoUsuario}")
    public String editar(Usuario usuario, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuario = servicioUsuario.encontrar(usuario);
        if(usuario==null){
            throw  new Error("Usuario no encontrado");
        }
        model.addAttribute("usuario", usuario);
        var tipoDocumento = servicioTipoDocumento.listar();
        model.addAttribute("tipoDocumento", tipoDocumento);
        var sucursal = usuario.getSucursal();
        model.addAttribute("sucursal", sucursal);
        var ciudad = usuario.getSucursal().getCiudad();
        model.addAttribute("ciudad", ciudad);

        var estado =servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO"));
        model.addAttribute("estado", estado);

        return "usuario/modificarUsuario";
    }

    @GetMapping("/eliminar/{codigoUsuario}")
    public String eliminar(Usuario usuario, Model model) {
        try {
            servicioUsuario.eliminar(usuario);
        } catch (DataAccessException e) {
            List<String> errores = new ArrayList<>();
            errores.add("Error al eliminar el usuario " + e.getMostSpecificCause().getMessage());
            model.addAttribute("errores", errores);
            return "errores/error";
        }
        return "redirect:/usuario/listar";
    }

    @PostMapping("/eliminar/{codigoUsuario}")
    public ResponseEntity<?> modal(Usuario usuario) {
           Usuario usuarioRecuperado= servicioUsuario.encontrar(usuario);
        try {
           if(usuarioRecuperado!=null){
             servicioUsuario.eliminar(usuario);
            return ResponseEntity.ok("Usuario eliminado correctamente !!");
           }else{
               return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ClaseError.excepcion("Usuario no exixte en la base de datos..! " , null));
           }
        } catch (Exception e) {
 String nombreUsuario=usuarioRecuperado.getNombre()+" "+usuarioRecuperado.getApellido();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ClaseError.excepcion("Error al eliminar usuario " + nombreUsuario, e));
        }

    }
}
