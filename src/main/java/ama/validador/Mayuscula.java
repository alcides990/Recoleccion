 
package ama.validador;
 
import java.beans.PropertyEditorSupport;

public class Mayuscula extends PropertyEditorSupport{

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(text.trim().toUpperCase()); 
    }
    
    
}
