CREATE TEMPORARY TABLE tmp_pagos AS
SELECT 
  
    c.codigo_serie,
    c.numero_comprobante, 
    c.fecha_pago, 
    c.cantidad_pago,

    DATE_ADD(
        DATE_ADD(
            s.fecha_inicio,
            INTERVAL IFNULL(
                SUM(
                    CASE 
                        WHEN c.codigo_estado = 1 
                        THEN c.cantidad_pago 
                    END
                ) OVER (
                    PARTITION BY c.cuenta_corriente
                    ORDER BY c.fecha_emision
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                ), 0
            ) MONTH
        ),
        INTERVAL c.cantidad_pago MONTH
    ) AS pago_hasta,

    CONCAT(
        DATE_ADD(
            s.fecha_inicio,
            INTERVAL IFNULL(
                SUM(
                    CASE 
                        WHEN c.codigo_estado = 1 
                        THEN c.cantidad_pago 
                    END
                ) OVER (
                    PARTITION BY c.cuenta_corriente
                    ORDER BY c.fecha_emision
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                ), 0
            ) MONTH
        ),
        '/',
        DATE_ADD(
            DATE_ADD(
                s.fecha_inicio,
                INTERVAL IFNULL(
                    SUM(
                        CASE 
                            WHEN c.codigo_estado = 1 
                            THEN c.cantidad_pago 
                        END
                    ) OVER (
                        PARTITION BY c.cuenta_corriente
                        ORDER BY c.fecha_emision
                        ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                    ), 0
                ) MONTH
            ),
            INTERVAL c.cantidad_pago MONTH
        )
    ) AS periodo_pago

FROM comprobantes c
JOIN servicios s
    ON s.cuenta_corriente = c.cuenta_corriente;
