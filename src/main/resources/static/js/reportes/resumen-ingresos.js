$(function () {
    const moneda = valor => 'Gs. ' + Number(valor || 0).toLocaleString('es-PY', {maximumFractionDigits: 0});
    const iso = fecha => fecha.getFullYear() + '-' + String(fecha.getMonth() + 1).padStart(2, '0') + '-' + String(fecha.getDate()).padStart(2, '0');

    function fila(item) {
        return $('<tr>')
                .append($('<td>').text(item.nombre || 'Sin especificar'))
                .append($('<td>').text(Number(item.cantidad || 0).toLocaleString('es-PY')))
                .append($('<td>', {class: 'text-end fw-bold'}).text(moneda(item.importe)));
    }

    function llenar(cuerpo, datos) {
        cuerpo.empty();
        if (!datos.length) {
            cuerpo.append($('<tr>').append($('<td>', {colspan: 3, class: 'text-center text-muted py-4'}).text('Sin ingresos en el período.')));
            return;
        }
        datos.forEach(item => cuerpo.append(fila(item)));
    }

    function llenarDetalleCobrador(datos) {
        const cuerpo = $('#detalleCobradorMedio').empty();
        if (!datos.length) {
            cuerpo.append($('<tr>').append($('<td>', {colspan: 3, class: 'text-center text-muted py-4'}).text('Sin ingresos en el período.')));
            return;
        }
        const grupos = new Map();
        datos.forEach(function (item) {
            const clave = String(item.codigoCobrador);
            if (!grupos.has(clave)) grupos.set(clave, {nombre: item.cobrador, items: []});
            grupos.get(clave).items.push(item);
        });
        grupos.forEach(function (grupo) {
            cuerpo.append($('<tr>', {class: 'resumen-grupo-cobrador'})
                    .append($('<td>', {colspan: 3}).text(grupo.nombre || 'Sin cobrador')));
            let total = 0;
            let cantidad = 0;
            grupo.items.forEach(function (item) {
                total += Number(item.importe || 0);
                cantidad += Number(item.cantidad || 0);
                cuerpo.append($('<tr>')
                        .append($('<td>', {class: 'ps-4'}).text(item.medioPago || 'Sin especificar'))
                        .append($('<td>').text(Number(item.cantidad || 0).toLocaleString('es-PY')))
                        .append($('<td>', {class: 'text-end fw-bold'}).text(moneda(item.importe))));
            });
            cuerpo.append($('<tr>', {class: 'resumen-subtotal-cobrador'})
                    .append($('<td>').text('Total cobrado'))
                    .append($('<td>').text(cantidad.toLocaleString('es-PY')))
                    .append($('<td>', {class: 'text-end'}).text(moneda(total))));
        });
    }

    function llenarResumenGeneralMedios(datos) {
        llenar($('#resumenGeneralPorMedio'), datos);
        const cantidad = datos.reduce((total, item) => total + Number(item.cantidad || 0), 0);
        const importe = datos.reduce((total, item) => total + Number(item.importe || 0), 0);
        $('#cantidadGeneralPorMedio').text(cantidad.toLocaleString('es-PY'));
        $('#importeGeneralPorMedio').text(moneda(importe));
    }

    function consultar() {
        const desde = $('#resumenDesde').val();
        const hasta = $('#resumenHasta').val();
        const mensaje = $('#mensajeResumenIngresos').empty();
        if (!desde || !hasta || hasta < desde) {
            mensaje.html('<div class="alert alert-warning">Seleccione un rango de fechas válido.</div>');
            return;
        }
        const boton = $('#consultarResumenIngresos').prop('disabled', true);
        $.getJSON('/reporte/ingresos-v2/resumen/datos', {desde: desde, hasta: hasta})
                .done(function (respuesta) {
                    $('#resumenTotal').text(moneda(respuesta.total));
                    $('#resumenCantidad').text(Number(respuesta.comprobantes || 0).toLocaleString('es-PY'));
                    llenar($('#resumenPorCobrador'), respuesta.porCobrador || []);
                    llenarDetalleCobrador(respuesta.detallePorCobrador || []);
                    llenarResumenGeneralMedios(respuesta.porMedioPago || []);
                })
                .fail(function () {
                    mensaje.html('<div class="alert alert-danger">No fue posible obtener el resumen de ingresos.</div>');
                })
                .always(function () { boton.prop('disabled', false); });
    }

    const hoy = new Date();
    $('#resumenDesde').val(iso(new Date(hoy.getFullYear(), hoy.getMonth(), 1)));
    $('#resumenHasta').val(iso(hoy));
    $('#consultarResumenIngresos').on('click', consultar);
    $('#resumenDesde,#resumenHasta').on('change', consultar);
    consultar();
});
