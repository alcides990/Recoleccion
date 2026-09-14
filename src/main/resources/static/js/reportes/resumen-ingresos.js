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
        const grupos = new Map();
        const mediosOrdenados = [];
        datos.forEach(function (item) {
            const cobrador = item.cobrador || 'Sin cobrador';
            const medio = item.medioPago || 'Sin especificar';
            if (!mediosOrdenados.includes(medio)) {
                mediosOrdenados.push(medio);
            }
            if (!grupos.has(cobrador)) {
                grupos.set(cobrador, {cobrador: cobrador, medios: new Map()});
            }
            const grupo = grupos.get(cobrador);
            grupo.medios.set(medio, {
                cantidad: Number(item.cantidad || 0),
                importe: Number(item.importe || 0)
            });
        });

        const columnas = ['minmax(6.8rem, 1fr)'].concat(mediosOrdenados.map(() => 'minmax(5.1rem, .72fr)')).join(' ');
        const totales = new Map(mediosOrdenados.map(medio => [medio, {cantidad: 0, importe: 0}]));
        cuerpo.css('--resumen-detalle-columnas', columnas);
        cuerpo.append($('<div>', {class: 'resumen-detalle-cabecera'})
                .append($('<span>').text('Cobrador')));
        const cabecera = cuerpo.find('.resumen-detalle-cabecera');
        mediosOrdenados.forEach(medio => cabecera.append($('<span>').text(medio)));

        [...grupos.values()].forEach(function (grupo) {
            const filaDetalle = $('<div>', {class: 'resumen-linea resumen-linea-compacta'})
                    .append($('<span>', {class: 'resumen-linea-nombre'}).text(grupo.cobrador));
            mediosOrdenados.forEach(function (medio) {
                const item = grupo.medios.get(medio);
                if (item) {
                    const total = totales.get(medio);
                    total.cantidad += item.cantidad;
                    total.importe += item.importe;
                }
                filaDetalle.append($('<span>', {class: 'resumen-celda-medio'})
                        .append($('<small>').text(item ? item.cantidad.toLocaleString('es-PY') : '-'))
                        .append($('<strong>').text(item ? moneda(item.importe) : '-')));
            });
            cuerpo.append(filaDetalle);
        });
        const filaTotales = $('<div>', {class: 'resumen-detalle-totales'})
                .append($('<span>').text('Totales'));
        mediosOrdenados.forEach(function (medio) {
            const total = totales.get(medio);
            filaTotales.append($('<span>', {class: 'resumen-celda-medio'})
                    .append($('<small>').text(total.cantidad.toLocaleString('es-PY')))
                    .append($('<strong>').text(moneda(total.importe))));
        });
        cuerpo.append(filaTotales);
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
