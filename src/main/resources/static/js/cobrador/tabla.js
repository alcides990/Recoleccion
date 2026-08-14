$(function () {
    const token = $('#token').val();
    const roles = String($('#roles').text() || '');
    const puedeAdministrar = roles.includes('ROOT') || roles.includes('ADMIN');
    const escapar = valor => $('<div>').text(valor ?? '').html();

    $('#tablaCobradores').DataTable({
        processing: true,
        serverSide: true,
        searchDelay: 300,
        pageLength: 10,
        lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
        order: [[1, 'asc']],
        dom: 'lfrtip',
        ajax: {
            url: '/cobrador/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: datos => { datos._csrf = token; },
            dataSrc: function (respuesta) {
                $('#cobradorTotal').text(respuesta.recordsTotal);
                return respuesta.data;
            }
        },
        columns: [
            {data: 'codigo'},
            {
                data: 'nombre',
                render: nombre => '<div class="cobrador-list-persona"><span class="cobrador-list-avatar"><i class="fa-regular fa-user"></i></span><strong>' + escapar(nombre) + '</strong></div>'
            },
            {
                data: 'celular',
                render: celular => celular ? '<i class="fa-solid fa-mobile-screen cobrador-list-muted"></i> ' + escapar(celular) : '<span class="cobrador-list-muted">Sin celular</span>'
            },
            {data: 'direccion', defaultContent: '', render: direccion => escapar(direccion || 'Sin dirección')},
            {
                data: 'estado',
                render: function (estado) {
                    const activo = String(estado || '').toUpperCase() === 'ACTIVO';
                    return '<span class="cobrador-list-estado ' + (activo ? 'activo' : 'inactivo') + '">' + escapar(estado) + '</span>';
                }
            },
            {
                data: null,
                render: function (_, __, fila) {
                    return '<div class="cobrador-list-doble"><span>' + escapar(fila.sucursal) + '</span><small>' + escapar(fila.ciudad) + '</small></div>';
                }
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    if (!puedeAdministrar) return '';
                    return '<a href="/cobrador/editar/' + fila.codigo + '" class="btn btn-info btn-sm" title="Editar cobrador" aria-label="Editar cobrador"><i class="fa-regular fa-pen-to-square"></i></a> '
                            + '<button type="button" id="eliminar" data-url="/cobrador/eliminar/" data-id="' + fila.codigo + '" class="btn btn-danger btn-sm" title="Eliminar cobrador" aria-label="Eliminar cobrador"><i class="fa-solid fa-trash-can"></i></button>';
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });
});
