 
package ama.dao;
 
import ama.dominio.Empresa;
import org.springframework.data.repository.CrudRepository;

public interface EmpresaDao extends CrudRepository<Empresa, Integer>{
    
}
