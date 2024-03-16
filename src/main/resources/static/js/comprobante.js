
function fechaPagoHastaFormateada(pagoHasta) {
    var fecha = new Date(pagoHasta + 'T00:00:00');
    var anio = fecha.getFullYear();
    var mes = fecha.getMonth() + 1;
    var dia = fecha.getDate();
    var fechaFormateada = anio + '-' + mes.toString().padStart(2, '0') + '-' + dia.toString().padStart(2, '0');
    return fechaFormateada;
}
function nextNumber(number) {
    var numeroComprobanteSiguiente = (parseInt(number) + 1).toString().padStart(7, 0);
    return numeroComprobanteSiguiente;

}

function cargarDatosComprobante(datos, url) {
    consultar(datos, url)
        .then(function (data) {
            $("#razonSocial").val(data.usuario.nombre + ' ' + data.usuario.apellido);
            $("#numeroDocumento").val(data.usuario.numeroDocumento);
            $("#categoria").empty();
            $("#categoria").append('<option value="' + data.categoria.codigoCategoria + '">' + data.categoria.nombreCategoria + '-' + data.categoria.tarifa + '</option>');
            $("#cobrador").empty();
            $("#cobrador").append('<option value="' + data.manzana.zona.cobrador.codigoCobrador + '">' + data.manzana.zona.cobrador.nombre + ' ' + data.manzana.zona.cobrador.apellido + '</option>');
            $("#tarifa").val(data.categoria.tarifa);

            $("#pagoHasta").val(fechaPagoHastaFormateada(data.estadoCuenta.pagoHasta));
            $("#cantidadDeuda").val(data.estadoCuenta.cantidadDeuda);
            $("#subTotal").val(formatPYG(data.estadoCuenta.subTotal));
            $("#saldoAnterior").val(formatPYG(data.estadoCuenta.saldoAnterior));
            $("#recargoPago").val(data.estadoCuenta.recargo);
            $("#totalPagar").val(formatPYG(data.estadoCuenta.totalDeuda));
            if ($("#tipoFactura option:selected").text() == 'MANUAL') {
                $("#numeroComprobante").focus();
            } else {
                $("#cantidadPago").focus();
                $("#cantidadPago").val("");
            }
            $("#numeroComprobante").on('keydown', function (e) {
                if (e.keyCode == 13 || e.keyCode == 9) {
                    e.preventDefault();
                    $("#cantidadPago").focus();
                    $("#cantidadPago").val("");
                }
            });
        });
}
function getComprobanteAnular(datos) {
    var url = '/comprobante/getComprobante';

    consultar(datos, url)
        .then(function (data) {
            $("#anularModal").modal('show');
            $("#cuentaCorriente").val(data.cuentaCorriente);
            $("#razonSocial").val(data.nombreUsuario);
            $("#numeroDocumento").val(data.numeroDocumento);
            $("#numeroComprobante").val(data.numeroComprobante);
            $("#categoria").empty();
            $("#categoria").append('<option value="' + data.categoria.codigoCategoria + '">' + data.categoria.tarifa + '-' + data.categoria.nombreCategoria + '</option>');
            $("#tarifa").val(data.categoria.tarifa);
            $("#cobrador").empty();
            $("#cobrador").append('<option value="' + data.cobrador.codigoCobrador + '">' + data.cobrador.nombre + ' ' + data.cobrador.apellido + '</option>');
            $("#sucursalAnular").empty();
            $("#sucursalAnular").append('<option value="' + data.sucursal.codigoSucursal + '">' + data.sucursal.nombreSucursal + '</option>');
            $("#puntoExpedicionAnular").empty();
            $("#puntoExpedicionAnular").append('<option value="' + data.puntoExpedicion.puntoExpedicionPK.codigoPuntoExpedicion + '">' + data.puntoExpedicion.nombrePuntoExpedicion + '</option>');
            $("#tipoFactura").empty();
            $("#tipoFactura").append('<option value="' + data.tipoFactura.codigoTipoFactura + '">' + data.tipoFactura.tipoFactura + '</option>');
            $("#condicionVenta").empty();
            $("#condicionVenta").append('<option value="' + data.condicionVenta.codigoCondicionVenta + '">' + data.condicionVenta.condicionVenta + '</option>');
            $("#periodoPago").val(data.periodoPago);
            $("#cantidadPago").val(data.cantidadPago);
            $("#subTotal").val(formatPYG(data.importe));
            $("#recargoPago").val(data.recargo);
            $("#totalPago").val(formatPYG(data.importe + data.recargo));
        });
}
tabulador("#numeroComprobante", "#cantidadPago");
tabulador("#recargoPago", "#guardar");


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
});
$("#recargoPago").on('keyup', function (e) {
    let recargo = parseInt(soloNumero($("#recargoPago").val()));
    let saldoAnterior = parseInt(soloNumero($("#saldoAnterior").val()));
    let total = 0;
    total = recargo + subTotal - saldoAnterior;
    if ($("#recargoPago").val() !== '') {
        $("#totalPagar").val(formatPYG(total));
    }
    ;
});
$("#recargoPago").blur(function (e) {
    let recargo = parseInt($("#recargoPago").val());
    let total = 0;

    total = recargo + subTotal;
    $("#totalDeuda").val(formatPYG(total));
});
//    -----------------GUARDAR COMPROBANTE-------------------------------
//Campos a limpira
var campos = [
    "#cuentaCorriente",
    "#numeroComprobante",
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

function metosdosPagoAgregados() {
    return $("#tablaMetodoPago tbody tr").length;
}

$(document).on('click', 'tr  td #eliminar', function (event) {
    $(this).closest('tr').remove();
    sumarImportes();
});

function formatPYG(number) {
    return parseInt(number).toLocaleString('es-PY', { style: 'currency', currency: 'PYG' });
}

function soloNumero(value) {
    return value.replace(/[^0-9]/g, '');
}

$("#importe").on('keypress', function (e) {
    if (e.keyCode === 13) {
        agregarMetodoPago();
    }
});

function limpiarMetodosPago() {
    $('#tablaMetodoPago tbody ').empty();
    limpiar([$("#totalImporte")]);
    $("#metodoPago").show();
}
function agregarMetodoPago() {
    if ($("#importe").val().trim() !== '') {
        let importe = parseInt($("#importe").val());
        let codigoMetodoPago = $("#metodoPagoSelect").val();
        let metodoPago = $("#metodoPagoSelect").find('option:selected').text();

        $('#tablaMetodoPago tbody tr').each(function (index, element) {
            if (codigoMetodoPago == $(this).find('td:eq(0)').text()) {
                importeTr = parseInt($(this).find('td:eq(2)').text());
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
        limpiar([$("#importe")]);
    }
}
function sumarImportes() {
    let totalImporte = 0;
    if (metosdosPagoAgregados() > 0) {
        $('#tablaMetodoPago tbody tr').each(function (index, element) {
            importe = parseInt($(this).find('td:eq(2)').text());
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
    let codigoSerie = 0;
    let codigoTimbrado = 0;
    let codigoCondicionVenta = 1;
    let fechaPago = $("#fechaPago").val();
    let pagoHasta = $("#pagoHasta").val();
    let cuentaCorriente = $("#cuentaCorriente").val();
    let numeroComprobante = $("#numeroComprobante").val();
    let codigoPuntoExpedicion = $("#puntoExpedicion").val();
    let codigoTipoFactura = $("#tipoFactura").val();
    let codigoCobrador = $("#cobrador").val();
    let cantidadPago = $("#cantidadPago").val();
    let recargoPago = soloNumero($("#recargoPago").val());
    let saldoAnterior = soloNumero($("#saldoAnterior").val());
    let url = '/comprobante/guardar';

    let datos = {
        codigoSerie: codigoSerie,
        codigoTimbrado: codigoTimbrado,
        cuentaCorriente: cuentaCorriente,
        numeroComprobante: numeroComprobante,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
        codigoTipoFactura: codigoTipoFactura,
        codigoCobrador: codigoCobrador,
        fechaPago: fechaPago,
        codigoCondicionVenta: codigoCondicionVenta,
        pagoHasta: pagoHasta,
        cantidadPago: cantidadPago,
        recargoPago: recargoPago,
        saldoAnterior: saldoAnterior,
        detallePago: getDetallePago()
    };
    let contenType = 'application/json';
    guardar(datos, url, contenType).then(function (data) {
        mostrarAlerta({
            mensaje: data,
            url: url,
            tipo: 'success',
            redirigir: false,
            recargar: false
        });
        let numComprobante = $("#numeroComprobante").val();
        limpiar(campos);
        limpiarMetodosPago();
        $("#numeroComprobante").val(nextNumber(numComprobante));
        $("#cuentaCorriente").focus();
    });

});

$('#frm-comprobante-anular').submit(function (event) {
    event.preventDefault();
    let codigoSerie = 0;
    let codigoTimbrado = 1;
    let numeroComprobante = $("#numeroComprobante").val();
    let codigoPuntoExpedicion = $("#puntoExpedicionAnular").val();
    let codigoTipoFactura = $("#tipoFactura").val();
    let motivoAnulacion = $("#motivoAnulacion").val();
    let url = '/comprobante/anular';
   
    let datos = {
        codigoSerie: codigoSerie,
        codigoTimbrado: codigoTimbrado,
        numeroComprobante: numeroComprobante,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
        codigoTipoFactura: codigoTipoFactura,
        motivoAnulacion: motivoAnulacion
    };
    guardarComprobante(datos, url);
    $("#anularModal").modal('hide', function () {
        $("#frm-comprobante-anular").trigger("reset");
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
                url: url,
                tipo: 'success',
                redirigir: false,
                recargar: false
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
                url: url,
                tipo: 'danger'
            });
        }
    });
}

//------------------Filtrar tabla de comprobantes----------------
function buscarComprobantes() {
    var codigoSucursal = $("#sucursal").val();
    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoSerie = $("#serie").val();
    var numeroComprobante = $("#txtBuscar").val().replace(/[^0-9]/g, '');
    numeroComprobante = (numeroComprobante === '0000000') ? '' : numeroComprobante;
    var numeroPagina = 0;
    var cantidadRegistro = $("#cantidadRegistro").val();
    var url = '/comprobante/filtrar';
    var datos = {
        codigoSucursal: codigoSucursal, codigoPuntoExpedicion: codigoPuntoExpedicion, numeroComprobante: numeroComprobante,
        codigoSerie: codigoSerie, numeroPagina: numeroPagina, cantidadRegistro: cantidadRegistro
    };
    let contentType = 'application/x-www-form-urlencoded';
    consultar(datos, url, contentType).then(function (data) {

            $("tbody").empty();
            $.each(data, function (llave, valor) {
                var datosTabla = `
                                 <tr> 
                                    <td data-id=${valor.tipoFactura.codigoTipoFactura}> ${valor.tipoFactura.tipoFactura} </td>
                                    <td data-id=${valor.puntoExpedicion.puntoExpedicionPK.codigoPuntoExpedicion}> ${valor.puntoExpedicion.nombrePuntoExpedicion} </td>
                                    <td data-id=${valor.numeroComprobante}>  ${valor.numeroComprobante.toString().padStart(7, 0)}</td>
                                    <td> ${valor.cuentaCorriente} </td>
                                    <td> ${valor.nombreUsuario} </td>
                                    <td> ${valor.fechaPago} </td>
                                    <td> ${valor.periodoPago} </td>
                                    <td> ${valor.cantidadPago} </td>
                                    <td> ${valor.tarifa} </td>
                                    <td> ${valor.recargo} </td>
                                    <td> ${valor.importe} </td>
                                    <td> ${valor.estado} </td>
                                    <td  title="Anular">
                                    <a   id="anular" 
                                        data-id=''>
                                        <i class="btn-close fa-regular fa-pen-to-square"></i>
                                    </a>  
                                 </tr> `;
                $("tbody").append(datosTabla);

            });
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


function generarNumeroComprobante() {
    var seleccionado = $("#tipoFactura option:selected").text();
    if (seleccionado === 'MANUAL') {
        $("#numeroComprobante").prop('readonly', false);
    } else {
        $("#numeroComprobante").prop('readonly', true);
    }

    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoTipoFactura = $("#tipoFactura").val();
    var codigoSerie = 0;
    var url = '/comprobante/numComprobante';
    var datos = {
        puntoExpedicionPK: {
            codigoPuntoExpedicion: codigoPuntoExpedicion
        },
        codigoTipoFactura: codigoTipoFactura,
        codigoSerie: codigoSerie
    };
    let token = $("#token").val();
    $.ajax({
        headers: {
            'X-CSRF-TOKEN': token
        },
        url: url,
        data: JSON.stringify(datos),
        type: "post",
        dataType: "json",
        contentType: 'application/json',
        success: function (data) {
            $("#numeroComprobante").val(data.toString().padStart(7, 0));
            $("#cuentaCorriente").focus();
        },
        error: function (error) {

            mostrarAlerta({
                mensaje: error,
                url: url,
                tipo: 'success'
            });
        }
    });


}






