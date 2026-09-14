import {egresosApi, catalogosApi} from './api.js';
import {aviso, confirmar, leerRango, moneda, mostrarError} from './vista.js';

function acciones(_, tipo, gasto) {
    if (tipo !== 'display') return '';
    const editar = `<a href="/egresos/editar/${gasto.id}" class="btn btn-info btn-sm editar" aria-label="${gasto.anulado ? 'Ver' : 'Editar'} gasto"><i class="fa-solid fa-pen"></i> ${gasto.anulado ? 'Ver' : 'Editar'}</a>`;
    const anular = `<button class="btn btn-danger btn-sm anular" data-id="${gasto.id}" title="Anular gasto"><i class="fa-solid fa-ban"></i> Anular</button>`;
    return editar + (gasto.anulado ? '' : ' ' + anular);
}

export function iniciarGastos() {
    $('#panelGastos').removeClass('d-none');
    const texto = $.fn.dataTable.render.text();
    const tabla = $('#tablaEgresos').DataTable({
        data: [], order: [[0, 'desc']], language: {url: '/i18n/es-ES.json'},
        columns: [
            {data: 'id'}, {data: 'tipo', render: texto}, {data: 'fecha'},
            {data: 'proveedor', render: texto},
            {data: 'total', render: (valor, tipo) => tipo === 'display' ? moneda(valor) : valor},
            {data: 'anulado', render: valor => valor ? 'Anulado' : 'Vigente'},
            {data: null, orderable: false, render: acciones}
        ]
    });
    let version = 0;

    async function cargar() {
        const actual = ++version;
        try {
            const rango = leerRango();
            const categoriaId = $('#categoriaFiltroEgreso').val();
            if (categoriaId) rango.categoriaId = categoriaId;
            sessionStorage.setItem('egresos-rango', JSON.stringify(rango));
            const datos = await egresosApi.listar(rango);
            if (actual !== version) return;
            tabla.clear().rows.add(datos).draw();
            const total = datos.filter(gasto => !gasto.anulado).reduce((suma, gasto) => suma + Number(gasto.total), 0);
            $('#totalPeriodo').text(moneda(total));
        } catch (error) { if (actual === version) mostrarError(error); }
    }

    $('#filtroEgresos').on('submit', evento => { evento.preventDefault(); cargar(); });
    $('#tablaEgresos').on('click', '.anular', async function () {
        const id = $(this).data('id');
        if (!await confirmar('Anular gasto', '¿Anular el gasto #' + id + '? Quedará registrado como anulado.')) return;
        try { await egresosApi.anular(id); aviso('Gasto anulado.'); await cargar(); }
        catch (error) { mostrarError(error); }
    });
    catalogosApi.buscar('CATEGORIA_PRODUCTO').then(categorias => {
        const selector = $('#categoriaFiltroEgreso').empty().append($('<option>').val('').text('Todas'));
        categorias.forEach(categoria => selector.append($('<option>').val(categoria.id).text(categoria.nombre)));
        try {
            const rango = JSON.parse(sessionStorage.getItem('egresos-rango')) || {};
            if (rango.categoriaId) selector.val(rango.categoriaId);
        } catch (_) { /* Sin filtro guardado. */ }
    }).catch(() => {});
    cargar();
}
