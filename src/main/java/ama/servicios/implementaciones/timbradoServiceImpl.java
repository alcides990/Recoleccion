package ama.servicios.implementaciones;

import ama.dao.TimbradoDao;
import ama.dominio.Timbrado;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.TimbradoService;

@Service
public class timbradoServiceImpl implements TimbradoService {

    @Autowired
    TimbradoDao timbradoDao;

    @Transactional(readOnly = true)
    @Override
    public List<Timbrado> listar() {
        return (List<Timbrado>) timbradoDao.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Timbrado> listarActivos() {
        return timbradoDao.listarActivos();
    }

    @Transactional
    @Override
    public Timbrado guardar(Timbrado timbrado) {
        return timbradoDao.save(timbrado);
    }

    @Transactional
    @Override
    public void eliminar(Timbrado timbrado) {
        timbradoDao.delete(timbrado);
    }

    @Transactional(readOnly = true)
    @Override
    public Timbrado encontrar(Timbrado timbrado) {
        return timbradoDao.getTimbrado(timbrado);
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCodigoTimbrado() {
        Integer codigotimbrado = timbradoDao.getCodigoTimbrado();
        return codigotimbrado != null ? codigotimbrado : 0;
    }

}
