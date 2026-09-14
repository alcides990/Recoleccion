package ama.modulos.egresos;

public class EgresoException extends RuntimeException {
    public enum Motivo { DATOS_INVALIDOS, NO_ENCONTRADO, SIN_DATOS }

    private final Motivo motivo;

    public EgresoException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo getMotivo() {
        return motivo;
    }

    public static EgresoException datosInvalidos(String mensaje) {
        return new EgresoException(Motivo.DATOS_INVALIDOS, mensaje);
    }
}
