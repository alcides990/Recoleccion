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
import ama.servicio.AuditoriaEntidadService;
import ama.utilerias.DataTableResponse;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
    @Autowired
    private AuditoriaEntidadService auditoriaEntidad;

    @GetMapping("/listar")
    public String listaCtegoria(Model modelo) {
        modelo.addAttribute("titulo", "Categoria");
        Sucursal sucursal = getUserSession().getSucursal();
        modelo.addAttribute("sucursal", sucursal);
        return "categoria/categoria";
    }

    @PostMapping("/tabla")
    @ResponseBody
    public DataTableResponseV2<Map<String, Object>> tabla(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "1") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "asc") String direccion) {
        int limite = Math.min(Math.max(length, 1), 100);
        Sucursal sucursal = getUserSession().getSucursal();
        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(start, 0) / limite, limite,
                Sort.by(sentido, ordenCategoria(columna)));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Page<Categoria> pagina = filtro.isBlank()
                ? servicioCategoria.listar(pageable, sucursal)
                : servicioCategoria.buscarPorSucursal(pageable, sucursal.getCodigoSucursal(), filtro);
        List<Map<String, Object>> filas = pagina.getContent().stream().map(categoria -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", categoria.getCodigoCategoria());
            fila.put("nombre", categoria.getNombreCategoria());
            fila.put("tarifa", categoria.getTarifa());
            fila.put("sucursal", categoria.getSucursal().getNombreSucursal());
            fila.put("ciudad", categoria.getSucursal().getCiudad().getNombreCiudad());
            return fila;
        }).toList();
        return new DataTableResponseV2<>(draw,
                servicioCategoria.contarPorSucursal(sucursal.getCodigoSucursal()),
                pagina.getTotalElements(), filas);
    }

    private String ordenCategoria(int columna) {
        return switch (columna) {
            case 0 -> "codigoCategoria";
            case 2 -> "tarifa";
            case 3 -> "sucursal.nombreSucursal";
            default -> "nombreCategoria";
        };
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
    @Transactional
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
        boolean alta = categoria.getCodigoCategoria() == null;
        Categoria anterior = alta ? null : servicioCategoria.encontrar(categoria);
        Map<String, Object> datosAntes = auditoriaEntidad.categoria(anterior);
        if (alta) {
            Integer codigoCategoria = servicioCategoria.getCodigoCategoria() + 1;
            categoria.setCodigoCategoria(codigoCategoria);
        }
        categoria.setSucursal(getUserSession().getSucursal());
        try {
            Categoria newCategoria = new Categoria();
            servicioCategoria.guardar(categoria);
            auditoriaEntidad.registrar("CATEGORIA", alta ? "ALTA" : "MODIFICACION",
                    String.valueOf(categoria.getCodigoCategoria()), datosAntes,
                    auditoriaEntidad.categoria(categoria), null);
            return ResponseEntity.ok("Registro guardado correctamente ");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al guardar registro" + e.getMessage());
        }
    }

    @PostMapping("/editar")
    @ResponseBody
    public ResponseEntity<?> editar(@RequestBody Categoria categoria) {
        var categoriaEncontrada = servicioCategoria.encontrar(categoria);

        if (categoriaEncontrada == null || !categoriaEncontrada.getSucursal().getCodigoSucursal()
                .equals(getUserSession().getSucursal().getCodigoSucursal())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Registro no encontrado");
        }
        return ResponseEntity.ok(categoriaEncontrada);
    }

    @PostMapping("/eliminar/{codigoCategoria}")
    @Transactional
    public ResponseEntity<?> eliminar(Categoria categoria) {
        try {
            Categoria categoriaEncontrada = servicioCategoria.encontrar(categoria);
            if (categoriaEncontrada == null || !categoriaEncontrada.getSucursal().getCodigoSucursal()
                    .equals(getUserSession().getSucursal().getCodigoSucursal())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("La categoría no pertenece a su sucursal");
            }
            Map<String, Object> datosAntes = auditoriaEntidad.categoria(categoriaEncontrada);
            servicioCategoria.eliminar(categoriaEncontrada);
            auditoriaEntidad.registrar("CATEGORIA", "ELIMINACION",
                    String.valueOf(categoriaEncontrada.getCodigoCategoria()),
                    datosAntes, null, null);
            return ResponseEntity.ok().body("Categoria Eliminda Correctamente ");
        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al Eliminar Categoria "+ e.getMessage());
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
