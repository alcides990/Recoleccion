$(function () {
    const token = $('#token').val();
    const roles = String($('#roles').text() || '');
    const esRoot = roles.includes('ROOT');
    const administra = esRoot || roles.includes('ADMINISTRADOR');

    $('#tablaUsuariosSistema').DataTable({
        processing: true,
        serverSide: true,
        searchDelay: 300,
        pageLength: 10,
        lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
        order: [[1, 'asc']],
        dom: 'lfrtip',
        ajax: {
            url: '/usuarioSistema/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: datos => { datos._csrf = token; }
        },
        columns: [
            {data: 'codigo'},
            {data: 'nombre'},
            {data: 'sucursal'},
            {
                data: 'estado',
                render: function (estado) {
                    const activo = String(estado || '').toUpperCase() === 'ACTIVO';
                    return '<span class="usuarios-sistema-estado ' + (activo ? 'activo' : 'inactivo') + '">'
                            + $('<div>').text(estado || '').html() + '</span>';
                }
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    if (!administra) return '';
                    let acciones = '<a href="/auditoria-entidades?entidad=USUARIO_SISTEMA&identificador=' + encodeURIComponent(fila.codigo)
                            + '" class="btn btn-outline-secondary btn-sm" title="Ver historial de modificaciones" aria-label="Ver historial de modificaciones"><i class="fa-solid fa-clock-rotate-left"></i></a> '
                            + '<a href="/detalleUsuarioSistema/agregar/' + fila.codigo
                            + '" class="btn btn-primario btn-sm" title="Administrar roles" aria-label="Administrar roles"><i class="fa-solid fa-user-gear"></i></a> ';
                    if (esRoot) {
                        acciones += '<a href="/usuarioSistema/editar/' + fila.codigo
                                + '" class="btn btn-info btn-sm" title="Editar usuario" aria-label="Editar usuario"><i class="fa-regular fa-pen-to-square"></i></a> ';
                    }
                    acciones += '<button type="button" id="eliminar" data-url="/usuarioSistema/eliminar/" data-id="'
                            + fila.codigo + '" class="btn btn-danger btn-sm" title="Eliminar usuario" aria-label="Eliminar usuario"><i class="fa-solid fa-trash-can"></i></button>';
                    return acciones;
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });
});
