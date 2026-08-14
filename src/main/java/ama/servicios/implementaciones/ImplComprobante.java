package ama.servicios.implementaciones;

import ama.dao.ComprobanteDao;
import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.PuntoExpedicion;
import ama.dominio.Servicio;
import ama.dominio.Sucursal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
        Sucursal sucursal = new Sucursal(comprobantePK.getPuntoExpedicionPK().getCodigoSucursal());
        if (comprobantePK.getNumeroComprobante() == null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() == 0
                && comprobantePK.getCodigoSerie() == 0) {
            comprobantes = comprobanteDao.findBySucursal(pageable, sucursal);
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() != null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() != 0
                && comprobantePK.getCodigoSerie() != 0) {
            comprobantes = comprobanteDao.filterByPuntoExpedicionAndSerieAndNumeroComprobante(pageable,
                    new PuntoExpedicion(comprobantePK.getPuntoExpedicionPK()),
                    comprobantePK.getCodigoSerie(),
                    comprobantePK.getNumeroComprobante());
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() != null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() == 0
                && comprobantePK.getCodigoSerie() == 0) {
            comprobantes = comprobanteDao.filterBySucursalAndNumeroComprobante(pageable,
                    sucursal, comprobantePK.getNumeroComprobante());
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() != null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() != 0
                && comprobantePK.getCodigoSerie() == 0) {
            comprobantes = comprobanteDao.filterByPuntoExpedicionAndNumeroComprobante(pageable,
                    new PuntoExpedicion(comprobantePK.getPuntoExpedicionPK()),
                    comprobantePK.getNumeroComprobante());
            return comprobantes;
        } else if (comprobantePK.getNumeroComprobante() == null
                && comprobantePK.getPuntoExpedicionPK().getCodigoPuntoExpedicion() != 0
                && comprobantePK.getCodigoSerie() == 0) {
            comprobantes = comprobanteDao.findByPuntoExpedicion(pageable,
                    new PuntoExpedicion(comprobantePK.getPuntoExpedicionPK()));
            return comprobantes;
        }
        return null;
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
    public Optional<Comprobante> findById(ComprobantePK comprobantePK) {
        return comprobanteDao.findById(comprobantePK);
    }

    @Transactional(readOnly = true)
    @Override
    public int getCantidadComprobante(String cuentaCorriente) {
        return comprobanteDao.getCantidadComprobante(cuentaCorriente);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Comprobante> getComprobantesCuenta(Pageable page, Servicio servicio) {
        return comprobanteDao.getComprobantesCuenta(page, servicio.getCuentaCorriente());
    }

    @Override
    public Integer getNumeroComprobante(ComprobantePK comprobantePK) {
        Integer numeroComprobante = numeroComprobante = comprobanteDao.getNumeroComprobante(
                comprobantePK.getPuntoExpedicionPK(),
                comprobantePK.getCodigoTipoComprobante(),
                comprobantePK.getCodigoSerie());
        return numeroComprobante == null ? 1 : numeroComprobante + 1;
    }

    @Override
    public int getCantidadPago(String cuentaCorriente) {
        Integer cantidadPago = comprobanteDao.getCantidadPago(cuentaCorriente);
        return cantidadPago != null ? cantidadPago : 0;
    }

    @Override
    public Optional<String> getPagoDesde(String cuentaCorriente) {
        return comprobanteDao.getPagoDesde(cuentaCorriente);
    }

    @Override
    public Optional<Comprobante> getUltimoComprobanteCuentaActivo(String cuentaCorriente) {
        return comprobanteDao.getUltimoComprobanteCuentaActivo(cuentaCorriente);
    }

    @Override
    public List<Object[]> getPagoDesdeAndSaldo(String cuentaCorriente) {
        return comprobanteDao.getPagoDesdeAndSaldo(cuentaCorriente);
    }
}
