package ama.modulos.rg90;

import ama.dominio.Sucursal;
import ama.dominio.Comprobante;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Rg90ExportService {

    private static final int MAXIMO_FILAS = 5000;
    private static final DateTimeFormatter FECHA_RG90 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Rg90ComprobanteRepository comprobantes;
    private final Rg90SecuenciaRepository secuencias;

    public Rg90ExportService(Rg90ComprobanteRepository comprobantes, Rg90SecuenciaRepository secuencias) {
        this.comprobantes = comprobantes;
        this.secuencias = secuencias;
    }

    @Transactional
    public Rg90Archivo exportarVentas(Sucursal sucursal, LocalDate desde, LocalDate hasta,
            Integer codigoTipoComprobante) {
        if (sucursal == null || sucursal.getEmpresa() == null || sucursal.getEmpresa().getRuc() == null) {
            throw new IllegalArgumentException("La sucursal no tiene empresa/RUC configurado.");
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Seleccione un rango de fechas válido.");
        }
        String ruc = rucSinDv(sucursal.getEmpresa().getRuc());
        if (ruc.isBlank()) {
            throw new IllegalArgumentException("El RUC de la empresa no es válido.");
        }
        Integer tipoComprobante = codigoTipoComprobante == null ? 0 : codigoTipoComprobante;
        List<Rg90VentaFila> filas = comprobantes.ventasRg90(sucursal.getCodigoSucursal(), desde, hasta, tipoComprobante)
                .stream()
                .map(this::filaVenta)
                .toList();
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("No existen comprobantes activos para el período seleccionado.");
        }
        if (filas.size() > MAXIMO_FILAS) {
            throw new IllegalArgumentException("El período tiene " + filas.size()
                    + " comprobantes. RG90 permite máximo " + MAXIMO_FILAS + " filas por archivo.");
        }

        String periodoArchivo = String.format("%02d%04d", desde.getMonthValue(), desde.getYear());
        String lote = siguienteLote(ruc, periodoArchivo, tipoComprobante);
        String base = ruc + "_REG_" + periodoArchivo
                + "_" + lote;
        String contenido = construirCsv(filas);
        return new Rg90Archivo(base + ".csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private Rg90VentaFila filaVenta(Comprobante comprobante) {
        String tipoDocumento = comprobante.getUsuario() == null || comprobante.getUsuario().getTipoDocumento() == null
                ? "" : comprobante.getUsuario().getTipoDocumento().getTipoDocumento();
        String numeroDocumento = comprobante.getUsuario() == null ? "" : comprobante.getUsuario().getNumeroDocumento();
        String razonSocial = comprobante.getRazonSocial();
        if (razonSocial == null || razonSocial.isBlank()) {
            razonSocial = comprobante.getUsuario() == null ? "" : (valor(comprobante.getUsuario().getNombre())
                    + " " + valor(comprobante.getUsuario().getApellido())).trim();
        }
        String tipoComprobante = comprobante.getTipoComprobante() == null
                ? "" : comprobante.getTipoComprobante().getNombreTipoComprobante();
        String timbrado = comprobante.getNumeroTimbradoFiscal();
        if ((timbrado == null || timbrado.isBlank()) && comprobante.getTimbrado() != null) {
            timbrado = String.valueOf(comprobante.getTimbrado().getNumeroTimbrado());
        }
        String condicion = comprobante.getCondicionVenta() == null
                ? "" : comprobante.getCondicionVenta().getCondicionVenta();
        boolean sinDocumento = sinDocumentoValido(tipoDocumento, numeroDocumento);

        return new Rg90VentaFila(
                sinDocumento ? "15" : codigoIdentificacion(tipoDocumento, numeroDocumento),
                sinDocumento ? "X" : identificacionSinDv(numeroDocumento),
                sinDocumento ? "SIN NOMBRE" : limpiarTexto(razonSocial, 250),
                codigoComprobante(tipoComprobante),
                comprobante.getFechaPago(),
                soloDigitos(timbrado),
                numeroComprobante(comprobante),
                Math.round(comprobante.getTotalImporte()),
                codigoCondicion(condicion));
    }

    private String construirCsv(List<Rg90VentaFila> filas) {
        StringBuilder salida = new StringBuilder();
        for (Rg90VentaFila fila : filas) {
            long total = Math.max(0L, fila.total() == null ? 0L : fila.total());
            salida.append(String.join(",",
                    csv("1"),
                    csv(fila.tipoIdentificacion()),
                    csv(fila.numeroIdentificacion()),
                    csv(fila.razonSocial()),
                    csv(fila.tipoComprobante()),
                    csv(fila.fechaEmision().format(FECHA_RG90)),
                    csv(fila.timbrado()),
                    csv(fila.numeroComprobante()),
                    csv(String.valueOf(total)),
                    csv("0"),
                    csv("0"),
                    csv(String.valueOf(total)),
                    csv(fila.condicionVenta()),
                    csv("N"),
                    csv("S"),
                    csv("N"),
                    csv(""),
                    csv("")));
            salida.append("\r\n");
        }
        return salida.toString();
    }

    private String numeroComprobante(Comprobante comprobante) {
        String establecimiento = comprobante.getEstablecimientoFiscal();
        if ((establecimiento == null || establecimiento.isBlank())
                && comprobante.getComprobantePK() != null
                && comprobante.getComprobantePK().getPuntoExpedicionPK() != null) {
            establecimiento = String.valueOf(comprobante.getComprobantePK().getPuntoExpedicionPK().getCodigoSucursal());
        }
        String punto = comprobante.getPuntoExpedicionFiscal();
        if ((punto == null || punto.isBlank())
                && comprobante.getComprobantePK() != null
                && comprobante.getComprobantePK().getPuntoExpedicionPK() != null) {
            punto = String.valueOf(comprobante.getComprobantePK().getPuntoExpedicionPK().getCodigoPuntoExpedicion());
        }
        Integer numero = comprobante.getComprobantePK() == null ? 0
                : comprobante.getComprobantePK().getNumeroComprobante();
        return String.join("-",
                completarTres(establecimiento),
                completarTres(punto),
                String.format("%07d", numero == null ? 0 : numero));
    }

    private String csv(String valor) {
        String limpio = valor == null ? "" : valor;
        if (limpio.contains(",") || limpio.contains("\"") || limpio.contains("\r") || limpio.contains("\n")) {
            return "\"" + limpio.replace("\"", "\"\"") + "\"";
        }
        return limpio;
    }

    private String valor(String valor) {
        return valor == null ? "" : valor;
    }

    private String completarTres(String valor) {
        String limpio = soloDigitos(valor);
        if (limpio.isBlank()) {
            limpio = "0";
        }
        return String.format("%03d", Integer.parseInt(limpio));
    }

    private String siguienteLote(String ruc, String periodoArchivo, Integer tipoComprobante) {
        String clave = ruc + "|" + periodoArchivo + "|" + (tipoComprobante == null ? 0 : tipoComprobante);
        Rg90Secuencia secuencia = secuencias.findByClave(clave)
                .orElseGet(() -> new Rg90Secuencia(clave, 1));
        int lote = secuencia.getSiguienteLote() == null || secuencia.getSiguienteLote() < 1
                ? 1 : secuencia.getSiguienteLote();
        secuencia.setSiguienteLote(lote + 1);
        secuencias.save(secuencia);
        return "V" + lote;
    }

    private String rucSinDv(String ruc) {
        String limpio = soloDigitos(ruc);
        if (limpio.length() > 1 && ruc.contains("-")) {
            return limpio.substring(0, limpio.length() - 1);
        }
        return limpio;
    }

    private String identificacionSinDv(String documento) {
        String limpio = soloAlfanumerico(documento);
        if (documento != null && documento.contains("-") && limpio.length() > 1) {
            return limpio.substring(0, limpio.length() - 1);
        }
        return limpiarTexto(limpio, 20);
    }

    private String codigoIdentificacion(String tipoDocumento, String numeroDocumento) {
        String tipo = normalizar(tipoDocumento);
        if (tipo.contains("RUC")) {
            return "11";
        }
        if (tipo.contains("PASAPORTE")) {
            return "13";
        }
        if (tipo.contains("EXTRANJ")) {
            return "14";
        }
        if (tipo.contains("DIPLO")) {
            return "16";
        }
        if (tipo.contains("TRIBUT")) {
            return "17";
        }
        String documento = soloAlfanumerico(numeroDocumento);
        return documento.isBlank() ? "15" : "12";
    }

    private boolean sinDocumentoValido(String tipoDocumento, String numeroDocumento) {
        String documento = soloAlfanumerico(numeroDocumento);
        if (documento.isBlank() || documento.equalsIgnoreCase("X") || documento.equalsIgnoreCase("SN")) {
            return true;
        }
        String tipo = normalizar(tipoDocumento);
        if (tipo.contains("RUC") || tipo.contains("CED") || tipo.contains("C.I") || tipo.contains("CI")) {
            return soloDigitos(numeroDocumento).isBlank();
        }
        return false;
    }

    private String codigoComprobante(String tipoComprobante) {
        String tipo = normalizar(tipoComprobante);
        if (tipo.contains("NOTA") && tipo.contains("CRED")) {
            return "110";
        }
        if (tipo.contains("NOTA") && tipo.contains("DEB")) {
            return "111";
        }
        if (tipo.contains("TICKET")) {
            return "112";
        }
        if (tipo.contains("BOLETA")) {
            return "103";
        }
        return "109";
    }

    private String codigoCondicion(String condicionVenta) {
        String condicion = normalizar(condicionVenta);
        return condicion.contains("CRED") ? "2" : "1";
    }

    private String limpiarTexto(String valor, int maximo) {
        String limpio = valor == null ? "" : valor.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    private String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D+", "");
    }

    private String soloAlfanumerico(String valor) {
        return valor == null ? "" : valor.replaceAll("[^A-Za-z0-9]+", "");
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }
}
