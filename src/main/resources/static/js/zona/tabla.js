$(function () {
    const token = $('#token').val();
    const escapar = valor => $('<div>').text(valor ?? '').html();

    $('#tablaZonas').DataTable({
        processing: true,
        serverSide: true,
        searchDelay: 300,
        pageLength: 10,
        lengthMenu: [[10, 20, 50, 100], [10, 20, 50, 100]],
        order: [[1, 'asc']],
        dom: 'lfrtip',
        ajax: {
            url: '/zona/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: datos => { datos._csrf = token; },
            dataSrc: function (respuesta) {
                $('#zonaTotal').text(respuesta.recordsTotal);
                return respuesta.data;
            }
        },
        columns: [
            {
                data: null,
                render: function (_, __, fila) {
                    return '<span class="zona-sucursal">' + escapar(fila.sucursal) + '</span>'
                            + '<small>' + escapar(fila.ciudad) + '</small>';
                }
            },
            {
                data: 'zona',
                render: zona => '<span class="zona-nombre">' + escapar(zona) + '</span>'
            },
            {
                data: 'cobrador',
                render: cobrador => '<span class="zona-cobrador-avatar"><i class="fa-regular fa-user"></i></span>'
                        + '<span>' + escapar(cobrador) + '</span>'
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    return '<a href="/manzana/agregar/' + fila.codigo + '" class="btn btn-primario btn-sm" title="Administrar manzanas" aria-label="Administrar manzanas"><i class="fa-solid fa-map"></i></a> '
                            + '<a href="/zona/editar/' + fila.codigo + '" class="btn btn-info btn-sm" title="Editar zona" aria-label="Editar zona"><i class="fa-regular fa-pen-to-square"></i></a> '
                            + '<button type="button" id="eliminar" data-url="/zona/eliminar/" data-id="' + fila.codigo + '" class="btn btn-danger btn-sm" title="Eliminar zona" aria-label="Eliminar zona"><i class="fa-solid fa-trash-can"></i></button>';
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });
});
