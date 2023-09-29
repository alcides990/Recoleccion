package ama.validador;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpperCase<T> {
 

    public T UpperCase(T objeto) {
        try {
            Class<?> clazz = objeto.getClass();
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    String valor = (String) field.get(objeto);
                    if (valor != null) {
                        field.set(objeto, valor.toUpperCase());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return objeto;
    }

}
