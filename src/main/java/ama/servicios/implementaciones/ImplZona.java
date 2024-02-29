package ama.servicios.implementaciones;

import ama.dao.ZonaDao;
import ama.dominio.Zona;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.ZonaService;

@Service
public class ImplZona implements ZonaService {

    @Autowired
    ZonaDao zonaDao;

    @Transactional(readOnly = true)
    @Override
    public List<Zona> listar() {
        return (List<Zona>) zonaDao.listar();
    }

    @Transactional
    @Override
    public void guardar(Zona zona) {
        zonaDao.save(zona);
    }

    @Transactional
    @Override
    public void eliminar(Zona zona) {
        zonaDao.delete(zona);
    }

    @Transactional(readOnly = true)
    @Override
    public Zona encontrar(Zona zona) {
        return zonaDao.econtrar(zona);
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCodigoZona() {
        Integer codigoZona = 0;
        if (zonaDao.getCodigoZona() != null) {
            codigoZona = zonaDao.getCodigoZona();
        }
        return codigoZona;
    }

}
