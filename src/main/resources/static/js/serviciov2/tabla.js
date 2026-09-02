$(function () {
    const tablaElemento = $('#tabla-servicio');
    if (tablaElemento.data('server-side') !== true) return;

    const token = $('#token').val();
    const escapar = valor => $('<div>').text(valor ?? '').html();
    const roles = String($('#roles').text() || '');
    const puedeEditar = roles.includes('ROOT') || roles.includes('ADMINISTRADOR') || roles.includes('SUPERVISOR');
    const puedeEliminar = puedeEditar;
    const puedeVerAuditoria = roles.includes('ROOT') || roles.includes('ADMINISTRADOR');

    const tabla = tablaElemento.DataTable({
        processing: true,
        serverSide: true,
        searchDelay: 300,
        pageLength: 10,
        lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
        order: [[0, 'asc']],
        dom: 'lfrtip',
        ajax: {
            url: '/servicio/tabla-v2',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: datos => { datos._csrf = token; }
        },
        columns: [
            {data: 'cuentaCorriente'},
            {data: 'usuario'},
            {data: 'fechaInicio'},
            {data: 'ocupado', render: valor => valor === 'BALDIO'
                    ? '<span class="badge bg-warning text-dark">Baldío</span>'
                    : (valor === 'DESOCUPADO'
                        ? '<span class="badge bg-secondary">Desocupado</span>'
                        : '<span class="badge bg-success">Ocupado</span>')},
            {data: 'categoria'},
            {data: 'estado'},
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    const cuenta = escapar(fila.cuentaCorriente);
                    let acciones = '<button type="button" class="btn btn-primario btn-sm estado-cuenta" data-id="' + cuenta + '">Estado Cuenta</button> ';
                    acciones += '<button type="button" class="btn btn-success btn-sm ubicacion-servicio" data-id="' + cuenta + '" title="Ubicación del servicio"><i class="fa-solid fa-location-dot"></i></button> ';
                    if (puedeVerAuditoria) {
                        acciones += '<a href="/auditoria-entidades?entidad=CUENTA&identificador=' + encodeURIComponent(fila.cuentaCorriente) + '" class="btn btn-outline-secondary btn-sm" title="Ver historial de modificaciones" aria-label="Ver historial de modificaciones"><i class="fa-solid fa-clock-rotate-left"></i></a> ';
                    }
                    if (puedeEditar) {
                        acciones += '<a id="editar" data-id="' + cuenta + '" class="btn btn-info btn-sm" data-bs-toggle="modal" data-bs-target="#servicioModal" title="Editar"><i class="fa-regular fa-pen-to-square fa-lg"></i></a> ';
                    }
                    if (puedeEliminar) {
                        acciones += '<a id="eliminar" data-url="/servicio/eliminar" data-id="' + cuenta + '" class="btn btn-danger eliminar btn-sm" title="Eliminar"><i class="fa-solid fa-trash-can"></i></a>';
                    }
                    return acciones;
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });

});
