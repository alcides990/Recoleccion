package ama.servicios.implementaciones;

import ama.dao.SucursalDao;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ama.servicio.SucursalService;

@Service
public class ImplSucursal implements SucursalService {

    @Autowired
    SucursalDao sucursalDao;

    @Override
    public List<Sucursal> listar() {
        return sucursalDao.listar();
    }

    @Override
    public Sucursal guardar(Sucursal sucursal) {
        return sucursalDao.save(sucursal);
    }

    @Override
    public void eliminar(Sucursal sucursal) {
        sucursalDao.delete(sucursal);
    }

    @Override
    public Sucursal encontrar(Sucursal sucursal) {
        return sucursalDao.encontrar(sucursal);
    }
    @Override
    public Integer getCodigoSucursal(){
        Integer codigoSucursal=0;
        if(sucursalDao.getCodigoSucursal()!=null){
            codigoSucursal=sucursalDao.getCodigoSucursal();
        }
        return codigoSucursal;
    }
    
     

}
