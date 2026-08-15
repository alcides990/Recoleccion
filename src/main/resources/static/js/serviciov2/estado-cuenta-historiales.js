import {datePickerInit} from '/js/modulos.js';

$(function () {
    datePickerInit('#suspensionFecha');
    datePickerInit('#exoneracionFecha');
    const token = $('#token').val();
    const modal = new bootstrap.Modal(document.getElementById('historialServicioModal'));
    const numero = new Intl.NumberFormat('es-PY', {maximumFractionDigits: 0});
    const escapar = valor => $('<div>').text(valor ?? '').html();
    const hoy = () => new Date().toISOString().slice(0, 10);
    let tablaPagos = null;

    const cuentaActual = () => String($('#cuentaCorriente').val() || '');

    function preparar(titulo, panel) {
        $('#historialServicioTitulo').text(titulo + ' · ' + cuentaActual());
        $('#historialServicioAlerta').addClass('d-none').text('');
        $('#panelSuspensiones,#panelExoneraciones,#panelPagos').addClass('d-none');
        $(panel).removeClass('d-none');
        modal.show();
    }

    async function api(url, opciones = {}) {
        const respuesta = await fetch(url, opciones);
        if (!respuesta.ok) throw new Error(await respuesta.text() || 'No se pudo completar la operación');
        const tipo = respuesta.headers.get('content-type') || '';
        return tipo.includes('json') ? respuesta.json() : respuesta.text();
    }

    function mostrarError(error) {
        $('#historialServicioAlerta').removeClass('d-none').text(error.message || error);
    }

    async function cargarSuspensiones() {
        const datos = await api('/servicio/historial/suspensiones?cuentaCorriente=' + encodeURIComponent(cuentaActual()));
        $('#suspensionesCuerpo').html(datos.length ? datos.map(item => `<tr>
            <td>${escapar(item.fechaDesde)}</td><td>${escapar(item.fechaHasta || 'Activa')}</td>
            <td><span class="badge bg-info text-dark">${escapar(item.cantidadPeriodosPendientes)} mes(es)</span></td>
            <td>${escapar(item.motivo)}</td><td>${escapar(item.usuario)}</td>
            <td>${item.fechaHasta ? '' : '<button class="btn btn-success btn-sm finalizar-suspension"><i class="fa-solid fa-play"></i> Reactivar</button>'}</td>
        </tr>`).join('') : '<tr><td colspan="6" class="text-center text-muted">Sin suspensiones registradas</td></tr>');
    }

    $('.historial-suspensiones').on('click', async function () {
        preparar('Historial de suspensión', '#panelSuspensiones');
        $('#suspensionFecha').val(hoy());
        try { await cargarSuspensiones(); } catch (error) { mostrarError(error); }
    });

    $('#formSuspension').on('submit', async function (event) {
        event.preventDefault();
        try {
            await api('/servicio/historial/suspensiones', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}, body: JSON.stringify({cuentaCorriente: cuentaActual(), fechaDesde: $('#suspensionFecha').val(), motivo: $('#suspensionMotivo').val()})});
            $('#suspensionMotivo').val(''); await cargarSuspensiones();
        } catch (error) { mostrarError(error); }
    });

    $(document).on('click', '.finalizar-suspension', async function () {
        try {
            await api('/servicio/historial/suspensiones/finalizar', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}, body: JSON.stringify({cuentaCorriente: cuentaActual(), fechaHasta: hoy()})});
            await cargarSuspensiones();
        } catch (error) { mostrarError(error); }
    });

    async function cargarExoneraciones() {
        const datos = await api('/servicio/historial/exoneraciones?cuentaCorriente=' + encodeURIComponent(cuentaActual()));
        $('#exoneracionesCuerpo').html(datos.length ? datos.map(item => `<tr><td>${escapar(item.fechaDesdeAnterior)}</td><td>${escapar(item.fechaDesdeNueva)}</td><td><span class="badge bg-warning text-dark">${escapar(item.cantidadPeriodos)} mes(es)</span></td><td>${escapar(item.motivo)}</td><td>${escapar(item.usuario)}</td></tr>`).join('') : '<tr><td colspan="5" class="text-center text-muted">Sin exoneraciones registradas</td></tr>');
    }

    $('.historial-exoneraciones').on('click', async function () {
        preparar('Historial de exoneración', '#panelExoneraciones');
        $('#exoneracionFecha').val(hoy());
        try { await cargarExoneraciones(); } catch (error) { mostrarError(error); }
    });

    $('#formExoneracion').on('submit', async function (event) {
        event.preventDefault();
        try {
            await api('/servicio/historial/exoneraciones', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}, body: JSON.stringify({cuentaCorriente: cuentaActual(), fechaDesdeNueva: $('#exoneracionFecha').val(), motivo: $('#exoneracionMotivo').val()})});
            $('#exoneracionMotivo').val(''); await cargarExoneraciones();
        } catch (error) { mostrarError(error); }
    });

    $('.historial-pagos').on('click', function () {
        preparar('Historial de pagos', '#panelPagos');
        if (tablaPagos) { tablaPagos.destroy(); $('#tablaHistorialPagos tbody').empty(); }
        tablaPagos = $('#tablaHistorialPagos').DataTable({processing: true, serverSide: true, searchDelay: 300, pageLength: 10, order: [[1, 'desc']],
            ajax: {url: '/servicio/historial/pagos', type: 'GET', data: datos => { datos.cuentaCorriente = cuentaActual(); }, error: xhr => mostrarError(xhr.responseText || 'No se pudo cargar el historial de pagos')},
            columns: [{data: 'numeroComprobante'}, {data: 'fechaPago'}, {data: 'periodoPago', defaultContent: ''}, {data: 'cantidadPago'}, {data: 'totalImporte', render: valor => numero.format(valor || 0)}, {data: 'estado'}],
            language: {url: '/i18n/es-ES.json'}
        });
    });

    $('#btnExtractoPdfHistorial').on('click', async function () {
        const boton = $(this);
        const contenidoOriginal = boton.html();
        const estado = $('#extractoEstadoHistorial');
        boton.prop('disabled', true).html('<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> <span>Generando…</span>');
        estado.removeClass('d-none alert-danger alert-success').addClass('alert-info')
                .html('<i class="fa-solid fa-circle-notch fa-spin"></i> Generando el extracto completo. Espere un momento…');
        const visorPdf = window.open('', '_blank');
        if (visorPdf) {
            visorPdf.document.write('<!doctype html><html><head><title>Generando extracto</title></head><body style="font-family:sans-serif;padding:2rem"><p>Generando extracto PDF…</p></body></html>');
        }
        try {
            const cuerpo = new URLSearchParams({cuentaCorriente: cuentaActual(), _csrf: token});
            const respuesta = await fetch('/servicio/extractoCuenta', {
                method: 'POST',
                headers: {'X-CSRF-TOKEN': token, 'Content-Type': 'application/x-www-form-urlencoded'},
                body: cuerpo
            });
            if (!respuesta.ok) {
                const tipo = respuesta.headers.get('content-type') || '';
                const detalle = tipo.includes('json') ? await respuesta.json() : await respuesta.text();
                throw new Error(typeof detalle === 'string' ? detalle : (detalle.mensaje || detalle.message || 'No se pudo generar el extracto PDF'));
            }
            const urlPdf = URL.createObjectURL(await respuesta.blob());
            if (visorPdf) {
                visorPdf.location.replace(urlPdf);
            } else {
                window.open(urlPdf, '_blank');
            }
            setTimeout(() => URL.revokeObjectURL(urlPdf), 60000);
            estado.removeClass('alert-info').addClass('alert-success')
                    .html('<i class="fa-solid fa-circle-check"></i> Extracto generado y abierto para visualizar.');
        } catch (error) {
            if (visorPdf && !visorPdf.closed) visorPdf.close();
            estado.removeClass('alert-info').addClass('alert-danger')
                    .html('<i class="fa-solid fa-triangle-exclamation"></i> ' + escapar(error.message || error));
        } finally {
            boton.prop('disabled', false).html(contenidoOriginal);
        }
    });

});
