package ama.dominio;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;

@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Controller
public class EstadoCuenta implements Serializable {

    private static final long serialVersionUID = 1L;
    private double tarifa;
    private int cantidadDeuda;
    private boolean cantidadDeudaDefinida;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate pagoHasta;
    private double recargo;
    private double subTotal;
    private double totalDeuda;
    private double saldoAnterior;
    private Parametro parametro;

    public EstadoCuenta(double tarifa, Parametro parametro, LocalDate pagoHasta) {
        this.tarifa = tarifa;
        this.parametro = parametro;
        this.pagoHasta = pagoHasta;
    }

    public int getCantidadDeuda() {
        if (!cantidadDeudaDefinida && pagoHasta != null) {
            // Se comparan meses completos, sin considerar el día actual. pagoHasta
            // representa el próximo período a pagar: octubre contra agosto = -2.
            this.cantidadDeuda = Math.toIntExact(ChronoUnit.MONTHS.between(
                    YearMonth.from(pagoHasta), YearMonth.now()));
        }

        return cantidadDeuda;
    }

    public void setCantidadDeuda(int cantidadDeuda) {
        this.cantidadDeuda = cantidadDeuda;
        this.cantidadDeudaDefinida = true;
    }

    public double getTarifa() {
        return tarifa;
    }

    public void setTarifa(double tarifa) {
        this.tarifa = tarifa;
    }

    public LocalDate getPagoHasta() {
        return this.pagoHasta;
    }

    public void setPagoHasta(LocalDate pagoHasta) {
        this.pagoHasta = pagoHasta;
    }

    public double getRecargo() {
        if (cantidadDeuda >= 3) {
            recargo = (cantidadDeuda * tarifa) * (parametro.getRecargoMora() / 100);

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
            totalDeuda = getSubTotal() + getRecargo() - saldoAnterior;
        }

        return totalDeuda;
    }

    public void setTotalDeuda(double totalDeuda) {
        this.totalDeuda = totalDeuda;
    }

    public double getSaldoAnterior() {
        return this.saldoAnterior;
    }

    public void setSaldoAnterior(double saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public Parametro getParametro() {
        return this.parametro;
    }

    public void setParametro(Parametro parametro) {
        this.parametro = parametro;
    }

    @Override
    public String toString() {
        return "EstadoCuenta{" + ", tarifa=" + tarifa + ", cantidadDeuda=" + cantidadDeuda + ", pagoHasta=" + pagoHasta + ", recargo=" + recargo + ", subTotal=" + subTotal + ", totalDeuda=" + totalDeuda + ", saldoAnterior=" + saldoAnterior + '}';
    }

}
