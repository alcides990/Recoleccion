package ama.dominio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;

public class EstadoCuenta implements Serializable {

    private static final long serialVersionUID = 1L;
    private LocalDate fechaInicio;
    private double tarifa;
    private int periodoPagado;
    private int cantidadDeuda;
    @NotNull
    @Max(value = 1)
    private int cantidadPago;
    private String periodoPago;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate pagoHasta;
    private double recargo;
    private double subTotal;
    private double totalDeuda;

    private double subTotalImpporte;
    private double totalImporte;

    private double recargoPago;

//    private LocalDate fecham = LocalDate.of(2012, 6, 30);
//   para sumar mes a la fecha
//    LocalDate b = a.plusMonths(2);
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public int getCantidadDeuda() {
        LocalDate hoy = LocalDate.now();
        if (fechaInicio != null) {
            LocalDate fechaInicio = LocalDate.parse(getFechaInicio().toString());
            int cantidadPeriodo = (int) (ChronoUnit.DAYS.between(fechaInicio, hoy)) / 30;
            cantidadDeuda = cantidadPeriodo - getPeriodoPagado();
        }

        return cantidadDeuda;
    }

    public void setCantidadDeuda(int cantidadDeuda) {
        this.cantidadDeuda = cantidadDeuda;
    }

    public double getTarifa() {
        return tarifa;
    }

    public void setTarifa(double tarifa) {
        this.tarifa = tarifa;
    }

    public int getCantidadPago() {
        return cantidadPago;
    }

    public void setCantidadPago(int cantidadPago) {
        this.cantidadPago = cantidadPago;
    }

    public String getPeriodoPago() {
        if (cantidadPago > 0) {
            periodoPago = fechaInicio.plusMonths(this.periodoPagado).toString() + " / " + fechaInicio.plusMonths(this.periodoPagado + cantidadPago).toString();
        }
        return periodoPago;
    }

    public void setPeriodoPago(String periodoPago) {
        this.periodoPago = periodoPago;
    }

    public LocalDate getPagoHasta() {
        if (this.pagoHasta == null) {
            this.pagoHasta = getFechaInicio();
        } else {
            this.pagoHasta = LocalDate.parse(fechaInicio.plusMonths(periodoPagado + cantidadPago).toString(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return this.pagoHasta;
    }

    public void setPagoHasta(LocalDate pagoHasta) {
        this.pagoHasta = pagoHasta;
    }

    public double getRecargo() {
        if (cantidadDeuda >= 3) {
            recargo = (cantidadDeuda * tarifa) * 0.1;

        }
        return recargo;
    }

    public void setRecargo(double recargo) {
        this.recargo = recargo;
    }

    public double getSubTotal() {
        if (cantidadDeuda > 0) {
            subTotal = (getCantidadDeuda() * getTarifa());
        }
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getTotalDeuda() {
        if (cantidadDeuda > 0) {
            recargo = getRecargo();
            totalDeuda = getSubTotal() + getRecargo();
        }

        return totalDeuda;
    }

    public void setTotalDeuda(double totalDeuda) {
        this.totalDeuda = totalDeuda;
    }

    public int getPeriodoPagado() {
        return periodoPagado;
    }

    public void setPeriodoPagado(int periodoPagado) {
        this.periodoPagado = periodoPagado;
    }

    public double getSubTotalImpporte() {
        return tarifa * cantidadPago;
    }

    public void setSubTotalImpporte(double subTotalImpporte) {
        this.subTotalImpporte = subTotalImpporte;
    }

    public double getRecargoPago() {
        return recargoPago;
    }

    public void setRecargoPago(double recargoPago) {
        this.recargoPago = recargoPago;
    }

    public double getTotalImporte() {
        totalImporte = getSubTotalImpporte() + getRecargoPago();
        return totalImporte;
    }

    public void setTotalImporte(double totalImporte) {
        this.totalImporte = totalImporte;
    }

    @Override
    public String toString() {
        return "EstadoCuenta{" + "fechaInicio=" + fechaInicio + ", tarifa=" + tarifa + ", periodoPagado=" + periodoPagado + ", cantidadDeuda=" + cantidadDeuda + ", cantidadPago=" + cantidadPago + ", periodoPago=" + periodoPago + ", pagoHasta=" + pagoHasta + ", recargo=" + recargo + ", subTotal=" + subTotal + ", totalDeuda=" + totalDeuda + ", subTotalImpporte=" + subTotalImpporte + ", totalImporte=" + totalImporte + ", recargoPago=" + recargoPago + '}';
    }

}
