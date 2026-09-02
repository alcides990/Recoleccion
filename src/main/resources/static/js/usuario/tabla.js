$(function () {
    const token = $('#token').val();
    const roles = String($('#roles').text() || '');
    const puedeAdministrar = roles.includes('ROOT') || roles.includes('ADMINISTRADOR');
    const puedeEditar = puedeAdministrar || roles.includes('SUPERVISOR');
    const puedeEliminar = puedeEditar;
    const escapar = valor => $('<div>').text(valor ?? '').html();

    $('#tablaUsuarios').DataTable({
        processing: true,
        serverSide: true,
        searchDelay: 300,
        pageLength: 10,
        lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
        order: [[2, 'asc']],
        dom: 'lfrtip',
        ajax: {
            url: '/usuario/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: datos => { datos._csrf = token; },
            dataSrc: function (respuesta) {
                $('#usuarioTotal').text(respuesta.recordsTotal);
                return respuesta.data;
            }
        },
        columns: [
            {data: 'codigo'},
            {data: 'documento'},
            {
                data: 'nombre',
                render: nombre => '<span class="usuario-list-nombre">' + escapar(nombre) + '</span>'
            },
            {
                data: null,
                render: function (_, __, fila) {
                    const celular = fila.celular ? '<span><i class="fa-solid fa-mobile-screen"></i> ' + escapar(fila.celular) + '</span>' : '';
                    const correo = fila.correo ? '<small><i class="fa-solid fa-envelope"></i> ' + escapar(fila.correo) + '</small>' : '';
                    return '<div class="usuario-list-doble">' + celular + correo + (celular || correo ? '' : '<small>Sin contacto</small>') + '</div>';
                }
            },
            {
                data: null,
                render: function (_, __, fila) {
                    return '<div class="usuario-list-doble"><span>' + escapar(fila.barrio || 'Sin barrio')
                            + '</span><small>' + escapar(fila.direccion || 'Sin dirección') + '</small></div>';
                }
            },
            {
                data: null,
                render: function (_, __, fila) {
                    return '<div class="usuario-list-doble"><span>' + escapar(fila.sucursal)
                            + '</span><small>' + escapar(fila.ciudad) + '</small></div>';
                }
            },
            {
                data: 'estado',
                render: function (estado) {
                    const activo = String(estado || '').toUpperCase() === 'ACTIVO';
                    return '<span class="usuario-list-estado ' + (activo ? 'activo' : 'inactivo') + '">'
                            + escapar(estado) + '</span>';
                }
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    let acciones = '';
                    if (puedeAdministrar) {
                        acciones += '<a href="/auditoria-entidades?entidad=USUARIO&identificador=' + encodeURIComponent(fila.codigo) + '" class="btn btn-outline-secondary btn-sm" title="Ver historial de modificaciones" aria-label="Ver historial de modificaciones"><i class="fa-solid fa-clock-rotate-left"></i></a> ';
                    }
                    if (puedeEditar) {
                        acciones += '<a href="/usuario/editar/' + fila.codigo + '" class="btn btn-info btn-sm" title="Editar usuario" aria-label="Editar usuario"><i class="fa-regular fa-pen-to-square"></i></a> ';
                    }
                    if (puedeEliminar) {
                        acciones += '<button type="button" id="eliminar" data-url="/usuario/eliminar/" data-id="' + fila.codigo + '" class="btn btn-danger btn-sm" title="Eliminar usuario" aria-label="Eliminar usuario"><i class="fa-solid fa-trash-can"></i></button>';
                    }
                    return acciones;
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });
});
