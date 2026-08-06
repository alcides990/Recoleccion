UPDATE comprobantes c
JOIN tmp_pagos t ON t.numero_comprobante = c.numero_comprobante
and t.codigo_serie=c.codigo_serie
SET 
    c.pago_hasta = t.pago_hasta,
    c.periodo_pago = t.periodo_pago;
