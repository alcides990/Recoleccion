import {consultar, guardar, modificar, mostrarAlerta, limpiar, tabulador, tabular, generarPaginacion}
from '/js/modulos.js';
import {isTbodyNotEmpty}
from '/js/utils.js';

tabulador("#numeroComprobante", "#cantidadPago");
tabulador("#subTotal", "#recargoPago");
tabulador("#recargoPago", "#guardar");

function fechaPagoHastaFormateada(pagoHasta) {
    if (!pagoHasta) return '';
    const partes = pagoHasta.substring(0, 10).split('-');
    return partes.length === 3 ? partes[1] + '-' + partes[0] : pagoHasta;
}
function numeroComprobanteSiguiente(number) {
    var numeroComprobanteSiguiente = (parseInt(number) + 1).toString().padStart(7, 0);
    return numeroComprobanteSiguiente;
}

export async function cargarDatosComprobante(datos, url) {
    $("#guardar").prop('disabled', true);
    try {
        const data = await consultar(datos, url);

        $("#razonSocial").val(`${data.usuario.nombre} ${data.usuario.apellido?? ''}`);
        $("#numeroDocumento").val(data.usuario.numeroDocumento);
        $("#categoria").empty();
        $("#categoria").append('<option value="' + data.categoria.codigoCategoria + '">' + data.categoria.nombreCategoria + '-' + data.categoria.tarifa + '</option>');
        $("#cobrador").val(data.manzana.zona.cobrador.codigoCobrador);
        $("#codigoUsuario").val(data.usuario.codigoUsuario);
        $("#tarifa").val(data.categoria.tarifa);
        $("#pagoHasta").val(fechaPagoHastaFormateada(data.estadoCuenta.pagoHasta))
                .attr('data-iso', data.estadoCuenta.pagoHasta || '');
        $("#cantidadDeuda").val(data.estadoCuenta.cantidadDeuda);
        $("#subTotal").val(formatPYG(data.estadoCuenta.subTotal));
        $("#saldoAnterior").val(formatPYG(data.estadoCuenta.saldoAnterior));
        $("#recargoPago").val(data.estadoCuenta.recargo);
        $("#totalPagar").val(formatPYG(data.estadoCuenta.totalDeuda));
        if ($("#tipoComprobante option:selected").text() == 'FACTURA MANUAL') {
            tabular("#numeroComprobante");
        } else {
            tabular("#cantidadPago");
            limpiar(["#cantidadPago"]);
        }
        $("#numeroComprobante").on('keydown', (e) => {
            if (e.keyCode === 13 || e.keyCode === 9) {
                e.preventDefault();
                tabular("#cantidadPago");
                limpiar(["#cantidadPago"]);
            }
        });
    } catch (e) {
        
        mostrarAlerta({
            mensaje: e,
            url: url,
            tipo: 'success'
        });
    } finally {
        $("#guardar").prop('disabled', false);
    }
;
}

export function cargarDatosComprobanteModal(datos, accion) {
    var url = '/comprobante/getComprobante';
    consultar(datos, url)
            .then(function (data) {
                if (data.estado === 'ANULADO' && accion === 'editar') {
                    $('#motivoAnulacionContainer').removeClass('d-none');
                }
                $("#comprobanteModal").modal('show');
                $("#cuentaCorriente").val(data.cuentaCorriente);
                $("#razonSocial").val(data.nombreUsuario);
                $("#numeroDocumento").val(data.numeroDocumento);
                $("#numeroComprobante").val(data.numeroComprobante);
                $("#categoria").empty();
                $("#categoria").append(`<option value=${data.categoria.codigoCategoria}> ${data.categoria.tarifa}-${data.categoria.nombreCategoria}</option>'`);
                $("#tarifa").val(data.categoria.tarifa);
                $("#cobrador").val(data.cobrador.codigoCobrador);
                $("#sucursalModal").empty();
                $("#sucursalModal").append(`<option value=${data.codigoSucursal}>${data.sucursal}</option>`);
                $("#puntoExpedicionModal").empty();
                $("#puntoExpedicionModal").append(`<option value=${data.codigoPuntoExpedicion}> ${data.puntoExpedicion} </option>`);
                $("#serieModal").empty();
                $("#serieModal").append(`<option value=${data.serie.codigoSerie}> ${data.serie.serie} </option>`);
                $("#tipoComprobante").empty();
                $("#tipoComprobante").append(`<option value=${data.tipoComprobante.codigoTipoComprobante}>${data.tipoComprobante.nombreTipoComprobante}</option>`);
                $("#condicionVenta").empty();
                $("#condicionVenta").append(`<option value=${data.condicionVenta.codigoCondicionVenta}>${ data.condicionVenta.condicionVenta}</option>`);
                $("#estado").val(data.estado.codigoEstado);
                $("#fechaPago").val(data.fechaPago);
                $("#periodoPago").val(data.periodoPago);
                $("#cantidadPago").val(data.cantidadPago);
                $("#subTotal").val(formatPYG(data.tarifa * data.cantidadPago));
                $("#saldo").val(formatPYG(data.saldo));
                $("#recargoPago").val(formatPYG(data.recargo));
                $("#totalImporte").val(formatPYG(data.importe));
                $("#motivoAnulacion").val(data.motivoAnulacion);

                $("#detalle-pago").empty();
                let detosPago;
                $.each(data.detallePago, function (llave, valor) {
                    detosPago = `
                  <div class="row">
                                    <div class="col-4">
                                        <label for="recargoPago">${valor.metodoPago}:</label>
                                    </div>
                                    <div class="col">
                                        <input type="text" readonly="true"  value=${formatPYG(valor.importe)} 
                                               class="form-control" />
                                    </div>
                                </div>
                <div>
                    `;
                    $("#detalle-pago").append(detosPago);
                });
            });
}

var subTotal = 0;
$("#cantidadPago").on('keydown', function (e) {
    if (e.keyCode == 13 || e.keyCode == 9) {
        e.preventDefault();
        $('#recargoPago').focus();
    }
});
$("#cantidadPago").blur(function (e) {
    var cantidadPago = $('#cantidadPago').val();
    var tarifa = $('#tarifa').val();
    subTotal = cantidadPago * tarifa;
    $('#subTotal').val(formatPYG(subTotal));
    if (isTbodyNotEmpty) {
        limpiarMetodosPago("#tablaMetodoPago");
    }
});

$("#recargoPago").on('keyup', function (e) {
    let recargo = soloNumero($("#recargoPago").val());
    $("#recargoPago").val(formatPYG(recargo));
    calcularTotalImporte();
});
$("#subTotal").on('keyup', function (e) {
    calcularTotalImporte();
});

$("#recargoPago").blur(function (e) {
    let recargo = parseInt($("#recargoPago").val());
    let total = 0;
    total = recargo + subTotal;
    $("#totalDeuda").val(formatPYG(total));
});

function calcularTotalImporte() {
    let subTotal = parseInt(soloNumero($("#subTotal").val()) || 0);
    let recargo = parseInt(soloNumero($("#recargoPago").val()) || 0);
    let saldoAnterior = parseInt(soloNumero($("#saldoAnterior").val()) || 0);
    let total = 0;
    total = recargo + subTotal - saldoAnterior;
    if ($("#recargoPago").val() !== '') {
        $("#totalPagar").val(formatPYG(total));
        $("#subTotal").val(formatPYG(subTotal));
    }
}
//    -----------------GUARDAR COMPROBANTE-------------------------------
//Campos a limpira



export function limpiarComprobante() {
    let campos = [
    "#cuentaCorriente",
    "#numeroDocumento",
    "#razonSocial",
    "#categoria",
    "#cobrador",
    "#pagoHasta",
    "#cantidadDeuda",
    "#cantidadPago",
    "#subTotal",
    "#saldoAnterior",
    "#recargoPago",
    "#totalPagar"
];
    limpiar(campos);
}
function metosdosPagoAgregados() {
    return $("#tablaMetodoPago tbody tr").length;
}

function actualizarEstadoSubtotal() {
    $("#subTotal").prop('disabled', metosdosPagoAgregados() > 0);
}

actualizarEstadoSubtotal();

$(document).on('click', 'tr  td #eliminar', function (event) {
    $(this).closest('tr').remove();
    sumarImportes();
    actualizarEstadoSubtotal();
    tabular("#importe");
});

export function formatPYG(number) {
    number = number === '' || number === 'NaN' ? 0 : number;
    return parseInt(number).toLocaleString('es-PY', {style: 'currency', currency: 'PYG'});
}

export function soloNumero(value) {
    return value.replace(/[^0-9]/g, '');
}

$("#importe").on('keypress', function (e) {
    if (e.keyCode === 13) {
        agregarMetodoPago();
    }
});
export function limpiarMetodosPago() {
    $('#tablaMetodoPago tbody ').empty();
    limpiar([$("#totalImporte")]);
    $("#metodoPago").show();
    actualizarEstadoSubtotal();
    tabular("#importe");
}
export function agregarMetodoPago() {
    if ($("#importe").val().trim() !== '') {
        let importe = parseInt($("#importe").val());
        let codigoMetodoPago = $("#metodoPagoSelect").val();
        let metodoPago = $("#metodoPagoSelect").find('option:selected').text();
        $('#tablaMetodoPago tbody tr').each(function (index, element) {
            if (codigoMetodoPago == $(this).find('td:eq(0)').text()) {
                let importeTr = parseInt($(this).find('td:eq(2)').text());
                importe += importeTr;
                element.remove();
            }

        });
        let datos = `
            <tr> 
                <td>${codigoMetodoPago}</td>
                <td>${metodoPago}</td>
                <td>${importe}</td>
                <td> <a id="eliminar" class="btn btn-danger eliminar btn-sm" >
                                    <i class="fa-regular fa-trash-can"></i>
                     </a>
                </td>
            </tr>
`;
        $("#tablaMetodoPago tbody").append(datos);
        sumarImportes();
        actualizarEstadoSubtotal();
        limpiar([$("#importe")]);
        tabular("#importe");
    }
}
function sumarImportes() {
    let totalImporte = 0;
    if (metosdosPagoAgregados() > 0) {
        $('#tablaMetodoPago tbody tr').each(function (index, element) {
            let  importe = parseInt($(this).find('td:eq(2)').text());
            totalImporte += importe;
        });
    }
    $("#totalImporte").val(formatPYG(totalImporte));
}

$('#configModal').on('hidden.bs.modal', function (e) {
    if (metosdosPagoAgregados() !== 0) {
        $("#metodoPago").hide();
    } else {
        $("#metodoPago").show();
    }
    actualizarEstadoSubtotal();
});
function getDetallePago() {
    let detallePago = [];
    if (metosdosPagoAgregados() > 0) {
        $('#tablaMetodoPago tbody tr').each(function (index, element) {
            let codigoMetodoPago = parseInt($(this).find('td:eq(0)').text());
            let importe = parseInt($(this).find('td:eq(2)').text());
            let metodoPago = {
                detallePagoPK: {
                    codigoMetodoPago: codigoMetodoPago
                },
                importe: importe
            };
            detallePago.push(metodoPago);
        });
    } else {
        let codigoMetodoPago = $("#metodoPago").val();
        let importe = (soloNumero($("#totalPagar").val()));
        let metodoPago = {
            detallePagoPK: {
                codigoMetodoPago: codigoMetodoPago
            },
            importe: importe
        };
        detallePago.push(metodoPago);
    }
    return detallePago;
}



$('#frm-comprobante').submit(function (event) {
    event.preventDefault();
    let timbradoSeleccionado = $("#detalleTimbrado option:selected");
    let codigoSerie = timbradoSeleccionado.data("serie");
    let codigoTimbrado = timbradoSeleccionado.val();
    if (!codigoTimbrado || codigoSerie === undefined) {
        mostrarAlerta({mensaje: 'Seleccione un timbrado y serie vigente.', url: '/comprobante/guardar', tipo: 'danger'});
        return;
    }
    let codigoCondicionVenta = $("#condicionVenta").val();
    ;
    let fechaPago = $("#fechaPago").val();
    let pagoHasta = $("#pagoHasta").attr('data-iso') || null;
    let cuentaCorriente = $("#cuentaCorriente").val();
    let codigoUsuario = $("#codigoUsuario").val();
    let razonSocial = $("#razonSocial").val();
    let numeroComprobante = $("#numeroComprobante").val();
    let codigoPuntoExpedicion = $("#puntoExpedicion").val();
    let codigoTipoComprobante = $("#tipoComprobante").val();
    let codigoCobrador = $("#cobrador").val();
    let codigoComision = $("#comision").val();
    let cantidadDeuda = $("#cantidadDeuda").val();
    let cantidadPago = $("#cantidadPago").val();
    let codigoCategoria = $("#categoria").val();
    let tarifa = $("#categoria").text().split('-')[1];
    let recargoPago = soloNumero($("#recargoPago").val());
    let saldoAnterior = soloNumero($("#saldoAnterior").val());
    let url = '/comprobante/guardar';
    let datos = {
        codigoSerie: codigoSerie,
        codigoTimbrado: codigoTimbrado,
        cuentaCorriente: cuentaCorriente,
        codigoUsuario: codigoUsuario,
        razonSocial: razonSocial,
        numeroComprobante: numeroComprobante,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
        codigoTipoComprobante: codigoTipoComprobante,
        codigoCobrador: codigoCobrador,
        codigoComision: codigoComision,
        fechaPago: fechaPago,
        codigoCondicionVenta: codigoCondicionVenta,
        pagoHasta: pagoHasta,
        cantidadDeuda: cantidadDeuda,
        cantidadPago: cantidadPago,
        codigoCategoria: codigoCategoria,
        tarifa: tarifa,
        recargoPago: recargoPago,
        saldoAnterior: saldoAnterior,
        detallePago: getDetallePago()
    };
    let contenType = 'application/json';
    guardar(datos, url, contenType).then(function (data) {
        mostrarAlerta({
            mensaje: data,
            url: url,
            tipo: 'success'
        });
        let numComprobante = $("#numeroComprobante").val();
        limpiarComprobante();
        limpiarMetodosPago();
        $("#numeroComprobante").val(numeroComprobanteSiguiente(numComprobante));
        $("#cuentaCorriente").focus();
    });
});


function guardarComprobante(datos, url) {
    let token = $("#token").val();
    $.ajax({
        headers: {
            'X-CSRF-TOKEN': token
        },
        url: url,
        data: datos,
        type: "post",
        success: function (response) {
            mostrarAlerta({
                mensaje: response,
                tipo: 'success'
            });
            limpiar(campos);
            generarNumeroComprobante();
        },
        error: function (error) {
            var mensajeError = error.responseText;
            if (error.responseJSON && error.responseJSON.hasOwnProperty("message")) {
                mensajeError = error.responseJSON.message;
            }
            mostrarAlerta({
                mensaje: mensajeError,
                tipo: 'danger'
            });
        }
    });
}
export function modificarComprobante(url) {
    let codigoSucursal = $('#sucursalModal').val();
    let codigoPuntoExpedicion = $("#puntoExpedicionModal").val();
    let codigoTipoComprobante = $("#tipoComprobante").val();
    let codigoSerie = $('#serieModal').val();
    let numeroComprobante = $("#numeroComprobante").val();
    let fechaPago = $("#fechaPago").val();
    let codigoCobrador = $("#cobrador").val();
    let codigoEstado = $("#estado").val();
    let motivoAnulacion = $("#motivoAnulacion").val();
    let contentType = 'application/json';

    var datos = {
        comprobantePK: {
            codigoTipoComprobante: codigoTipoComprobante,
            codigoSerie: codigoSerie,
            numeroComprobante: numeroComprobante,
            puntoExpedicionPK: {
                codigoSucursal: codigoSucursal,
                codigoPuntoExpedicion: codigoPuntoExpedicion
            }
        },
        cobrador: {codigoCobrador: codigoCobrador},
        estado: {codigoEstado: codigoEstado},
        obs: motivoAnulacion,
        fechaPago: fechaPago
    };
    modificar(datos, url, contentType).then(response => {
        $('#comprobanteModal').modal('hide');
        $('#motivoAnulacionContainer').val('');
        mostrarAlerta({
            mensaje: response,
            tipo: 'warning',
            time: 3000,
            recargar: true
        });
    });
}

//------------------Filtrar tabla de comprobantes----------------
export function buscarComprobantes(numeroPagina) {
    var codigoSucursal = $("#sucursal").val();
    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoSerie = $("#serie").val();
    var numeroComprobante = $("#txtBuscar").val().replace(/[^0-9]/g, '');
    numeroComprobante = (numeroComprobante === '0000000') ? '' : numeroComprobante;
    var numeroPagina = numeroPagina;
    var cantidadRegistro = $("#cantidadRegistro").val();
    var url = '/comprobante/filtrar';
    var datos = {
        codigoSucursal: codigoSucursal, codigoPuntoExpedicion: codigoPuntoExpedicion, numeroComprobante: numeroComprobante,
        codigoSerie: codigoSerie, numeroPagina: numeroPagina, cantidadRegistro: cantidadRegistro
    };
    let contentType = 'application/x-www-form-urlencoded';
    consultar(datos, url, contentType).then((response) => {

        $("tbody").empty();
        $("#paginador").empty();
        $.each(response.data, (llave, valor) => {
            let fila = `
                     <tr> 
                        <td data-id=${valor.tipoComprobante.codigoTipoComprobante}> ${valor.tipoComprobante.nombreTipoComprobante} </td>
                        <td data-id=${valor.codigoPuntoExpedicion}> ${valor.puntoExpedicion} </td>
                        <td data-id=${valor.numeroComprobante}>  ${valor.numeroComprobante.toString().padStart(7, 0)}</td>
                        <td data-id=${valor.serie.codigoSerie} hidden=""> </td>
                        <td> ${valor.cuentaCorriente} </td>
                        <td> ${valor.nombreUsuario} </td>
                        <td> ${valor.fechaPago} </td>
                        <td> ${valor.tarifa} </td>
                        <td> ${valor.cantidadPago} </td>
                        <td> ${valor.recargo} </td>
                        <td> ${valor.importe} </td>
                        <td> ${valor.saldo} </td>
                        <td> ${valor.estado.estado} </td>
                        <td  >
                         <a title="Editar comprobante" id="editar"  
                             <i class="fa-regular fa-pen-to-square btn-primario p-1"></i>
                             </a>
                            <a title="Anular" id="anular" 
                               <i class="btn-close fa-regular fa-pen-to-square"></i>
                            </a> 
                     </tr> `;
            $("tbody").append(fila);
        });
        generarPaginacion(response.page);
        $("#paginador ul a").click(function () {
            getPageSeclected(response.page);
        });
    });
}

export function getPageSeclected(page) {
    $("#paginador ul a").click(function () {
        $("#cantidadRegistro").val(page.cantidadRegistro);
        let numeroPagina = $(this).text();
        switch (numeroPagina) {
            case 'Primera':
                buscarComprobantes(0);
                break;
            case '«':
                buscarComprobantes(page.paginaActual - 2);
                break;
            case '»':
                buscarComprobantes(page.paginaActual);
                break;
            case 'Última':
                buscarComprobantes(page.totalPaginas - 1);
                break;
            default :
                buscarComprobantes(parseInt(numeroPagina) - 1);
        }
    });
}


$("#cantidadRegistro").change(function () {
    var cantidadRegistro = $(this).val();
    var url = new URL(window.location.href);
    var searchParams = new URLSearchParams(url.search);
    searchParams.set('page', 0);
    searchParams.set('cantidadRegistro', cantidadRegistro);
    url.search = searchParams.toString();
    window.location.href = url;
}
);
export function generarNumeroComprobante() {
    var modoEmision = $("#detalleTimbrado option:selected").attr('data-modo-emision');
    if (modoEmision !== 'AUTOIMPRESOR') {
        $("#numeroComprobante").prop('readonly', false);
    } else {
        $("#numeroComprobante").prop('readonly', true);
    }

    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoTipoComprobante = $("#tipoComprobante").val();
    var codigoSerie = $("#detalleTimbrado option:selected").data("serie");
    if (codigoSerie === undefined) {
        $("#numeroComprobante").val("");
        return;
    }
    var url = '/comprobante/numComprobante';
    var datos = {
        puntoExpedicionPK: {
            codigoPuntoExpedicion: codigoPuntoExpedicion
        },
        codigoTipoComprobante: codigoTipoComprobante,
        codigoSerie: codigoSerie
    };
    consultar(datos, url).then(data => {
        $("#numeroComprobante").val(data.toString().padStart(7, 0));
    }).catch(error => {

        mostrarAlerta({
            mensaje: error,
            url: url,
            tipo: 'danger'
        });
    });


}






