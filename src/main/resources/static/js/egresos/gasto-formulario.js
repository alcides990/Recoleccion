import {egresosApi, catalogosApi} from './api.js';
import {iniciarDetalles} from './detalles.js';
import {aviso, autocompletar, fechaActual, mostrarError} from './vista.js';
import {tabuladorFormulario} from '/js/utils.js';

export async function iniciarFormulario(id) {
    $('#editorEgreso').removeClass('d-none');
    const detalles = iniciarDetalles();
    let soloLectura = false;
    let listo = false;
    let guardando = false;
    autocompletar($('#proveedorEgreso'), 'PROVEEDOR');

    $('#formEgreso').on('submit', async evento => {
        evento.preventDefault();
        if (!listo || soloLectura || guardando) return;
        if (detalles.pendiente()) {
            aviso('Hay un producto pendiente. Agréguelo al detalle con Enter o con el botón Agregar.', true);
            return;
        }
        const lineas = detalles.leer();
        if (!lineas.length) { aviso('Ingrese al menos un producto o servicio.', true); return; }
        guardando = true;
        $('#guardarEgreso').prop('disabled', true);
        try {
            await egresosApi.guardar(id, {
                fecha: $('#fechaEgreso').val(), tipoId: Number($('#tipoEgreso').val()),
                proveedor: $('#proveedorEgreso').val(), comprobante: $('#comprobanteEgreso').val(),
                observacion: $('#observacionEgreso').val(), detalles: lineas
            });
            window.location.assign('/egresos');
        } catch (error) {
            mostrarError(error);
            guardando = false;
            $('#guardarEgreso').prop('disabled', false);
        }
    });

    try {
        const [gasto, tipos] = await Promise.all([id ? egresosApi.detalle(id) : null, catalogosApi.buscar('TIPO')]);
        soloLectura = Boolean(gasto?.anulado);
        $('#tituloEgreso').text(id ? 'Gasto #' + id + (soloLectura ? ' - Anulado' : '') : 'Gasto');
        $('#fechaEgreso').val(gasto?.fecha || fechaActual());
        $('#proveedorEgreso').val(gasto?.proveedor || '');
        $('#comprobanteEgreso').val(gasto?.comprobante || '');
        $('#observacionEgreso').val(gasto?.observacion || '');
        const selector = $('#tipoEgreso').empty().append($('<option>').val('').text('Seleccione un tipo'));
        tipos.forEach(tipo => selector.append($('<option>').val(tipo.id).text(tipo.nombre)));
        if (gasto) {
            if (!tipos.some(tipo => tipo.id === gasto.tipo_id)) selector.append($('<option>').val(gasto.tipo_id).text(gasto.tipo));
            selector.val(gasto.tipo_id);
        }
        detalles.cargar(gasto?.detalles || [], soloLectura);
        if (!tipos.length && !gasto) aviso('Registre primero un tipo de gasto desde el menú Tipos de Gastos.', true);
        listo = Boolean(tipos.length || gasto);
        $('#formEgreso :input').prop('disabled', soloLectura);
        $('#guardarEgreso').prop('disabled', !listo || soloLectura);
        tabuladorFormulario('#formEgreso, #formDetalleEgreso');
    } catch (error) {
        $('#formEgreso :input, #formDetalleEgreso :input').prop('disabled', true);
        mostrarError(error);
    }
}
