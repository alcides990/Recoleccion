$(function () {
    const moneda = valor => 'Gs. ' + Number(valor || 0).toLocaleString('es-PY', {maximumFractionDigits: 0});
    const iso = fecha => fecha.getFullYear() + '-' + String(fecha.getMonth() + 1).padStart(2, '0') + '-' + String(fecha.getDate()).padStart(2, '0');

    function fila(item) {
        return $('<div>', {class: 'resumen-linea'})
                .append($('<span>', {class: 'resumen-linea-nombre'}).text(item.nombre || 'Sin especificar'))
                .append($('<span>', {class: 'resumen-linea-cantidad'}).text(
                    Number(item.cantidad || 0).toLocaleString('es-PY')))
                .append($('<strong>', {class: 'resumen-linea-importe'}).text(moneda(item.importe)));
    }

    function llenar(cuerpo, datos) {
        cuerpo.empty();
        if (!datos.length) {
            cuerpo.append($('<span>', {class: 'resumen-vacio'}).text('Sin ingresos en el período.'));
            return;
        }
        datos.forEach(item => cuerpo.append(fila(item)));
    }

    function llenarDetalleCobrador(datos) {
        const cuerpo = $('#detalleCobradorMedio').empty();
        if (!datos.length) {
            cuerpo.append($('<span>', {class: 'resumen-vacio'}).text('Sin ingresos en el período.'));
            return;
        }
        datos.forEach(function (item) {
            const cobrador = item.cobrador || 'Sin cobrador';
            const medio = item.medioPago || 'Sin especificar';
            cuerpo.append($('<div>', {class: 'resumen-linea'})
                    .append($('<span>', {class: 'resumen-linea-nombre'}).text(cobrador + ' · ' + medio))
                    .append($('<span>', {class: 'resumen-linea-cantidad'}).text(
                        Number(item.cantidad || 0).toLocaleString('es-PY')))
                    .append($('<strong>', {class: 'resumen-linea-importe'}).text(moneda(item.importe))));
        });
    }

    function llenarResumenGeneralMedios(datos) {
        const cuerpo = $('#resumenGeneralPorMedio').empty();
        if (!datos.length) {
            cuerpo.append($('<span>', {class: 'resumen-vacio'}).text('Sin ingresos en el período.'));
        } else {
            datos.forEach(function (item) {
                cuerpo.append($('<div>', {class: 'resumen-medio-item'})
                        .append($('<span>').text(item.nombre || 'Sin especificar'))
                        .append($('<small>').text(Number(item.cantidad || 0).toLocaleString('es-PY')))
                        .append($('<strong>').text(moneda(item.importe))));
            });
        }
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
