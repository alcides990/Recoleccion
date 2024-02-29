package ama.servicios.implementaciones;

import ama.dao.ManzanaDao;
import ama.dominio.Manzana;
import ama.dominio.ManzanaPK;
import ama.dominio.Zona;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.ManzanaService;

@Slf4j
@Service
public class ImplManzana implements ManzanaService {

    @Autowired
    ManzanaDao manzanaDao;

    @Transactional(readOnly = true)
    @Override
    public List<Manzana> listar(Zona zona) {
        return (List<Manzana>) manzanaDao.listar(zona);
    }

    @Transactional
    @Override
    public void guardar(Manzana manzana) {
        manzanaDao.save(manzana);
    }

    @Transactional
    @Modifying
    @Override
    public void eliminar(Manzana manzana) {
        manzanaDao.delete(manzana);
    }
    
    @Transactional(readOnly = true)
    @Override
    public Manzana encontrar(ManzanaPK manzanaPK) {
        return manzanaDao.encontrar(manzanaPK);
    }


    @Transactional
    @Override
    public void modificar(Manzana manzana) {
        manzanaDao.save(manzana);
    }

}
