import {catalogosApi} from './api.js';
import {confirmacioModal} from '/js/modulos.js';

export function moneda(valor) {
    return 'Gs. ' + Number(valor || 0).toLocaleString('es-PY', {maximumFractionDigits: 2});
}

export function escapar(valor) {
    return $('<div>').text(valor ?? '').html();
}

export function aviso(texto, error = false) {
    $('#mensajeEgreso').empty().append($('<div>')
        .addClass('alert ' + (error ? 'alert-danger' : 'alert-success')).text(texto));
}

export function mostrarError(error) {
    aviso(error.message || 'No se pudo completar la operación.', true);
}

export function confirmar(titulo, mensaje) {
    return new Promise(resolve => {
        confirmacioModal(titulo, mensaje);
        const modal = $('#confirmacionModal');
        modal.one('hidden.bs.modal', () => resolve(false));
        modal.one('click', '#confirmarBoton', () => {
            modal.off('hidden.bs.modal');
            modal.modal('hide');
            resolve(true);
        });
        modal.modal('show');
    });
}

export function fechaActual() {
    const hoy = new Date();
    return [hoy.getFullYear(), String(hoy.getMonth() + 1).padStart(2, '0'),
        String(hoy.getDate()).padStart(2, '0')].join('-');
}

export function leerRango() {
    const desde = $('#desdeEgreso').val();
    const hasta = $('#hastaEgreso').val();
    if (!desde || !hasta || desde > hasta) throw new Error('Seleccione un rango de fechas válido.');
    return {desde, hasta};
}

export function autocompletar(input, clase) {
    input.autocomplete({
        minLength: 1,
        delay: 250,
        source: (consulta, responder) => catalogosApi.buscar(clase, consulta.term)
            .then(datos => responder(datos.map(dato => ({label: dato.nombre, value: dato.nombre}))))
            .catch(() => responder([]))
    });
}
