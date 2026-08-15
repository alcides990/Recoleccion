package ama.facturaelectronica;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recoleccion.sifen")
public class SifenProperties {
    private boolean enabled;
    private String ambiente = "TEST";
    private Path storageRoot = Path.of("./documentos/factura-electronica");
    private String certificado;
    private String certificadoPassword;
    private String cscId;
    private String csc;

    public boolean configuracionCompleta() {
        return enabled && texto(certificado) && texto(certificadoPassword) && texto(cscId) && texto(csc);
    }

    private boolean texto(String valor) { return valor != null && !valor.isBlank(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public Path getStorageRoot() { return storageRoot; }
    public void setStorageRoot(Path storageRoot) { this.storageRoot = storageRoot; }
    public String getCertificado() { return certificado; }
    public void setCertificado(String certificado) { this.certificado = certificado; }
    public String getCertificadoPassword() { return certificadoPassword; }
    public void setCertificadoPassword(String certificadoPassword) { this.certificadoPassword = certificadoPassword; }
    public String getCscId() { return cscId; }
    public void setCscId(String cscId) { this.cscId = cscId; }
    public String getCsc() { return csc; }
    public void setCsc(String csc) { this.csc = csc; }
}
