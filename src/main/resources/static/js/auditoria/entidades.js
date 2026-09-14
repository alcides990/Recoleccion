$(function () {
    const parametrosUrl = new URLSearchParams(window.location.search);
    $('#fEntidadAuditoria').val(String(parametrosUrl.get('entidad') || '').toUpperCase());
    $('#fIdentificadorAuditoria').val(parametrosUrl.get('identificador') || '');
    const etiquetas = {
        codigo: 'Código', documento: 'Documento', nombre: 'Nombre', apellido: 'Apellido',
        celular: 'Celular', correo: 'Correo', barrio: 'Barrio', direccion: 'Dirección',
        observacion: 'Observación', tipoDocumento: 'Tipo de documento', estado: 'Estado',
        sucursal: 'Sucursal', cuentaCorriente: 'Cuenta corriente', fechaInicio: 'Fecha de inicio',
        ocupado: 'Ocupado', categoria: 'Categoría', usuario: 'Usuario', manzana: 'Manzana',
        tarifa: 'Tarifa', roles: 'Roles'
    };

    const analizarJson = valor => {
        if (valor === null || valor === undefined || valor === '') return {};
        if (typeof valor === 'object') return valor;
        try {
            const resultado = JSON.parse(valor);
            return resultado && typeof resultado === 'object' ? resultado : {valor: resultado};
        } catch (_) {
            return {valor: String(valor)};
        }
    };

    const normalizar = valor => {
        if (valor === undefined) return null;
        return valor && typeof valor === 'object' ? JSON.stringify(valor) : valor;
    };
    const sonIguales = (anterior, nuevo) => normalizar(anterior) === normalizar(nuevo);

    const formatearFechaCampo = (valor, campo) => {
        if (!/^fecha/i.test(campo) || !valor) return null;
        const texto = String(valor);
        const soloFecha = texto.match(/^(\d{4})-(\d{2})-(\d{2})$/);
        if (soloFecha) return soloFecha[3] + '/' + soloFecha[2] + '/' + soloFecha[1];
        const fecha = new Date(texto);
        return Number.isNaN(fecha.getTime()) ? texto : fecha.toLocaleString('es-PY', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
        });
    };

    const presentarValor = (valor, campo) => {
        if (valor === null || valor === undefined || valor === '') {
            return '<span class="text-muted fst-italic">Sin valor</span>';
        }
        if (typeof valor === 'boolean') return valor ? 'Sí' : 'No';
        const fecha = formatearFechaCampo(valor, campo);
        const texto = fecha !== null ? fecha
                : (typeof valor === 'object' ? JSON.stringify(valor, null, 2) : String(valor));
        return $('<div>').text(texto).html().replace(/\n/g, '<br>');
    };

    const etiquetaCampo = campo => etiquetas[campo]
            || campo.replace(/([A-Z])/g, ' $1').replace(/^./, letra => letra.toUpperCase());

    const fechaLegible = valor => {
        if (!valor) return '—';
        const fecha = new Date(valor);
        return Number.isNaN(fecha.getTime()) ? String(valor) : fecha.toLocaleString('es-PY', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
        });
    };

    const claseAccion = accion => ({ALTA: 'text-bg-success', MODIFICACION: 'text-bg-warning', BAJA: 'text-bg-danger'}[accion]
            || 'text-bg-secondary');

    const mostrarDetalle = fila => {
        const anterior = analizarJson(fila.antes);
        const nuevo = analizarJson(fila.despues);
        const campos = [...new Set([...Object.keys(anterior), ...Object.keys(nuevo)])];
        let modificados = 0;
        const filas = campos.map(campo => {
            const cambio = !sonIguales(anterior[campo], nuevo[campo]);
            if (cambio) modificados++;
            return '<tr class="' + (cambio ? 'auditoria-campo-modificado' : '') + '">' +
                    '<td><strong>' + $('<div>').text(etiquetaCampo(campo)).html() + '</strong>' +
                    (cambio ? '<span class="badge text-bg-warning ms-2">Cambió</span>' : '') + '</td>' +
                    '<td><div class="auditoria-valor ' + (cambio ? 'auditoria-valor-anterior' : '') + '">' + presentarValor(anterior[campo], campo) + '</div></td>' +
                    '<td><div class="auditoria-valor ' + (cambio ? 'auditoria-valor-nuevo' : '') + '">' + presentarValor(nuevo[campo], campo) + '</div></td></tr>';
        }).join('');

        $('#detalleAuditoriaCampos').html(filas || '<tr><td colspan="3" class="text-center text-muted py-4">No hay valores registrados.</td></tr>');
        $('#detalleAuditoriaEntidadSubtitulo').text('Registro: ' + (fila.identificador || '—'));
        $('#detalleAuditoriaFecha').text(fechaLegible(fila.fecha));
        $('#detalleAuditoriaUsuario').text(fila.usuario || '—');
        $('#detalleAuditoriaModulo').text(fila.entidad || '—');
        $('#detalleAuditoriaAccion').html('<span class="badge ' + claseAccion(fila.accion) + '">' +
                $('<div>').text(fila.accion || '—').html() + '</span>');
        $('#detalleAuditoriaResumen').text(modificados + (modificados === 1 ? ' campo modificado' : ' campos modificados'));
        if (fila.motivo) {
            $('#detalleAuditoriaMotivo').text(fila.motivo);
            $('#detalleAuditoriaMotivoContenedor').removeClass('d-none');
        } else {
            $('#detalleAuditoriaMotivo').text('');
            $('#detalleAuditoriaMotivoContenedor').addClass('d-none');
        }
        bootstrap.Modal.getOrCreateInstance(document.getElementById('detalleAuditoriaEntidadModal')).show();
    };

    const tabla = $('#tablaAuditoriaEntidades').DataTable({
        processing: true, serverSide: true, searchDelay: 300, pageLength: 10, order: [[0, 'desc']],
        ajax: {url: '/auditoria-entidades/tabla', type: 'POST', headers: {'X-CSRF-TOKEN': $('#token').val()},
            data: d => { d._csrf = $('#token').val(); d.entidad = $('#fEntidadAuditoria').val(); d.accion = $('#fAccionEntidadAuditoria').val(); d.identificador = $('#fIdentificadorAuditoria').val(); }},
        columns: [{data:'fecha', render:function(valor, tipo){return tipo === 'display' || tipo === 'filter' ? fechaLegible(valor) : valor;}},{data:'entidad', render:function(valor, tipo){return tipo === 'display' && valor === 'USUARIO_SISTEMA' ? 'USUARIO DEL SISTEMA' : valor;}},{data:'accion'},{data:'identificador'},{data:'usuario'},
            {data:null,orderable:false,searchable:false,render:function(_,tipo,fila){if(tipo!=='display')return '';
                return '<button type="button" class="btn btn-sm btn-outline-primary ver-cambios-auditoria" data-codigo="' +
                        fila.codigo + '"><i class="fa-solid fa-code-compare me-1"></i>Comparar</button>';}}],
        language:{url:'/i18n/es-ES.json'}
    });
    $('#tablaAuditoriaEntidades tbody').on('click', '.ver-cambios-auditoria', function () {
        const codigo = String($(this).data('codigo'));
        const fila = tabla.rows().data().toArray().find(item => String(item.codigo) === codigo);
        if (fila) mostrarDetalle(fila);
    });
    $('#filtrarAuditoriaEntidades').on('click',()=>tabla.ajax.reload());
    $('#fIdentificadorAuditoria').on('keydown', function (evento) {
        if (evento.key === 'Enter') { evento.preventDefault(); tabla.ajax.reload(); }
    });
    $('#limpiarAuditoriaEntidades').on('click',()=>{$('#fEntidadAuditoria,#fAccionEntidadAuditoria,#fIdentificadorAuditoria').val('');tabla.search('').ajax.reload();});
});
