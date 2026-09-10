import {catalogosApi} from './api.js';
import {aviso, confirmar, escapar, moneda, mostrarError} from './vista.js';
import {tabuladorFormulario} from '/js/utils.js';

const configuraciones = {
    TIPO: {lista: 'Lista de Tipos de Gastos', formulario: 'Datos Tipo de Gasto', nombre: 'Nombre', columnas: [['id', 'Id'], ['nombre', 'Nombre']]},
    PROVEEDOR: {lista: 'Lista de Proveedores', formulario: 'Datos Proveedor', nombre: 'Nombre/Razón Social',
        columnas: [['id', 'Id'], ['documento', 'RUC'], ['nombre', 'Razón Social'], ['telefono', 'Celular'], ['correo', 'Correo'], ['direccion', 'Dirección']]},
    PRODUCTO: {lista: 'Lista de Productos y Servicios', formulario: 'Datos Producto/Servicio', nombre: 'Descripción', columnas: [['id', 'Id'], ['nombre', 'Descripción'], ['categoria', 'Categoría'], ['monto', 'Monto']]},
    CATEGORIA_PRODUCTO: {lista: 'Lista de Categorías de Productos', formulario: 'Datos Categoría de Producto', nombre: 'Nombre', columnas: [['id', 'Id'], ['nombre', 'Nombre']]}
};

export function iniciarCatalogos(clase, modo, id) {
    const configuracion = configuraciones[clase];
    if (!configuracion) { mostrarError(new Error('Catálogo no válido.')); return; }
    const base = '/egresos/catalogos/' + clase;
    $('#tituloCatalogo').text(configuracion.lista);
    $('#tituloEditorCatalogo').text(configuracion.formulario);
    $('#nuevoCatalogo').attr('href', base + '/agregar');
    $('#volverCatalogo').attr('href', base + '/lista');
    $('#etiquetaNombreCatalogo').text(configuracion.nombre);
    $('.campoProveedor').toggleClass('d-none', clase !== 'PROVEEDOR');
    $('.campoProducto').toggleClass('d-none', clase !== 'PRODUCTO');
    if (modo !== 'lista') { iniciarEditor(clase, id, base); return; }

    $('#panelCatalogo').removeClass('d-none');
    $('#cabeceraCatalogo').html(configuracion.columnas.map(([, titulo]) => '<th>' + titulo + '</th>').join('') + '<th>Acciones</th>');
    let version = 0;
    let espera;
    async function cargar() {
        const actual = ++version;
        try {
            const datos = await catalogosApi.buscar(clase, $('#buscarCatalogo').val());
            if (actual !== version) return;
            $('#filasCatalogo').html(datos.map(dato => '<tr>' + configuracion.columnas.map(([campo]) =>
                '<td>' + escapar(campo === 'monto' ? moneda(dato[campo]) : dato[campo]) + '</td>').join('') +
                `<td class="text-nowrap"><a class="btn btn-info btn-sm editarCatalogo" href="${base}/editar/${dato.id}"><i class="fa-solid fa-pen"></i> Editar</a>
                 <button class="btn btn-danger btn-sm eliminarCatalogo" data-id="${dato.id}"><i class="fa-solid fa-trash-can"></i> Eliminar</button></td></tr>`).join(''));
        } catch (error) { if (actual === version) mostrarError(error); }
    }
    $('#buscarCatalogo').on('input', () => { clearTimeout(espera); espera = setTimeout(cargar, 250); });
    $('#filasCatalogo').on('click', '.eliminarCatalogo', async function () {
        if (!await confirmar('Eliminar registro', '¿Eliminar este registro? Solo se permite si no tiene gastos asociados.')) return;
        try { await catalogosApi.eliminar(clase, $(this).data('id')); aviso('Registro eliminado.'); await cargar(); }
        catch (error) { mostrarError(error); }
    });
    cargar();
}

async function iniciarEditor(clase, id, base) {
    $('#editorCatalogo').removeClass('d-none');
    $('#guardarCatalogo').prop('disabled', true);
    let guardando = false;
    try {
        if (clase === 'PRODUCTO') {
            const categorias = await catalogosApi.buscar('CATEGORIA_PRODUCTO');
            const selector = $('#categoriaCatalogo').empty().append($('<option>').val('').text('Sin categoría'));
            categorias.forEach(categoria => selector.append($('<option>').val(categoria.id).text(categoria.nombre)));
        }
        if (id) {
            const dato = await catalogosApi.detalle(clase, id);
            $('#codigoCatalogo').val(dato.id);
            for (const campo of ['nombre', 'documento', 'telefono', 'direccion', 'correo', 'monto']) {
                $('#' + campo + 'Catalogo').val(dato[campo] ?? '');
            }
            $('#categoriaCatalogo').val(dato.categoriaId || '');
        }
        $('#guardarCatalogo').prop('disabled', false);
        tabuladorFormulario('#formCatalogo');
    } catch (error) { mostrarError(error); return; }
    $('#formCatalogo').on('submit', async evento => {
        evento.preventDefault();
        if (guardando) return;
        guardando = true;
        $('#guardarCatalogo').prop('disabled', true);
        try {
            await catalogosApi.guardar(clase, {
                id, nombre: $('#nombreCatalogo').val(), documento: $('#documentoCatalogo').val(),
                telefono: $('#telefonoCatalogo').val(), direccion: $('#direccionCatalogo').val(),
                correo: $('#correoCatalogo').val(), monto: Number($('#montoCatalogo').val() || 0),
                categoriaId: $('#categoriaCatalogo').val() ? Number($('#categoriaCatalogo').val()) : null
            });
            window.location.assign(base + '/lista');
        } catch (error) {
            mostrarError(error); guardando = false; $('#guardarCatalogo').prop('disabled', false);
        }
    });
}
