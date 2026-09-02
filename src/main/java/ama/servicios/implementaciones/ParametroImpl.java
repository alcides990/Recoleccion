package ama.servicios.implementaciones;

import ama.dao.ParametroDao;
import ama.dominio.Parametro;
import ama.dominio.Sucursal;
import ama.servicio.ParametroService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParametroImpl implements ParametroService {

    @Autowired
    ParametroDao parametroDao;

    @Override
    public List<Parametro> listar() {
        return (List<Parametro>) parametroDao.findAll();
    }

    @Override
    public Parametro guardar(Parametro parametro) {
        return parametroDao.save(parametro);
    }

    @Override
    public void eliminar(Parametro parametro) {
        parametroDao.delete(parametro);
    }

    @Override
    public Parametro encontrar(Sucursal sucursal) {
        return parametroDao.getParametro(sucursal).orElseThrow(()-> new NullPointerException("Parametro no encontrado"));
    }

    @Override
    public Optional<Parametro> buscar(Sucursal sucursal) {
        return parametroDao.getParametro(sucursal);
    }

    @Override
    public Integer generarCodigo() {
        Integer codigoParametro = parametroDao.generarCodigo();
        return codigoParametro != null ? codigoParametro+1 : 1;
    }

}
