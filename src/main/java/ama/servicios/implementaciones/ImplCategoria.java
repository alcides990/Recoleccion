package ama.servicios.implementaciones;

import ama.dao.CategoriaDao;
import ama.dominio.Categoria;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.CategoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class ImplCategoria implements CategoriaService {

    @Autowired
    CategoriaDao categoriaDao;

    @Transactional(readOnly = true)
    @Override
    public List<Categoria> listar() {
        return (List<Categoria>) categoriaDao.listar();
    }
    @Transactional(readOnly = true)
    @Override
    public List<Categoria> listar(Sucursal sucursal) {
        return (List<Categoria>) categoriaDao.listar(sucursal);
    }
    @Transactional(readOnly = true)
    @Override
    public Page<Categoria> listar(Pageable pageable,Sucursal sucursal) {
        return  categoriaDao.filtrar(pageable, sucursal);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Categoria> buscarPorSucursal(Pageable pageable, Integer codigoSucursal, String filtro) {
        return categoriaDao.buscarPorSucursal(pageable, codigoSucursal, filtro);
    }

    @Transactional(readOnly = true)
    @Override
    public long contarPorSucursal(Integer codigoSucursal) {
        return categoriaDao.contarPorSucursal(codigoSucursal);
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
