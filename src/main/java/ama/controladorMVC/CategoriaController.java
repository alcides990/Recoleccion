package ama.controladorMVC;

import ama.dominio.Categoria;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.validador.Vadidador;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ama.servicio.CategoriaService;
import ama.utilerias.DataTableResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/categoria")
public class CategoriaController {

    @Autowired
    private Vadidador validar;
    @Autowired
    private CategoriaService servicioCategoria;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaCtegoria(Model modelo) {
        modelo.addAttribute("titulo", "Categoria");
        Sucursal sucursal = getUserSession().getSucursal();
        modelo.addAttribute("sucursal", sucursal);
        List<Categoria> categorias = servicioCategoria.listar(sucursal);
        modelo.addAttribute("categorias", categorias);
        return "categoria/categoria";
    }

    @GetMapping("/filtrar")
    @ResponseBody
    public ResponseEntity<?> filtrarCtegoria(
            @RequestParam(name = "draw") Integer draw,
            @RequestParam(name = "start") Integer inicio,
            @RequestParam(name = "length") Integer cantidadRegistro,
            @RequestParam(name = "search", required = false)  Integer filtro,
            @RequestParam(name = "page") Integer page,
            @RequestParam(name = "size") Integer size,
            HttpServletRequest request) {
        log.info(request.getQueryString());
        Pageable pageable = PageRequest.of(0, 10);
        Page<Categoria> categorias=servicioCategoria.listar(pageable,getUserSession().getSucursal());
        DataTableResponse<Categoria> categoriaRespoonse=new DataTableResponse<>();
        categoriaRespoonse.setDraw(draw);
        categoriaRespoonse.setRecordsFiltered(categorias.getTotalElements());
        categoriaRespoonse.setRecordsTotal(categorias.getTotalElements());
        categoriaRespoonse.setData(categorias.getContent());
        return ResponseEntity.ok(categoriaRespoonse);
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<String> guardar(
            @RequestBody @Valid Categoria categoria, BindingResult result
    ) {
        StringBuffer errores = new StringBuffer();
        if (result.hasErrors()) {
            result.getAllErrors().forEach(error -> {
                errores.append(error.getDefaultMessage());
            });
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al guardar registro: " + errores);
        }
        if (categoria.getCodigoCategoria() == null) {
            Integer codigoCategoria = servicioCategoria.getCodigoCategoria() + 1;
            categoria.setCodigoCategoria(codigoCategoria);
        }
        try {
            Categoria newCategoria = new Categoria();
            servicioCategoria.guardar(categoria);
            return ResponseEntity.ok("Registro guardado correctamente ");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al guardar registro" + e.getMessage());
        }
    }

    @PostMapping("/editar")
    @ResponseBody
    public ResponseEntity<?> editar(@RequestBody Categoria categoria) {
        var categoriaEncontrada = servicioCategoria.encontrar(categoria);

        if (categoriaEncontrada == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Registro no encontrado");
        }
        return ResponseEntity.ok(categoriaEncontrada);
    }

    @PostMapping("/eliminar/{codigoCategoria}")
    public ResponseEntity<?> eliminar(Categoria categoria) {
        try {
            servicioCategoria.eliminar(categoria);
            return ResponseEntity.ok().body("Categoria Eliminda Correctamente ");
        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al Eliminar Categoria "+ e.getMessage());
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
