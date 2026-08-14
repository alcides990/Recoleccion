package ama.modulos.comprobantesv2;

import com.ibm.icu.text.RuleBasedNumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MontoEnLetrasService {

    private static final Locale ESPANOL = new Locale("es", "PY");

    public String convertir(BigDecimal monto) {
        if (monto == null) {
            return "";
        }
        long guaranies = monto.setScale(0, RoundingMode.HALF_UP).longValueExact();
        var formato = new RuleBasedNumberFormat(ESPANOL, RuleBasedNumberFormat.SPELLOUT);
        return formato.format(guaranies).toUpperCase(ESPANOL) + " GUARANÍES";
    }
}
