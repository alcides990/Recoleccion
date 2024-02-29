package ama.servicios.implementaciones;

import ama.dao.CategoriaDao;
import ama.dominio.Categoria;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.CategoriaService;

@Service
public class ImplCategoria implements CategoriaService {

    @Autowired
    CategoriaDao categoriaDao;

    @Transactional(readOnly = true)
    @Override
    public List<Categoria> listar() {
        return (List<Categoria>) categoriaDao.listar();
    }

    @Transactional
    @Override
    public Categoria guardar(Categoria categoria) {
     return categoriaDao.save(categoria);
    }

    @Transactional
    @Override
    public void eliminar(Categoria categoria) {
        categoriaDao.delete(categoria);
    }

    @Transactional(readOnly = true)
    @Override
    public Categoria encontrar(Categoria categoria) {
        return categoriaDao.encontrar(categoria);
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCodigoCategoria() {
        Integer codigoCategoria = 0;
        if (categoriaDao.getCodigoCategoria() != null) {
            codigoCategoria = categoriaDao.getCodigoCategoria();
        }
        return codigoCategoria;
    }

}
