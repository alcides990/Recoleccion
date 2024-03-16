package ama.servicios.implementaciones;

import ama.dao.ComprobanteDao;
import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.PuntoExpedicion;
import ama.dominio.Serie;
import ama.dominio.Servicio;
import ama.dominio.Sucursal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import ama.servicio.ComprobanteService;
import java.util.Optional;

@Slf4j
@Service
public class ImplComprobante implements ComprobanteService {

    @Autowired
    ComprobanteDao comprobanteDao;

    @Transactional(readOnly = true)
    @Override
    public Page<Comprobante> listar(Pageable pageable) {
        return (Page<Comprobante>) comprobanteDao.getAllComprobantes(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Comprobante> filtrar(Pageable pageable, ComprobantePK comprobantePK) {
        Page<Comprobante> comprobantes = null;
        if (comprobantePK.getNumeroComprobante() != null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() != 0) {
            comprobantes = comprobanteDao.filterByPuntoExpedicionAndNumeroComprobante(pageable,
                    new PuntoExpedicion(comprobantePK.getPuntoExpedicionPK()),
                    new Serie(comprobantePK.getCodigoSerie()),
                    comprobantePK.getNumeroComprobante());
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() != null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() == 0) {
            comprobantes = comprobanteDao.filterBySucursalAndNumeroComprobante(pageable,
                    new Sucursal(comprobantePK.getPuntoExpedicionPK().getCodigoSucursal()),
                    new Serie(comprobantePK.getCodigoSerie()),
                    comprobantePK.getNumeroComprobante());
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() == null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() != 0) {
            comprobantes = comprobanteDao.findByPuntoExpedicion(pageable,
                    new PuntoExpedicion(comprobantePK.getPuntoExpedicionPK()),
                    new Serie(comprobantePK.getCodigoSerie()));
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() == null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() == 0) {
            comprobantes = comprobanteDao.findBySucursal(pageable,
                    new Sucursal(comprobantePK.getPuntoExpedicionPK().getCodigoSucursal()),
                    new Serie(comprobantePK.getCodigoSerie()));
            return comprobantes;
        }
        return comprobantes;
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
        Page<ComprobantePK> comprobantePKs = comprobanteDao.getComprobantePKs(page, servicio);

        List<Comprobante> comprobantes = comprobanteDao.getComprobantesCuenta(comprobantePKs.getContent());

        return new PageImpl<Comprobante>(comprobantes, page, comprobantePKs.getTotalElements());
    }

    @Override
    public Integer getNumeroComprobante(ComprobantePK comprobantePK) {
        Integer numeroComprobante = numeroComprobante = comprobanteDao.getNumeroComprobante(
                comprobantePK.getPuntoExpedicionPK(),
                comprobantePK.getCodigoTipoFactura(),
                comprobantePK.getCodigoSerie());
        return numeroComprobante == null ? 1 : numeroComprobante + 1;
    }

    @Override
    public int getCantidadPago(String cuentaCorriente) {
        Integer cantidadPago = comprobanteDao.getCantidadPago(cuentaCorriente);
        return cantidadPago != null ? cantidadPago : 0;
    }

    @Override
    public Optional<Comprobante> getUltimoComprobanteCuentaActivo(String cuentaCorriente) {
        return comprobanteDao.getUltimoComprobanteCuentaActivo(cuentaCorriente);
    }

    @Override
    public List<Object[]> getPagoHastaAndSaldo(String cuentaCorriente) {
        return comprobanteDao.getPagoHastaAndSaldo(cuentaCorriente);
    }
}
