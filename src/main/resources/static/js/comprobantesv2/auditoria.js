$(function () {
    const token = $('#token').val();
    const escapar = valor => $('<div>').text(valor ?? '').html();
    const etiquetas = {
        numeroComprobante: 'Número de comprobante',
        razonSocial: 'Razón social', fechaEmision: 'Fecha de emisión', fechaPago: 'Fecha de pago',
        cuentaCorriente: 'Cuenta corriente', usuario: 'Usuario', categoria: 'Categoría',
        condicionVenta: 'Condición de venta', cobrador: 'Cobrador', cantidadDeuda: 'Cantidad de deuda',
        cantidadPago: 'Cantidad pagada', tarifa: 'Tarifa', recargo: 'Recargo', saldo: 'Saldo',
        totalImporte: 'Importe total', periodoPago: 'Período de pago', pagoHasta: 'Pago hasta',
        establecimientoFiscal: 'Establecimiento fiscal', puntoExpedicionFiscal: 'Punto de expedición fiscal',
        numeroTimbradoFiscal: 'Número de timbrado fiscal', inicioVigenciaFiscal: 'Inicio de vigencia fiscal',
        finVigenciaFiscal: 'Fin de vigencia fiscal', serieFiscal: 'Serie fiscal',
        estado: 'Estado', observacion: 'Observación', pagos: 'Medios de pago'
    };
    const analizar = function (valor) {
        if (!valor) return {};
        try { return typeof valor === 'string' ? JSON.parse(valor) : valor; }
        catch (error) { return {}; }
    };
    const normalizar = function (valor) {
        if (Array.isArray(valor)) {
            return valor.map(normalizar).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));
        }
        if (valor && typeof valor === 'object') {
            return Object.keys(valor).sort().reduce((resultado, clave) => {
                resultado[clave] = normalizar(valor[clave]);
                return resultado;
            }, {});
        }
        return valor ?? null;
    };
    const presentar = function (valor, campo) {
        if (valor === null || valor === undefined || valor === '') return '<span class="text-muted">—</span>';
        if (campo === 'pagos' && Array.isArray(valor)) {
            if (!valor.length) return '<span class="text-muted">Sin medios de pago</span>';
            return valor.map(function (pago) {
                const medio = pago.medioPago || ('Código ' + (pago.codigoMetodoPago ?? '—'));
                const importe = Number(pago.importe || 0).toLocaleString('es-PY');
                return '<div><span>' + escapar(medio) + '</span>: <strong>Gs. '
                        + escapar(importe) + '</strong></div>';
            }).join('');
        }
        if (typeof valor === 'object') {
            return '<pre class="small mb-0 text-wrap">' + escapar(JSON.stringify(valor, null, 2)) + '</pre>';
        }
        return escapar(valor);
    };
    const mostrarComparacion = function (detalle) {
        const anterior = analizar(detalle.anterior);
        const nuevo = analizar(detalle.nuevo);
        const campos = Array.from(new Set([...Object.keys(anterior), ...Object.keys(nuevo)]));
        campos.sort((a, b) => (etiquetas[a] || a).localeCompare(etiquetas[b] || b, 'es'));
        const filas = campos.map(function (campo) {
            const cambio = JSON.stringify(normalizar(anterior[campo]))
                    !== JSON.stringify(normalizar(nuevo[campo]));
            return '<tr class="' + (cambio ? 'table-warning' : '') + '"><th>'
                    + escapar(etiquetas[campo] || campo) + '</th><td>' + presentar(anterior[campo], campo)
                    + '</td><td>' + presentar(nuevo[campo], campo) + '</td></tr>';
        }).join('');
        $('#cuerpoComparacionAuditoriaComprobante').html(filas
                || '<tr><td colspan="3" class="text-center text-muted">No hay datos para comparar.</td></tr>');
        window.bootstrap.Modal.getOrCreateInstance(
                document.getElementById('comparacionAuditoriaComprobante')).show();
    };
    const formatearFecha = function (valor) {
        if (!valor) return '';
        const partes = String(valor).replace('T', ' ').split(/[- :]/);
        return partes.length >= 6
                ? partes[2] + '/' + partes[1] + '/' + partes[0] + ' '
                    + partes[3] + ':' + partes[4] + ':' + partes[5].substring(0, 2)
                : valor;
    };

    const tabla = $('#tablaAuditoriaComprobantes').DataTable({
        processing: true,
        serverSide: true,
        searching: true,
        searchDelay: 350,
       
        autoWidth: false,
        pageLength: 25,
        lengthMenu: [[10, 25, 50, 100], [10, 25, 50, 100]],
        pagingType: 'full_numbers',
        order: [[0, 'desc']],
        ajax: {
            url: '/comprobantes-v2/auditoria/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: function (datos) {
                datos._csrf = token;
                datos.numero = $('#fNumeroAuditoria').val();
                datos.accion = $('#fAccionAuditoria').val();
                datos.establecimiento = $('#fEstablecimientoAuditoria').val();
                datos.puntoExpedicion = $('#fPuntoExpedicionAuditoria').val();
            }
        },
        columns: [
            {data: 'fecha', className: 'text-nowrap', render: formatearFecha},
            {data: 'accion', render: valor => '<span class="badge bg-secondary">' + escapar(valor) + '</span>'},
            {data: 'documento', className: 'text-nowrap fw-semibold', render: $.fn.dataTable.render.text()},
            {data: 'timbrado', render: $.fn.dataTable.render.text()},
            {data: 'serie', render: $.fn.dataTable.render.text()},
            {data: 'usuario', render: $.fn.dataTable.render.text()},
            {data: 'motivo', render: $.fn.dataTable.render.text()},
            {data: null, orderable: false, searchable: false, className: 'text-center', render: function (_, __, fila) {
                return fila.accion === 'MODIFICACION'
                        ? '<button type="button" class="btn btn-outline-primary btn-sm ver-comparacion-auditoria" data-codigo="'
                            + fila.codigo + '"><i class="fa-solid fa-code-compare"></i> Comparar</button>'
                        : '<span class="text-muted">—</span>';
            }}
        ],
        language: {url: '/i18n/es-ES.json'}
    });

    $('#filtrarAuditoria').on('click', () => tabla.draw());
    $('#fAccionAuditoria').on('change', () => tabla.draw());
    $('#fNumeroAuditoria, #fEstablecimientoAuditoria, #fPuntoExpedicionAuditoria').on('keydown', function (evento) {
        if (evento.key === 'Enter') {
            evento.preventDefault();
            tabla.draw();
        }
    });
    $('#limpiarAuditoria').on('click', function () {
        $('#fNumeroAuditoria').val('');
        $('#fAccionAuditoria').val('');
        $('#fEstablecimientoAuditoria').val('');
        $('#fPuntoExpedicionAuditoria').val('');
        tabla.search('').draw();
    });
    $('#tablaAuditoriaComprobantes').on('click', '.ver-comparacion-auditoria', function () {
        const boton = $(this).prop('disabled', true);
        $.ajax({
            url: '/comprobantes-v2/auditoria/detalle',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: {codigo: boton.attr('data-codigo'), _csrf: token}
        }).done(mostrarComparacion).fail(function (xhr) {
            const mensaje = xhr.responseJSON?.message || xhr.responseText || 'No fue posible cargar la comparación.';
            window.alert(mensaje);
        }).always(function () { boton.prop('disabled', false); });
    });
});
