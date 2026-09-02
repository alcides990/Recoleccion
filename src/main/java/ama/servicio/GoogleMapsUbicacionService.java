package ama.servicio;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapsUbicacionService {

    private static final int LONGITUD_MAXIMA = 2048;
    private static final int REDIRECCIONES_MAXIMAS = 6;
    private static final Pattern ENLACE_ARROBA = Pattern.compile(
            "@(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)(?:,|/|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAMETRO = Pattern.compile(
            "(?:[?&#]|\\b)(?:q|query|ll|center|destination|origin)=(-?\\d{1,2}(?:\\.\\d+)?)[,\\s]+(-?\\d{1,3}(?:\\.\\d+)?)(?:[&#\\s]|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATOS_MAPA = Pattern.compile(
            "!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COORDENADAS = Pattern.compile(
            "^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*[,;]\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$");
    private static final Pattern COMPONENTE_GRADOS_MINUTOS_SEGUNDOS = Pattern.compile(
            "(\\d{1,3})\\s*[°º]\\s*(\\d{1,2})\\s*['′’]\\s*"
            + "(\\d{1,2}(?:[.,]\\d+)?)\\s*[\\\"″”]?\\s*([NSEWO])",
            Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;

    public GoogleMapsUbicacionService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    GoogleMapsUbicacionService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Coordenadas extraer(String valor) {
        String entrada = normalizarEntrada(valor);
        URI enlace = null;
        if (entrada.regionMatches(true, 0, "http://", 0, 7)
                || entrada.regionMatches(true, 0, "https://", 0, 8)) {
            enlace = crearEnlaceGoogleMaps(entrada);
        }
        Optional<Coordenadas> coordenadas = buscarCoordenadas(entrada);
        if (coordenadas.isPresent()) {
            return coordenadas.get();
        }

        if (enlace == null || !esEnlaceAbreviado(enlace)) {
            throw new IllegalArgumentException("La ubicación no contiene coordenadas válidas");
        }
        return resolverEnlaceAbreviado(enlace);
    }

    private Coordenadas resolverEnlaceAbreviado(URI enlace) {
        URI actual = enlace;
        try {
            for (int i = 0; i < REDIRECCIONES_MAXIMAS; i++) {
                validarEnlaceGoogle(actual);
                HttpRequest solicitud = HttpRequest.newBuilder(actual)
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "Mozilla/5.0 recoleccion-ubicacion")
                        .GET()
                        .build();
                HttpResponse<Void> respuesta = httpClient.send(
                        solicitud, HttpResponse.BodyHandlers.discarding());
                int estado = respuesta.statusCode();
                if (!esRedireccion(estado)) {
                    break;
                }
                String ubicacion = respuesta.headers().firstValue("location")
                        .orElseThrow(() -> new IllegalArgumentException(
                        "Google Maps no devolvió la ubicación del enlace"));
                actual = actual.resolve(ubicacion);
                validarEnlaceGoogle(actual);
                Optional<Coordenadas> coordenadas = buscarCoordenadas(actual.toString());
                if (coordenadas.isPresent()) {
                    return coordenadas.get();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Se interrumpió la consulta a Google Maps", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("No fue posible consultar el enlace de Google Maps", e);
        }
        throw new IllegalArgumentException(
                "No fue posible recuperar las coordenadas del enlace compartido");
    }

    private String normalizarEntrada(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Pegue una ubicación de Google Maps");
        }
        String entrada = valor.trim().replace("&amp;", "&");
        if (entrada.length() > LONGITUD_MAXIMA) {
            throw new IllegalArgumentException("La ubicación pegada es demasiado larga");
        }
        return entrada;
    }

    private Optional<Coordenadas> buscarCoordenadas(String valor) {
        for (String texto : List.of(valor, decodificar(valor))) {
            Optional<Coordenadas> coordenadasDms = buscarGradosMinutosSegundos(texto);
            if (coordenadasDms.isPresent()) {
                return coordenadasDms;
            }
            for (Pattern patron : List.of(ENLACE_ARROBA, PARAMETRO, DATOS_MAPA, COORDENADAS)) {
                Matcher coincidencia = patron.matcher(texto);
                if (coincidencia.find()) {
                    BigDecimal latitud = new BigDecimal(coincidencia.group(1));
                    BigDecimal longitud = new BigDecimal(coincidencia.group(2));
                    if (sonValidas(latitud, longitud)) {
                        return Optional.of(new Coordenadas(latitud, longitud));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Coordenadas> buscarGradosMinutosSegundos(String valor) {
        Matcher coincidencia = COMPONENTE_GRADOS_MINUTOS_SEGUNDOS.matcher(valor);
        BigDecimal latitud = null;
        BigDecimal longitud = null;
        while (coincidencia.find()) {
            int grados = Integer.parseInt(coincidencia.group(1));
            int minutos = Integer.parseInt(coincidencia.group(2));
            BigDecimal segundos = new BigDecimal(coincidencia.group(3).replace(',', '.'));
            String hemisferio = coincidencia.group(4).toUpperCase();
            if (minutos >= 60 || segundos.compareTo(BigDecimal.valueOf(60)) >= 0) {
                return Optional.empty();
            }
            BigDecimal decimal = BigDecimal.valueOf(grados)
                    .add(BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 12, RoundingMode.HALF_UP))
                    .add(segundos.divide(BigDecimal.valueOf(3600), 12, RoundingMode.HALF_UP));
            if ("S".equals(hemisferio) || "W".equals(hemisferio) || "O".equals(hemisferio)) {
                decimal = decimal.negate();
            }
            decimal = decimal.setScale(10, RoundingMode.HALF_UP);
            if ("N".equals(hemisferio) || "S".equals(hemisferio)) {
                latitud = decimal;
            } else {
                longitud = decimal;
            }
        }
        return latitud != null && longitud != null && sonValidas(latitud, longitud)
                ? Optional.of(new Coordenadas(latitud, longitud)) : Optional.empty();
    }

    private String decodificar(String valor) {
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return valor;
        }
    }

    private URI crearEnlaceGoogleMaps(String valor) {
        try {
            URI enlace = URI.create(valor);
            validarEnlaceGoogle(enlace);
            return enlace;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Pegue coordenadas válidas o un enlace de Google Maps", e);
        }
    }

    private void validarEnlaceGoogle(URI enlace) {
        String host = enlace.getHost();
        if (!"https".equalsIgnoreCase(enlace.getScheme()) || host == null
                || !esDominioGooglePermitido(host.toLowerCase())) {
            throw new IllegalArgumentException("El enlace debe pertenecer a Google Maps");
        }
    }

    private boolean esDominioGooglePermitido(String host) {
        return "maps.app.goo.gl".equals(host)
                || "goo.gl".equals(host)
                || "google.com".equals(host)
                || host.endsWith(".google.com");
    }

    private boolean esEnlaceAbreviado(URI enlace) {
        String host = enlace.getHost().toLowerCase();
        return "maps.app.goo.gl".equals(host) || "goo.gl".equals(host);
    }

    private boolean esRedireccion(int estado) {
        return estado == HttpURLConnection.HTTP_MOVED_PERM
                || estado == HttpURLConnection.HTTP_MOVED_TEMP
                || estado == HttpURLConnection.HTTP_SEE_OTHER
                || estado == 307 || estado == 308;
    }

    private boolean sonValidas(BigDecimal latitud, BigDecimal longitud) {
        return latitud.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitud.compareTo(BigDecimal.valueOf(90)) <= 0
                && longitud.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitud.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    public record Coordenadas(BigDecimal latitud, BigDecimal longitud) {}
}
