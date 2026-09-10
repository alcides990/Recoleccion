import {iniciarGastos} from './gastos.js';
import {iniciarFormulario} from './gasto-formulario.js';
import {iniciarCatalogos} from './catalogos.js';
import {fechaActual} from './vista.js';
import {datePickerInit} from '/js/utils.js';

$(function () {
    const partes = window.location.pathname.split('/').filter(Boolean);
    if (partes[1] === 'catalogos') {
        iniciarCatalogos(partes[2], partes[3], partes[4] ? Number(partes[4]) : null);
    } else if (partes[1] === 'agregar' || partes[1] === 'editar') {
        datePickerInit('#fechaEgreso');
        iniciarFormulario(partes[2] ? Number(partes[2]) : null);
    } else {
        const hoy = fechaActual();
        let rango = {desde: hoy.substring(0, 8) + '01', hasta: hoy};
        try { rango = JSON.parse(sessionStorage.getItem('egresos-rango')) || rango; } catch (_) { /* Usa el mes actual. */ }
        $('#desdeEgreso').val(rango.desde);
        $('#hastaEgreso').val(rango.hasta);
        datePickerInit('#desdeEgreso');
        datePickerInit('#hastaEgreso');
        iniciarGastos();
    }
});
