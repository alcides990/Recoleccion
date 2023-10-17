package ama.servicios.implementaciones;

import ama.dao.ComprobanteDao;
import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Servicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ama.servicio.ServicioComprobante;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImplComprobante implements ServicioComprobante {

    @Autowired
    ComprobanteDao comprobanteDao;

    @Transactional(readOnly = true)
    @Override
    public Page<Comprobante> listar(Pageable pageable) {
        return (Page<Comprobante>) comprobanteDao.getAllComprobantes(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Comprobante> filtrar(Pageable pageable, String filtro) {
        return (Page<Comprobante>) comprobanteDao.filtrarComprobantes(pageable, filtro);
    }

    @Transactional
    @Override
    public Comprobante guardar(Comprobante comprobante) {
        return comprobanteDao.save(comprobante);
    }

    @Transactional
    @Override
    public void anular(Comprobante comprobante) {
        comprobanteDao.save(comprobante);
    }

    @Transactional(readOnly = true)
    @Override
    public Comprobante getComprobante(ComprobantePK comprobantePK) {
        return comprobanteDao.getComprobante(comprobantePK);
    }

    @Transactional(readOnly = true)
    @Override
    public int getCantidadComprobante(String cuentaCorriente) {
        return comprobanteDao.getCantidadComprobante(cuentaCorriente);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Comprobante> getComprobantesCuenta(Pageable page, Servicio servicio) {
        return comprobanteDao.getComprobantesCuenta(page, servicio);
    }

    @Override
    public Integer getNumeroComprobante(ComprobantePK comprobantePK) {
        Integer numeroComprobante = numeroComprobante = comprobanteDao.getNumeroComprobante(
                comprobantePK.getCodigoSucursal(),
                comprobantePK.getCodigoPuntoExpedicion(),
                comprobantePK.getCodigoTipoFactura(),
                comprobantePK.getCodigoSerie());
        return numeroComprobante == null? 1: numeroComprobante + 1;
    }

}
