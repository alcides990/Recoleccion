package ama.facturaelectronica;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class AlmacenDocumentosSifen {
    private final Path raiz;

    public AlmacenDocumentosSifen(SifenProperties properties) throws IOException {
        raiz = properties.getStorageRoot().toAbsolutePath().normalize();
        Files.createDirectories(raiz);
    }

    public DocumentoGuardado guardar(ClaveComprobanteElectronico clave, String nombre, byte[] contenido)
            throws IOException {
        LocalDate hoy = LocalDate.now();
        Path directorio = raiz.resolve(String.valueOf(hoy.getYear()))
                .resolve("%02d".formatted(hoy.getMonthValue())).resolve(clave.directorio()).normalize();
        validar(directorio);
        Files.createDirectories(directorio);
        Path destino = directorio.resolve(nombre).normalize();
        validar(destino);
        Path temporal = Files.createTempFile(directorio, nombre + ".", ".tmp");
        try {
            Files.write(temporal, contenido);
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
        return new DocumentoGuardado(raiz.relativize(destino).toString().replace('\\', '/'), sha256(contenido));
    }

    public byte[] leer(String rutaRelativa) throws IOException {
        Path archivo = raiz.resolve(rutaRelativa).normalize();
        validar(archivo);
        return Files.readAllBytes(archivo);
    }

    private void validar(Path ruta) {
        if (!ruta.startsWith(raiz)) throw new SecurityException("Ruta de documento fuera del almacén SIFEN");
    }

    private String sha256(byte[] contenido) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido)); }
        catch (Exception ex) { throw new IllegalStateException("No se pudo calcular SHA-256", ex); }
    }

    public record DocumentoGuardado(String ruta, String sha256) {}
}
