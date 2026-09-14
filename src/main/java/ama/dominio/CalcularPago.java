package ama.dominio;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CalcularPago extends EstadoCuenta implements Serializable {

    private static final long serialVersionUID = 1L;

    private String periodoPago;
    private Integer cantidadPago;
    private double subTotal;
    private double recargoPago;
    private double totalPagar;
    private double totalImporte;
    private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("MM-yyyy");

    public CalcularPago(ComprobanteGuardar comprobante) {
        super.setParametro(comprobante.getParametro());
        this.setTarifa(comprobante.getTarifa());
        this.setPagoHasta(comprobante.getPagoHasta());
        this.recargoPago = comprobante.getRecargoPago();
        this.cantidadPago = comprobante.getCantidadPago();
        this.setSaldoAnterior(comprobante.getSaldoAnterior());
        this.setTotalImporte(comprobante.getTotalImporte());

    }

    public Integer getCantidadPago() {
        return this.cantidadPago;
    }

    public void setCantidadPago(int cantidadPago) {
        this.cantidadPago = cantidadPago;
    }

    @Override
    public double getSubTotal() {
        return this.cantidadPago * this.getTarifa();
    }

    @Override
    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getTotalPagar() {
        return getSubTotal() + recargoPago- this.getSaldoAnterior();
    }

    public void setTotalPagar(double totalPagar) {
        this.totalPagar = totalPagar;
    }

    public double getTotalImporte() {
        return this.totalImporte;
    }

    public void setTotalImporte(double totalImporte) {
        this.totalImporte = totalImporte;
    }

   public double getSaldo() {
    double saldo = getTotalImporte() - getTotalPagar();
    return saldo < 0 ? 0 : saldo;
}

    public String getPeriodoPago() {
        if (super.getPagoHasta() == null || cantidadPago == null || cantidadPago <= 0) {
            return "";
        }
        LocalDate periodoDesde = super.getPagoHasta();
        if (cantidadPago == 1) {
            this.periodoPago = periodoDesde.format(FORMATO_PERIODO);
            return this.periodoPago;
        }
        LocalDate periodoHasta = periodoDesde.plusMonths(cantidadPago - 1L);
        this.periodoPago = periodoDesde.format(FORMATO_PERIODO)
                + " / " + periodoHasta.format(FORMATO_PERIODO);
        return this.periodoPago;
    }

    @Override
    public LocalDate getPagoHasta() {
        return super.getPagoHasta().plusMonths(getCantidadPago()); 
    }

     


}
