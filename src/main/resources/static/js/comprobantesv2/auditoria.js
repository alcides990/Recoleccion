$(function () {
    const token = $('#token').val();
    const escapar = valor => $('<div>').text(valor ?? '').html();
    const formatearFecha = function (valor) {
        if (!valor) return '';
        const partes = String(valor).replace('T', ' ').split(/[- :]/);
        return partes.length >= 6
                ? partes[2] + '/' + partes[1] + '/' + partes[0] + ' '
                    + partes[3] + ':' + partes[4] + ':' + partes[5].substring(0, 2)
                : valor;
    };

    const tabla = $('#tablaAuditoriaComprobantes').DataTable({
        processing: true,
        serverSide: true,
        searching: true,
        searchDelay: 350,
        scrollX: true,
        autoWidth: false,
        pageLength: 25,
        lengthMenu: [[10, 25, 50, 100], [10, 25, 50, 100]],
        pagingType: 'full_numbers',
        order: [[0, 'desc']],
        ajax: {
            url: '/comprobantes-v2/auditoria/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: function (datos) {
                datos._csrf = token;
                datos.numero = $('#fNumeroAuditoria').val();
                datos.accion = $('#fAccionAuditoria').val();
            }
        },
        columns: [
            {data: 'fecha', className: 'text-nowrap', render: formatearFecha},
            {data: 'accion', render: valor => '<span class="badge bg-secondary">' + escapar(valor) + '</span>'},
            {data: 'documento', className: 'text-nowrap fw-semibold', render: $.fn.dataTable.render.text()},
            {data: 'timbrado', render: $.fn.dataTable.render.text()},
            {data: 'serie', render: $.fn.dataTable.render.text()},
            {data: 'usuario', render: $.fn.dataTable.render.text()},
            {data: 'motivo', render: $.fn.dataTable.render.text()}
        ],
        language: {url: '/i18n/es-ES.json'}
    });

    $('#filtrarAuditoria').on('click', () => tabla.draw());
    $('#fAccionAuditoria').on('change', () => tabla.draw());
    $('#fNumeroAuditoria').on('keydown', function (evento) {
        if (evento.key === 'Enter') {
            evento.preventDefault();
            tabla.draw();
        }
    });
    $('#limpiarAuditoria').on('click', function () {
        $('#fNumeroAuditoria').val('');
        $('#fAccionAuditoria').val('');
        tabla.search('').draw();
    });
});
