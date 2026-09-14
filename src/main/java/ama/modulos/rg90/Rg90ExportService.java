package ama.modulos.rg90;

import ama.dominio.Sucursal;
import ama.dominio.Comprobante;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Rg90ExportService {

    private static final int MAXIMO_FILAS = 5000;
    private static final DateTimeFormatter FECHA_RG90 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Rg90ComprobanteRepository comprobantes;

    public Rg90ExportService(Rg90ComprobanteRepository comprobantes) {
        this.comprobantes = comprobantes;
    }

    @Transactional(readOnly = true)
    public Rg90Archivo exportarVentas(Sucursal sucursal, YearMonth periodo, String identificador,
            Integer codigoTipoComprobante) {
        if (sucursal == null || sucursal.getEmpresa() == null || sucursal.getEmpresa().getRuc() == null) {
            throw new IllegalArgumentException("La sucursal no tiene empresa/RUC configurado.");
        }
        String ruc = rucSinDv(sucursal.getEmpresa().getRuc());
        if (ruc.isBlank()) {
            throw new IllegalArgumentException("El RUC de la empresa no es válido.");
        }
        String lote = normalizarIdentificador(identificador);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
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

        String base = ruc + "_REG_" + String.format("%02d%04d", periodo.getMonthValue(), periodo.getYear())
                + "_" + lote;
        String contenido = construirCsv(filas);
        return new Rg90Archivo(base + ".zip", zip(base + ".csv", contenido));
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

        return new Rg90VentaFila(
                codigoIdentificacion(tipoDocumento, numeroDocumento),
                identificacionSinDv(numeroDocumento),
                limpiarTexto(razonSocial, 250),
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

    private byte[] zip(String nombreCsv, String contenido) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                zip.setLevel(Deflater.BEST_COMPRESSION);
                zip.putNextEntry(new ZipEntry(nombreCsv));
                zip.write(contenido.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el archivo RG90.", ex);
        }
    }

    private String normalizarIdentificador(String identificador) {
        String limpio = soloAlfanumerico(identificador).toUpperCase(Locale.ROOT);
        return limpio.isBlank() ? "V0001" : limpiarTexto(limpio, 5);
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
