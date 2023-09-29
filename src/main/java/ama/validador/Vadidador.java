 
package ama.validador;
 
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class Vadidador implements Validator{

    @Override
    public boolean supports(Class<?> clazz) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void validate(Object target, Errors errors) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

//         @Override
//    public boolean supports(Class<?> clazz) {
//        return Categoria.class.isAssignableFrom(clazz);
//    }
//
//    @Override
//    public void validate(Object target, Errors errors) {
//          Categoria categoria=(Categoria) target;
//        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "nombreCategoria", "NotEmpty.categoria.nombreCategoria");
//    }
    
}
