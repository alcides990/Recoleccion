 
package ama.servicios.implementaciones;

import ama.dao.TipoDocumentoDao;
import ama.dominio.TipoDocumento;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.TipoDocumentoService;

 @Service
public class ImplTipoDocumento implements TipoDocumentoService {

    @Autowired
    TipoDocumentoDao tipoDocumentoDao;
    @Override
    public List<TipoDocumento> listar() {
       return (List<TipoDocumento>) tipoDocumentoDao.findAll();
    }

    @Override
    public void guardar(TipoDocumento tipoDocumento) {
       tipoDocumentoDao.save(tipoDocumento);
    }

    @Override
    public void eliminar(TipoDocumento tipoDocumento) {
        tipoDocumentoDao.delete(tipoDocumento);
    }
   @Override
        public TipoDocumento encontrar(TipoDocumento tipoDocumento) {
       return tipoDocumentoDao.findById(tipoDocumento.getCodigoTipoDocumento()).orElse(null);
    }
    
}
