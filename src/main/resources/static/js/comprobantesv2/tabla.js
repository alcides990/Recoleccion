$(function () {
    $.fn.dataTable.ext.renderer.pageButton.bootstrap = function (settings, host, index, buttons, page, pages) {
        const api = new $.fn.dataTable.Api(settings);
        const etiquetas = settings.oLanguage.oPaginate;
        const renderizar = function (contenedor, items) {
            items.forEach(function (item) {
                if (Array.isArray(item)) {
                    renderizar(contenedor, item);
                    return;
                }
                let texto;
                let estado = '';
                if (item === 'ellipsis') {
                    texto = '&hellip;';
                    estado = 'disabled';
                } else if (item === 'first') {
                    texto = etiquetas.sFirst;
                    estado = page === 0 ? 'disabled' : '';
                } else if (item === 'previous') {
                    texto = etiquetas.sPrevious;
                    estado = page === 0 ? 'disabled' : '';
                } else if (item === 'next') {
                    texto = etiquetas.sNext;
                    estado = page === pages - 1 ? 'disabled' : '';
                } else if (item === 'last') {
                    texto = etiquetas.sLast;
                    estado = page === pages - 1 ? 'disabled' : '';
                } else {
                    texto = item + 1;
                    estado = page === item ? 'active' : '';
                }
                const esEnlace = estado === '' && item !== 'ellipsis';
                const control = esEnlace
                        ? $('<a>', {href: '#', class: 'page-link', html: texto})
                        : $('<span>', {class: 'page-link', html: texto});
                $('<li>', {class: 'page-item ' + estado}).append(control).appendTo(contenedor);
                if (esEnlace) {
                    control.on('click', function (evento) {
                        evento.preventDefault();
                        api.page(item).draw('page');
                    });
                }
            });
        };
        const lista = $('<ul>', {class: 'pagination mb-0'}).appendTo($(host).empty());
        renderizar(lista, buttons);
    };

    const token = $('#token').val();
    const roles = String($('#roles').text() || '');
    const puedeEditar = roles.includes('ROOT') || roles.includes('ADMINISTRADOR') || roles.includes('SUPERVISOR');
    let accionActual = null;
    let cargandoEdicion = false;
    let comprobantePendienteImpresion = null;
    const claveFormatoImpresion = 'formatoComprobanteV2';
    const formatosImpresion = ['58mm', '80mm', 'a4'];

    const escapar = function (valor) {
        return $('<div>').text(valor ?? '').html();
    };
    const formatearImporte = function (valor) {
        return Number(valor || 0).toLocaleString('es-PY');
    };
    const calcularSubtotal = function (cantidadPago, tarifa) {
        return Number(cantidadPago || 0) * Number(tarifa || 0);
    };
    const actualizarSubtotalEdicion = function () {
        $('#editarSubtotalV2').val(calcularSubtotal(
                $('#editarCantidadPagoV2').val(), $('#editarTarifaV2').val()));
    };
    const formatearFecha = function (valor) {
        if (!valor) return '';
        const partes = String(valor).split('-');
        return partes.length === 3 ? partes[2] + '/' + partes[1] + '/' + partes[0] : valor;
    };
    const identificador = function (detalle) {
        return detalle.tipoComprobante + ' · ' + detalle.puntoExpedicion + ' · Serie '
                + detalle.serie + ' · N.º ' + String(detalle.numeroComprobante).padStart(7, '0');
    };
    const mostrarMensaje = function (tipo, mensaje) {
        const contenedor = $('#mensajeComprobantesV2').empty().appendTo(document.body);
        $('<div>', {class: 'alert alert-' + tipo + ' alert-dismissible fade show mb-0', role: 'alert'})
                .text(mensaje)
                .append($('<button>', {type: 'button', class: 'btn-close', 'data-bs-dismiss': 'alert', 'aria-label': 'Cerrar'}))
                .appendTo(contenedor);
    };
    const abrirModal = function (id) {
        window.bootstrap.Modal.getOrCreateInstance(document.getElementById(id)).show();
    };
    const cerrarModal = function (id) {
        window.bootstrap.Modal.getOrCreateInstance(document.getElementById(id)).hide();
    };
    const actualizarTotalPagos = function () {
        let total = 0;
        let cantidad = 0;
        $('#tablaPagosEdicionV2 tbody tr').each(function () {
            total += Number($(this).attr('data-importe') || 0);
            cantidad++;
        });
        $('#totalPagosEdicionV2').val(formatearImporte(total));
        $('#resumenPagosEdicionV2').text(cantidad
                ? cantidad + (cantidad === 1 ? ' medio' : ' medios') + ' · ₲ ' + formatearImporte(total)
                 : 'Sin detalle cargado');
    };
    const redistribuirDetallePagos = function (nuevoTotal) {
        const filas = $('#tablaPagosEdicionV2 tbody tr');
        if (!filas.length) return;
        let totalAnterior = 0;
        filas.each(function () {
            totalAnterior += Number($(this).attr('data-importe') || 0);
        });
        let acumulado = 0;
        filas.each(function (indice) {
            let importe;
            if (indice === filas.length - 1) {
                importe = Math.max(0, Math.round((nuevoTotal - acumulado) * 100) / 100);
            } else {
                const proporcion = totalAnterior > 0
                        ? Number($(this).attr('data-importe') || 0) / totalAnterior
                        : 1 / filas.length;
                importe = Math.min(Math.max(0, nuevoTotal - acumulado),
                        Math.round(nuevoTotal * proporcion * 100) / 100);
                acumulado += importe;
            }
            $(this).attr('data-importe', importe).children('td').eq(2).text(formatearImporte(importe));
        });
        actualizarTotalPagos();
    };
    const recalcularImporteEdicion = function () {
        actualizarSubtotalEdicion();
        if (cargandoEdicion) return;
        const subtotal = Number($('#editarSubtotalV2').val() || 0);
        const recargo = Number($('#editarRecargoV2').val() || 0);
        const saldoFavor = Number($('#editarSaldoV2').val() || 0);
        const total = Math.round((subtotal + recargo + saldoFavor) * 100) / 100;
        $('#editarImporteV2').val(total);
        redistribuirDetallePagos(total);
    };
    const agregarPagoTabla = function (pago) {
        const codigo = Number(pago.codigoMetodoPago);
        const importe = Number(pago.importe || 0);
        if (!codigo || importe <= 0) return;
        const existente = $('#tablaPagosEdicionV2 tbody tr[data-codigo="' + codigo + '"]');
        if (existente.length) {
            pago.importe = Number(existente.attr('data-importe') || 0) + importe;
            existente.remove();
        }
        const nombre = pago.metodoPago || $('#editarMetodoPagoSelectV2 option[value="' + codigo + '"]').text();
        $('<tr>', {'data-codigo': codigo, 'data-importe': pago.importe})
                .append($('<td>').text(codigo))
                .append($('<td>').text(nombre))
                .append($('<td>').text(formatearImporte(pago.importe)))
                .append($('<td>').append($('<button>', {
                    type: 'button', class: 'btn btn-danger btn-sm quitar-pago-v2', title: 'Eliminar medio de pago'
                }).html('<i class="fa-regular fa-trash-can"></i>')))
                .appendTo('#tablaPagosEdicionV2 tbody');
        actualizarTotalPagos();
    };
    const solicitar = async function (url, metodo, datos) {
        const respuesta = await fetch(url, {
            method: metodo,
            headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token},
            body: JSON.stringify(datos)
        });
        const contenido = await respuesta.text();
        let cuerpo = null;
        try {
            cuerpo = contenido ? JSON.parse(contenido) : null;
        } catch (error) {
            cuerpo = contenido;
        }
        if (!respuesta.ok) {
            throw new Error(cuerpo?.mensaje || cuerpo?.message || cuerpo || 'No fue posible completar la operación.');
        }
        return cuerpo;
    };
    const consultarCuentaEdicion = async function (cuentaCorriente) {
        const respuesta = await fetch('/comprobantes-v2/cuenta-edicion?cuentaCorriente='
                + encodeURIComponent(cuentaCorriente), {
            method: 'GET',
            headers: {'Accept': 'application/json', 'X-CSRF-TOKEN': token}
        });
        const contenido = await respuesta.text();
        let cuerpo = null;
        try {
            cuerpo = contenido ? JSON.parse(contenido) : null;
        } catch (error) {
            cuerpo = contenido;
        }
        if (!respuesta.ok) {
            throw new Error(cuerpo?.mensaje || cuerpo?.message || cuerpo || 'No fue posible recuperar la cuenta corriente.');
        }
        return cuerpo;
    };
    const clavesBoton = function (boton) {
        return {
            codigoSucursal: Number(boton.attr('data-sucursal')),
            codigoPuntoExpedicion: Number(boton.attr('data-punto')),
            codigoTipoComprobante: Number(boton.attr('data-tipo')),
            codigoSerie: Number(boton.attr('data-serie')),
            numeroComprobante: Number(boton.attr('data-numero'))
        };
    };
    const urlTicket = function (claves, formato) {
        return '/comprobantes-v2/ticket?' + new URLSearchParams({...claves, formato: formato}).toString();
    };
    const formatoRecordado = function () {
        try {
            const formato = localStorage.getItem(claveFormatoImpresion);
            return formatosImpresion.includes(formato) ? formato : '58mm';
        } catch (error) {
            return '58mm';
        }
    };
    const abrirSelectorImpresion = function (claves) {
        comprobantePendienteImpresion = claves;
        const formato = formatoRecordado();
        $('#formatoComprobanteV2').val(formato);
        $('#recordarFormatoComprobanteV2').prop('checked', true);
        abrirModal('formatoImpresionComprobanteV2');
    };
    const imprimirComprobante = function (claves, formato) {
        const destino = urlTicket(claves, formato);
        if (window.AndroidPrinter && typeof window.AndroidPrinter.imprimirPdf === 'function'
                && window.AndroidPrinter.disponible()) {
            window.AndroidPrinter.imprimirPdf(new URL(destino, window.location.origin).href);
        } else {
            window.open(destino, '_blank', 'noopener');
        }
    };
    const pintarDetalle = function (detalle) {
        const fila = function (etiqueta, valor) {
            return '<div class="col-md-6"><div class="detalle-v2-campo"><div class="text-muted small">' + escapar(etiqueta)
                    + '</div><div>' + escapar(valor) + '</div></div></div>';
        };
        const pagos = detalle.pagos && detalle.pagos.length
                ? '<ul class="list-group list-group-flush">' + detalle.pagos.map(function (pago) {
                    return '<li class="list-group-item d-flex justify-content-between px-0"><span>'
                            + escapar(pago.metodoPago) + '</span><strong>₲ ' + formatearImporte(pago.importe) + '</strong></li>';
                }).join('') + '</ul>'
                : '<span class="text-muted">Sin detalle de pagos.</span>';
        $('#contenidoDetalleComprobanteV2').html('<div class="row g-3">'
                + fila('Comprobante', identificador(detalle))
                + fila('Estado', detalle.estado)
                + fila('Cuenta corriente', detalle.cuentaCorriente)
                + fila('Receptor', detalle.receptor)
                + fila('Documento', detalle.documento)
                + fila('Fecha de emisión', detalle.fechaEmision ? formatearFecha(String(detalle.fechaEmision).substring(0, 10)) : '')
                + fila('Fecha de pago', formatearFecha(detalle.fechaPago))
                + fila('Pago desde', formatearFecha(detalle.pagoDesde))
                + fila('Período de pago', detalle.periodoPago)
                + fila('Cantidad de deuda', detalle.cantidadDeuda)
                + fila('Cantidad de pagos', detalle.cantidadPago)
                + fila('Tarifa', '₲ ' + formatearImporte(detalle.tarifa))
                + fila('Subtotal', '₲ ' + formatearImporte(calcularSubtotal(detalle.cantidadPago, detalle.tarifa)))
                + fila('Recargo', '₲ ' + formatearImporte(detalle.recargo))
                + fila('Importe total', '₲ ' + formatearImporte(detalle.importe))
                + fila('Saldo a favor', '₲ ' + formatearImporte(detalle.saldo))
                + fila('Categoría', detalle.categoria)
                + fila('Condición de venta', detalle.condicionVenta)
                + '<div class="col-12"><div class="detalle-v2-campo"><div class="text-muted small">Observación</div><div>'
                + escapar(detalle.observacion || 'Sin observación') + '</div></div></div>'
                + '<div class="col-12"><h6 class="mt-2">Métodos de pago</h6>' + pagos + '</div></div>');
    };

    const pintarDetalleLegado = function (detalle) {
        const campo = function (etiqueta, valor, columna) {
            return '<div class="' + (columna || 'col-md-3') + '"><label>' + escapar(etiqueta)
                    + '</label><input class="form-control" readonly type="text" value="' + escapar(valor) + '"></div>';
        };
        const filaValor = function (etiqueta, valor) {
            return '<div class="row"><div class="col-4"><label>' + escapar(etiqueta)
                    + ':</label></div><div class="col"><input type="text" readonly value="'
                    + escapar(valor) + '" class="form-control"></div></div>';
        };
        const filaImporte = function (etiqueta, valor) {
            return filaValor(etiqueta, 'Gs. ' + formatearImporte(valor));
        };
        const metodosPago = detalle.pagos && detalle.pagos.length
                ? detalle.pagos.map(function (pago) { return filaImporte(pago.metodoPago, pago.importe); }).join('')
                : '<div class="row"><div class="col"><input type="text" readonly value="Sin detalle de pagos" class="form-control"></div></div>';
        $('#contenidoDetalleComprobanteV2').html(
                campo('Cuenta corriente', detalle.cuentaCorriente, 'col-sm-3')
                + campo('RUC/CI', detalle.documento, 'col-sm-3')
                + campo('Razon social', detalle.receptor, 'col-md-6')
                + campo('Punto expedicion', detalle.puntoExpedicion, 'col-md-3')
                + campo('Serie', detalle.serie, 'col-md-2')
                + campo('Numero comprobante', String(detalle.numeroComprobante).padStart(7, '0'), 'col-md-3')
                + campo('Tipo comprobante', detalle.tipoComprobante, 'col-md-4')
                + campo('Estado', detalle.estado, 'col-md-3')
                + campo('Fecha pago', formatearFecha(detalle.fechaPago), 'col-md-3')
                + '<div class="row m-3"><div class="col-lg-6"><h5>Detalle Pago</h5>'
                + filaValor('Periodo pago', detalle.periodoPago)
                + filaValor('Cantidad pago', detalle.cantidadPago)
                + filaImporte('Tarifa', detalle.tarifa)
                + filaImporte('Subtotal', calcularSubtotal(detalle.cantidadPago, detalle.tarifa))
                + filaImporte('Saldo a favor', detalle.saldo)
                + filaImporte('Recargo', detalle.recargo)
                + filaImporte('Total importe', detalle.importe)
                + '<div><h5>Metodo Pago</h5>' + metodosPago + '</div></div>'
                + '<div class="form-floating col-lg-6"><textarea class="form-control" readonly placeholder="Observacion">'
                + escapar(detalle.observacion || 'Sin observacion') + '</textarea><label>Observacion</label></div></div>');
    };

    const tabla = $('#tablaComprobantesV2').DataTable({
        renderer: 'bootstrap',
        processing: true,
        serverSide: true,
        searching: true,
        scrollX: true,
        scrollCollapse: true,
        autoWidth: false,
        pageLength: 25,
        lengthMenu: [[10, 25, 50, 100], [10, 25, 50, 100]],
        pagingType: 'full_numbers',
        dom: "<'row g-3 align-items-center mb-3'<'col-md-6'l><'col-md-6'f>>"
                + "<'row'<'col-12'tr>>"
                + "<'row g-3 align-items-center mt-3'<'col-md-5'i><'col-md-7 d-flex justify-content-md-end'p>>",
        order: [[5, 'desc']],
        ajax: {
            url: '/comprobantes-v2/tabla',
            type: 'POST',
            headers: {'X-CSRF-TOKEN': token},
            data: function (datos) {
                datos._csrf = token;
                datos.numero = $('#fNumero').val();
                datos.cuenta = $('#fCuenta').val();
                datos.documento = $('#fDocumento').val();
                datos.nombre = $('#fNombre').val();
                datos.desde = $('#fDesde').val();
                datos.hasta = $('#fHasta').val();
            }
        },
        columns: [
            {data: 'tipoComprobante'},
            {data: 'puntoExpedicion'},
            {data: 'numeroComprobante', render: function (numero) { return String(numero).padStart(7, '0'); }},
            {data: 'cuentaCorriente'},
            {data: 'receptor'},
            {data: 'fechaPago', render: formatearFecha},
            {data: 'importe', render: formatearImporte},
            {data: 'estado'},
            {
                data: null,
                orderable: false,
                searchable: false,
                className: 'text-center text-nowrap',
                render: function (_, __, fila) {
                    const datos = 'data-sucursal="' + fila.codigoSucursal + '" data-punto="' + fila.codigoPuntoExpedicion
                            + '" data-tipo="' + fila.codigoTipoComprobante + '" data-serie="' + fila.codigoSerie
                            + '" data-numero="' + fila.numeroComprobante + '"';
                    const anulado = String(fila.estado || '').toUpperCase() === 'ANULADO';
                    const deshabilitado = anulado ? ' disabled' : '';
                    const editar = puedeEditar ? '<button type="button" class="btn-primario p-1 accion-v2" data-accion="editar" ' + datos + deshabilitado
                            + ' title="Editar comprobante" aria-label="Editar comprobante"><i class="fa-regular fa-pen-to-square"></i></button> ' : '';
                    return '<button type="button" class="btn-primario p-1 accion-v2" data-accion="imprimir" ' + datos
                            + ' title="Imprimir comprobante" aria-label="Imprimir comprobante"><i class="fa-solid fa-print"></i></button> '
                            + editar
                            + '<button type="button" class="btn-primario p-1 accion-v2" data-accion="anular" ' + datos + deshabilitado
                            + ' title="' + (anulado ? 'Comprobante anulado' : 'Anular comprobante') + '" aria-label="Anular comprobante"><i class="fa-solid fa-ban"></i></button>';
                }
            }
        ],
        language: {url: '/i18n/es-ES.json'}
    });

    $('#tablaComprobantesV2').on('click', '.accion-v2', async function () {
        const boton = $(this);
        const accion = boton.attr('data-accion');
        if (accion === 'imprimir') {
            abrirSelectorImpresion(clavesBoton(boton));
            return;
        }
        boton.prop('disabled', true);
        try {
            const detalle = await solicitar('/comprobantes-v2/detalle', 'POST', clavesBoton(boton));
            accionActual = clavesBoton(boton);
            if (accion === 'ver') {
                pintarDetalle(detalle);
                abrirModal('detalleComprobanteV2');
            } else if (accion === 'editar') {
                cargandoEdicion = true;
                $('#editarIdentificadorV2').text(identificador(detalle));
                const esFacturaManual = String(detalle.tipoComprobante || '').trim().replace(/\s+/g, ' ').toUpperCase() === 'FACTURA MANUAL';
                $('#editarNumeroComprobanteV2').val(detalle.numeroComprobante)
                        .prop('readonly', !esFacturaManual);
                $('#ayudaNumeroComprobanteV2').text(esFacturaManual
                        ? 'Puede cambiar el número antes de guardar.'
                        : 'El número no se puede cambiar para este tipo de comprobante.');
                $('#editarCuentaV2').val(detalle.cuentaCorriente || '')
                        .prop('readonly', !esFacturaManual);
                $('#ayudaCuentaV2').text(esFacturaManual
                        ? 'Ingrese la cuenta y salga del campo para recuperar sus datos.'
                        : 'La cuenta no se puede cambiar para este tipo de comprobante.');
                $('#editarDocumentoV2').val(detalle.documento || '');
                $('#editarRazonSocialV2').val(detalle.receptor || '');
                $('#editarFechaPagoV2').val(detalle.fechaPago || '');
                $('#editarPagoDesdeV2').val(detalle.pagoDesde || '');
                $('#editarPeriodoV2').val(detalle.periodoPago || '');
                $('#editarCantidadDeudaV2').val(detalle.cantidadDeuda ?? 0);
                $('#editarCantidadPagoV2').val(detalle.cantidadPago ?? 0);
                $('#editarTarifaV2').val(detalle.tarifa ?? 0);
                actualizarSubtotalEdicion();
                $('#editarRecargoV2').val(detalle.recargo ?? 0);
                $('#editarImporteV2').val(detalle.importe ?? 0);
                $('#editarSaldoV2').val(detalle.saldo ?? 0);
                $('#editarEstadoV2').val(String(detalle.codigoEstado || ''));
                $('#editarCobradorV2').val(String(detalle.codigoCobrador || ''));
                $('#editarCategoriaV2').val(String(detalle.codigoCategoria || ''));
                $('#editarCategoriaV2').trigger('change');
                $('#editarCondicionVentaV2').val(String(detalle.codigoCondicionVenta || ''));
                $('#tablaPagosEdicionV2 tbody').empty();
                (detalle.pagos || []).forEach(agregarPagoTabla);
                $('#editarImportePagoV2').val('');
                actualizarTotalPagos();
                cargandoEdicion = false;
                $('#configModal').addClass('d-none');
                $('#mostrarEdicionPagosV2').html('<i class="fa-regular fa-pen-to-square"></i> Editar detalle de pago');
                $('#editarObservacionV2').val(detalle.observacion || '');
                abrirModal('editarComprobanteV2');
            } else if (accion === 'anular') {
                $('#anularIdentificadorV2').text(identificador(detalle));
                $('#motivoAnulacionV2').val('');
                abrirModal('anularComprobanteV2');
            }
        } catch (error) {
            mostrarMensaje('danger', error.message);
        } finally {
            cargandoEdicion = false;
            boton.prop('disabled', false);
        }
    });

    $('#confirmarFormatoImpresionV2').on('click', function () {
        if (!comprobantePendienteImpresion) return;
        const formato = $('#formatoComprobanteV2').val() || '58mm';
        try {
            if ($('#recordarFormatoComprobanteV2').prop('checked')) {
                localStorage.setItem(claveFormatoImpresion, formato);
            } else {
                localStorage.removeItem(claveFormatoImpresion);
            }
        } catch (error) {
            // La impresión funciona aunque el navegador no permita almacenamiento local.
        }
        const claves = comprobantePendienteImpresion;
        comprobantePendienteImpresion = null;
        cerrarModal('formatoImpresionComprobanteV2');
        imprimirComprobante(claves, formato);
    });

    $('#formEditarComprobanteV2').on('submit', async function (evento) {
        evento.preventDefault();
        if (!accionActual) return;
        const boton = $('#guardarEdicionComprobanteV2').prop('disabled', true);
        try {
            const pagos = $('#tablaPagosEdicionV2 tbody tr').map(function () {
                return {
                    codigoMetodoPago: Number($(this).attr('data-codigo')),
                    importe: Number($(this).attr('data-importe'))
                };
            }).get();
            const totalPagos = pagos.reduce(function (total, pago) { return total + pago.importe; }, 0);
            const importeTotal = Number($('#editarImporteV2').val());
            if (Math.abs(totalPagos - importeTotal) > 0.001) {
                throw new Error('La suma del detalle de pago debe coincidir con el importe total.');
            }
            const respuesta = await solicitar('/comprobantes-v2/editar', 'PUT', {
                ...accionActual,
                nuevoNumeroComprobante: Number($('#editarNumeroComprobanteV2').val()),
                cuentaCorriente: $('#editarCuentaV2').val().trim(),
                razonSocial: $('#editarRazonSocialV2').val(),
                fechaPago: $('#editarFechaPagoV2').val(),
                pagoDesde: $('#editarPagoDesdeV2').val() || null,
                periodoPago: $('#editarPeriodoV2').val(),
                cantidadDeuda: Number($('#editarCantidadDeudaV2').val() || 0),
                cantidadPago: Number($('#editarCantidadPagoV2').val()),
                tarifa: Number($('#editarTarifaV2').val()),
                recargo: Number($('#editarRecargoV2').val()),
                totalImporte: importeTotal,
                saldo: Number($('#editarSaldoV2').val()),
                codigoEstado: Number($('#editarEstadoV2').val()),
                codigoCobrador: Number($('#editarCobradorV2').val()),
                codigoCategoria: Number($('#editarCategoriaV2').val()),
                codigoCondicionVenta: Number($('#editarCondicionVentaV2').val()),
                pagos: pagos,
                observacion: $('#editarObservacionV2').val()
            });
            cerrarModal('editarComprobanteV2');
            mostrarMensaje('success', respuesta.mensaje);
            tabla.ajax.reload(null, false);
        } catch (error) {
            mostrarMensaje('danger', error.message);
        } finally {
            boton.prop('disabled', false);
        }
    });

    $('#agregarPagoComprobanteV2').on('click', function () {
        const importe = Number($('#editarImportePagoV2').val());
        if (!importe || importe <= 0) {
            mostrarMensaje('warning', 'Ingrese un importe mayor a cero.');
            return;
        }
        agregarPagoTabla({
            codigoMetodoPago: Number($('#editarMetodoPagoSelectV2').val()),
            metodoPago: $('#editarMetodoPagoSelectV2 option:selected').text(),
            importe: importe
        });
        $('#editarImportePagoV2').val('').trigger('focus');
    });
    $('#mostrarEdicionPagosV2').on('click', function () {
        const panel = $('#configModal');
        const mostrar = panel.hasClass('d-none');
        panel.toggleClass('d-none', !mostrar);
        $(this).html(mostrar
                ? '<i class="fa-solid fa-chevron-up"></i> Ocultar detalle de pago'
                : '<i class="fa-regular fa-pen-to-square"></i> Editar detalle de pago');
        if (mostrar) $('#editarMetodoPagoSelectV2').trigger('focus');
    });
    $('#editarCategoriaV2').on('change', function () {
        const tarifa = Number($(this).find('option:selected').attr('data-tarifa') || 0);
        $('#editarTarifaV2').val(tarifa);
        recalcularImporteEdicion();
    });
    $('#editarCuentaV2').on('change', async function () {
        const campo = $(this);
        if (campo.prop('readonly')) return;
        const cuenta = campo.val().trim();
        if (!cuenta) {
            mostrarMensaje('warning', 'Ingrese una cuenta corriente.');
            return;
        }
        campo.prop('disabled', true);
        try {
            const detalle = await consultarCuentaEdicion(cuenta);
            campo.val(detalle.cuentaCorriente || cuenta);
            $('#editarDocumentoV2').val(detalle.documento || '');
            $('#editarRazonSocialV2').val(detalle.razonSocial || '');
            cargandoEdicion = true;
            $('#editarCategoriaV2').val(String(detalle.codigoCategoria || '')).trigger('change');
            $('#editarTarifaV2').val(detalle.tarifa ?? 0);
            cargandoEdicion = false;
            recalcularImporteEdicion();
        } catch (error) {
            mostrarMensaje('danger', error.message);
            campo.trigger('focus');
        } finally {
            cargandoEdicion = false;
            campo.prop('disabled', false);
        }
    });
    $('#editarCantidadPagoV2,#editarRecargoV2').on('input', recalcularImporteEdicion);
    $('#editarImportePagoV2').on('keypress', function (evento) {
        if (evento.key === 'Enter') {
            evento.preventDefault();
            $('#agregarPagoComprobanteV2').trigger('click');
        }
    });
    $('#tablaPagosEdicionV2').on('click', '.quitar-pago-v2', function () {
        $(this).closest('tr').remove();
        actualizarTotalPagos();
    });

    $('#formAnularComprobanteV2').on('submit', async function (evento) {
        evento.preventDefault();
        if (!accionActual) return;
        const boton = $('#confirmarAnulacionV2').prop('disabled', true);
        try {
            const respuesta = await solicitar('/comprobantes-v2/anular', 'PUT', {
                ...accionActual,
                observacion: $('#motivoAnulacionV2').val()
            });
            cerrarModal('anularComprobanteV2');
            mostrarMensaje('success', respuesta.mensaje);
            tabla.ajax.reload(null, false);
        } catch (error) {
            mostrarMensaje('danger', error.message);
        } finally {
            boton.prop('disabled', false);
        }
    });

    let espera;
    $('#fNumero,#fCuenta,#fDocumento,#fNombre').on('input', function () {
        clearTimeout(espera);
        espera = setTimeout(function () { tabla.ajax.reload(); }, 300);
    });
    $('#fDesde,#fHasta').on('change', function () { tabla.ajax.reload(); });
});
