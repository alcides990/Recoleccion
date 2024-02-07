
function fechaPagoHastaFormateada(pagoHasta) {
    var fecha = new Date(pagoHasta);
    var anio = fecha.getFullYear();
    var mes = fecha.getMonth() + 1;
    var dia = fecha.getDate();
    var fechaFormateada = dia.toString().padStart(2, '0') + '-' + mes.toString().padStart(2, '0') + '-' + anio;
    return fechaFormateada;
}
function cargarDatosComprobante(datos, url) {
    consultar(datos, url)
            .then(function (data) {
                $("#razonSocial").val(data.usuario.nombre + ' ' + data.usuario.apellido);
                $("#numeroDocumento").val(data.usuario.numeroDocumento);
                $("#categoria").empty();
                $("#categoria").append('<option value="' + data.categoria.codigoCategoria + '">' + data.categoria.categoriaConTarifa + '</option>');
                $("#cobrador").empty();
                $("#cobrador").append('<option value="' + data.cobrador.codigoCobrador + '">' + data.cobrador.nombre + ' ' + data.cobrador.apellido + '</option>');
                $("#tarifa").val(data.categoria.tarifa);

                $("#pagoHasta").val(fechaPagoHastaFormateada(data.estadoCuenta.pagoHasta));
                $("#cantidadDeuda").val(data.estadoCuenta.cantidadDeuda);
                $("#subTotal").val(data.estadoCuenta.subTotal.toLocaleString('es-ES', {style: 'currency', currency: 'PYG'}));
                $("#recargoPago").val(data.estadoCuenta.recargo);
                $("#totalDeuda").val(data.estadoCuenta.totalDeuda.toLocaleString('es-ES', {style: 'currency', currency: 'PYG'}));
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

$(document).on('click', '#anular', function (event) {
    var url = '/comprobante/getComprobante';
    var dataId = $(this).data('id').split('-');
    var codigoSucursal = dataId[0];
    var codigoPuntoExpedicion = dataId[1];
    var codigoTipoFactura = dataId[2];
    var codigoSerie = dataId[3];
    var numeroComprobante = dataId[4];
    var datos = {
        codigoSucursal: codigoSucursal,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
        codigoTipoFactura: codigoTipoFactura,
        codigoSerie: codigoSerie,
        numeroComprobante: numeroComprobante
    };
    consultar(datos, url)
            .then(function (data) {
                $("#cuentaCorriente").val(data.cuentaCorriente);
                $("#razonSocial").val(data.nombreUsuario);
                $("#numeroDocumento").val(data.numeroDocumento);
                $("#numeroComprobante").val(data.numeroComprobante);
                $("#categoria").empty();
                $("#categoria").append('<option value="' + data.categoria.codigoCategoria + '">' + data.categoria.categoriaConTarifa + '</option>');
                $("#cobrador").empty();
                $("#cobrador").append('<option value="' + data.cobrador.codigoCobrador + '">' + data.cobrador.nombre + ' ' + data.cobrador.apellido + '</option>');
                $("#tarifa").val(data.categoria.tarifa);
                $("#sucursal").empty();
                $("#sucursal").append('<option value="' + data.sucursal.codigoSucursal + '">' + data.sucursal.nombreSucursal + '</option>');
                $("#tarifa").val(data.categoria.tarifa);
                $("#puntoExpedicion").empty();
                $("#puntoExpedicion").append('<option value="' + data.puntoExpedicion.codigoPuntoExpedicion + '">' + data.puntoExpedicion.nombrePuntoExpedicion + '</option>');
                $("#tarifa").val(data.categoria.tarifa);
                $("#tipoFactura").empty();
                $("#tipoFactura").append('<option value="' + data.tipoFactura.codigoTipoFactura + '">' + data.tipoFactura.tipoFactura + '</option>');
                $("#tarifa").val(data.categoria.tarifa);
                $("#condicionVenta").empty();
                $("#condicionVenta").append('<option value="' + data.condicionVenta.codigoCondicionVenta + '">' + data.condicionVenta.condicionVenta + '</option>');
                $("#periodoPago").val(data.periodoPago);
                $("#cantidadPago").val(data.cantidadPago);
                $("#subTotal").val(data.importe.toLocaleString('es-ES', {style: 'currency', currency: 'PYG'}));
                $("#recargoPago").val(data.recargo);
                $("#totalDeuda").val((data.importe + data.recargo).toLocaleString('es-ES', {style: 'currency', currency: 'PYG'}));
            });
});
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
    $('#subTotal').val(subTotal.toLocaleString('es-PY', {style: 'currency', currency: 'PYG'}));
});
$("#recargoPago").on('keyup', function (e) {
    var recargo = 0;
    var total = 0;
    recargo = parseInt($("#recargoPago").val());
    total = recargo + subTotal;
    if ($("#recargoPago").val() !== '') {
        $("#totalDeuda").val(total.toLocaleString('es-PY', {style: 'currency', currency: 'PYG'}));
    }
});
$("#recargoPago").blur(function (e) {
    var recargo = 0;
    var total = 0;
    recargo = parseInt($("#recargoPago").val());
    total = recargo + subTotal;
    $("#totalDeuda").val(total.toLocaleString('es-PY', {style: 'currency', currency: 'PYG'}));
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
    "#recargoPago",
    "#totalDeuda"
];
$('#frm-comprobante').submit(function (event) {
    event.preventDefault();
    var codigoSerie = 0;
    var codigoTimbrado = 1;
    var codigoUsuario = 1;
    var codigoMetodoPago = 1;
    var codigoComision = 1;
    var codigoCondicionVenta = 1;
    var cuentaCorriente = $("#cuentaCorriente").val();
    var numeroComprobante = $("#numeroComprobante").val();
    var codigoSucursal = $("#sucursal").val();
    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoTipoFactura = $("#tipoFactura").val();
    var codigoCobrador = $("#cobrador").val();
    var cantidadPago = $("#cantidadPago").val();
    var recargoPago = $("#recargoPago").val();
    var url = '/comprobante/guardar';
    var datos = {
        codigoSerie: codigoSerie,
        codigoTimbrado: codigoTimbrado,
        cuentaCorriente: cuentaCorriente,
        numeroComprobante: numeroComprobante,
        codigoSucursal: codigoSucursal,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
        codigoTipoFactura: codigoTipoFactura,
        codigoUsuario: codigoUsuario,
        codigoCobrador: codigoCobrador,
        codigoCondicionVenta: codigoCondicionVenta,
        codigoMetodoPago: codigoMetodoPago,
        codigoComision: codigoComision,
        cantidadPago: cantidadPago,
        recargoPago: recargoPago
    };
    guardar(datos, url);
    limpiar(campos);
});
$('#frm-comprobante-anular').submit(function (event) {
    event.preventDefault();
    var codigoSerie = 0;
    var codigoTimbrado = 1;
    var numeroComprobante = $("#numeroComprobante").val();
    var codigoSucursal = $("#sucursal").val();
    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoTipoFactura = $("#tipoFactura").val();
    var motivoAnulacion = $("#motivoAnulacion").val();
    var url = '/comprobante/anular';
    var datos = {
        codigoSerie: codigoSerie,
        codigoTimbrado: codigoTimbrado,
        numeroComprobante: numeroComprobante,
        codigoSucursal: codigoSucursal,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
        codigoTipoFactura: codigoTipoFactura,
        motivoAnulacion: motivoAnulacion
    };
    guardar(datos, url);
});
function guardar(datos, url) {
    let token = $("#token").val();
    $.ajax({
        headers: {
            'X-CSRF-TOKEN': token
        },
        url: url,
        data: datos,
        type: "post",
        success: function (response) {
            mostrarAlerta(response, url, 'success', false, true);
            getNumComprobante();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            var mensajeError = jqXHR.responseText;
            if (jqXHR.responseJSON && jqXHR.responseJSON.hasOwnProperty("message")) {
                mensajeError = jqXHR.responseJSON.message;
            }
            mostrarAlerta(mensajeError, url, 'danger');
        }
    });
}

//------------------Filtrar tabla de comprobantes----------------
function buscarComprobantes() {
    var codigoSucursal = $("#sucursal").val();
    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var numeroComprobante = $("#txtBuscar").val();
    if (numeroComprobante.length > 3 || numeroComprobante.length === 0) {
        var numeroPagina = 0;
        var cantidadRegistro = $("#cantidadRegistro").val();
        var url = '/comprobante/filtrar';
        var datos = {codigoSucursal: codigoSucursal, codigoPuntoExpedicion: codigoPuntoExpedicion,
            numeroComprobante: numeroComprobante, numeroPagina: numeroPagina, cantidadRegistro: cantidadRegistro};
        let token = $("#token").val();
        $.ajax({
            headers: {
                'X-CSRF-TOKEN': token
            },
            url: url,
            data: datos,
            type: "post",
            dataType: "json",
            success: function (data) {
                $("tbody").empty();
                $.each(data, function (llave, valor) {
                    var datosTabla = `
                                 <tr> 
                                    <td> ${valor.tipoFactura.tipoFactura} </td>
                                    <td> ${valor.numeroComprobante.toString().padStart(7, 0)}</td>
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
                                        data-bs-toggle="modal"
                                        data-bs-target="#anularModal"
                                        data-id='${valor.sucursal.codigoSucursal}-${valor.puntoExpedicion.codigoPuntoExpedicion}-${valor.tipoFactura.codigoTipoFactura}-${valor.codigoSerie}-${valor.numeroComprobante}'>
                                        <i class="btn-close fa-regular fa-pen-to-square"></i>
                                    </a>  
                                 </tr> `;
                    $("tbody").append(datosTabla);
                });
            },
            error: function (jqXHR, textStatus, errorThrown) {
                var mensajeError = jqXHR.responseText;
                if (jqXHR.responseJSON && jqXHR.responseJSON.hasOwnProperty("message")) {
                    mensajeError = jqXHR.responseJSON.message;
                }
                mostrarAlerta(mensajeError, url, 'danger');
            }
        });
    }
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

    var codigoSucursal = $("#sucursal").val();
    var codigoPuntoExpedicion = $("#puntoExpedicion").val();
    var codigoTipoFactura = $("#tipoFactura").val();
    var codigoSerie = 0;
    var url = '/comprobante/numComprobante';
    var datos = {
        codigoSucursal: codigoSucursal,
        codigoPuntoExpedicion: codigoPuntoExpedicion,
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
        error: function (jqXHR, textStatus, errorThrown) {

            mostrarAlerta(jqXHR.responseText, url, 'danger');
        }
    });


}






