$(function () {
    const token = $('#token').val();
    const escapar = valor => $('<div>').text(valor ?? '').html();
    const moneda = new Intl.NumberFormat('es-PY', {maximumFractionDigits: 0});
    const roles = String($('#roles').text() || '');
    const puedeVerAuditoria = roles.includes('ROOT') || roles.includes('ADMINISTRADOR');

    $('#tablaCategorias').DataTable({
        processing: true,
        serverSide: true,
        searchDelay: 300,
        pageLength: 10,
        lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
        order: [[1, 'asc']],
        dom: 'lfrtip',
        ajax: {
            url: '/categoria/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: datos => { datos._csrf = token; },
            dataSrc: function (respuesta) {
                $('#categoriaTotal').text(respuesta.recordsTotal);
                return respuesta.data;
            }
        },
        columns: [
            {data: 'codigo'},
            {data: 'nombre', render: nombre => '<span class="categoria-nombre">' + escapar(nombre) + '</span>'},
            {data: 'tarifa', render: tarifa => '<span class="categoria-tarifa">Gs. ' + moneda.format(Number(tarifa || 0)) + '</span>'},
            {
                data: null,
                render: function (_, __, fila) {
                    return '<span class="categoria-sucursal">' + escapar(fila.sucursal) + '</span><small>' + escapar(fila.ciudad) + '</small>';
                }
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    const historial = puedeVerAuditoria
                            ? '<a href="/auditoria-entidades?entidad=CATEGORIA&identificador=' + encodeURIComponent(fila.codigo) + '" class="btn btn-outline-secondary btn-sm" title="Ver historial de modificaciones" aria-label="Ver historial de modificaciones"><i class="fa-solid fa-clock-rotate-left"></i></a> '
                            : '';
                    return historial + '<button type="button" class="btn btn-info btn-sm editar-categoria" data-id="' + fila.codigo + '" data-bs-toggle="modal" data-bs-target="#modificarModal" title="Editar categoría" aria-label="Editar categoría"><i class="fa-regular fa-pen-to-square"></i></button> '
                            + '<button type="button" id="eliminar" data-url="/categoria/eliminar/" data-id="' + fila.codigo + '" class="btn btn-danger btn-sm" title="Eliminar categoría" aria-label="Eliminar categoría"><i class="fa-solid fa-trash-can"></i></button>';
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });
});
