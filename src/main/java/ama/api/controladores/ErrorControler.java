package ama.api.controladores;

import java.sql.SQLSyntaxErrorException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ErrorControler {

    
    @ExceptionHandler(BindException.class)
    private String nullPoiterException(BindException e, Model model) {
        model.addAttribute("error", "Error de validacion");
        model.addAttribute("message", e.getMessage());
        model.addAttribute("timestamp", LocalDateTime.now());
        
        return "error/error";
    }
    @ExceptionHandler(SQLSyntaxErrorException.class)
    private String errorSQL(SQLSyntaxErrorException e, Model model) {
        model.addAttribute("error", "Error de sintaxis SQL");
        model.addAttribute("message", e.getMessage());
        model.addAttribute("timestamp", LocalDateTime.now());
        
        return "error/error";
    }
    @ExceptionHandler(JRException.class)
    private String errorSQL(JRException e, Model model) {
        model.addAttribute("error", "Ocurrio error al generar el reporte");
        model.addAttribute("message", e.getMessage());
        model.addAttribute("timestamp", LocalDateTime.now());
        
        return "error/error";
    }
    
  
   

}
