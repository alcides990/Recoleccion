$(document).ready(function () {

    //------------validar formulario de comprobante ----------------------------------------

    //    cargar datos de la cuenta al presionar enter en el campo cuentaCorriente
    $("#cuentaCorriente").on('keypress', function (e) {
        if (e.keyCode === 13) {
            e.preventDefault();
            var cuentaCorriente = $("#cuentaCorriente").val();
            var url = '/comprobante/facturar';
            var datos = { cuentaCorriente: cuentaCorriente };
            cargarDatosComprobante(datos, url);
        }
    });
    //Abrir ventans modal al precionar boton editar
    $(document).on('click', '#anular', function (event) {
        var url = '/comprobante/anular';
        var dataId = $(this).data('id').split('-');
        console.log(dataId);
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
        cargarDatosComprobante(datos, url);
    });
    tabulador("#numeroComprobante", "#cantidadPago");
    tabulador("#recargoPago", "#guardar");

    function cargarDatosComprobante(datos, url) {
        consultar(datos, url)
            .then(function (data) {
                //                    console.log(data);
                $("#razonSocial").val(data.nombreUsuario);
                $("#numeroDocumento").val(data.usuario.numeroDocumento);
                $("#categoria").empty();
                $("#categoria").append('<option value="' + data.categoria.codigoCategoria + '">' + data.categoria.categoriaConTarifa + '</option>');
                $("#cobrador").empty();
                $("#cobrador").append('<option value="' + data.cobrador.codigoCobrador + '">' + data.cobrador.nombre + ' ' + data.cobrador.apellido + '</option>');
                $("#tarifa").val(data.categoria.tarifa);

                var fecha = new Date(data.estadoCuenta.pagoHasta); // Crea un objeto Date a partir de la cadena de fecha y hora
                var anio = fecha.getFullYear(); // Obtiene el año de cuatro dígitos
                var mes = fecha.getMonth() + 1; // Obtiene el mes (0-11). Sumamos 1 porque los meses se indexan desde 0.
                var dia = fecha.getDate(); // Obtiene el día del mes (1-31)
                // Formatea la fecha en el formato deseado (por ejemplo, DD/MM/AAAA)
                var fechaFormateada = dia.toString().padStart(2, '0') + '-' + mes.toString().padStart(2, '0') + '-' + anio;
                $("#pagoHasta").val(fechaFormateada);
                $("#cantidadDeuda").val(data.estadoCuenta.cantidadDeuda);
                $("#subTotal").val(data.estadoCuenta.subTotal.toLocaleString('es-ES', { style: 'currency', currency: 'PYG' }));
                $("#recargoPago").val(data.estadoCuenta.recargo);
                $("#totalDeuda").val(data.estadoCuenta.totalDeuda.toLocaleString('es-ES', { style: 'currency', currency: 'PYG' }));

                if ($("#tipoFactura option:selected").text() == 'MANUAL') {
                    $("#numeroComprobante").focus();
                } else {
                    $("#cantidadPago").focus();
                    $("#cantidadPago").val("");
                }
            });
    }
    //----------- obtener numero de comprobante para registrar-----------
    function getNumComprobante() {
        var seleccionado = $("#tipoFactura option:selected").text();
        if (seleccionado != 'MANUAL') {
            $("#numeroComprobante").prop('readonly', true);
            $("#puntoExpedicion").prop('readonly', true);
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
            $.ajax({
                url: url,
                data: JSON.stringify(datos),
                type: "post",
                dataType: "json",
                contentType: 'application/json',
                success: function (data) {
                    $("#numeroComprobante").val(data);
                    $("#cuentaCorriente").focus();
                },
                error: function (jqXHR, textStatus, errorThrown) {

                    mostrarAlerta(jqXHR.responseText, url, 'danger');
                }
            });


        } else {
            $("#numeroComprobante").prop('readonly', false);
            $("#numeroComprobante").val('');
            $("#cuentaCorriente").focus();
        }

    }

    //     Al cargar la pagina se genera numero de comprobante
    getNumComprobante();


    //     Al seleccionar puntoExpedicion se genera numero de comprobante
    $('#puntoExpedicion').on('change', function () {
        getNumComprobante();
    });
    //     Al seleccionar tipoFactura se genera numero de comprobante
    $(document).on('change', '#tipoFactura', function () {
        getNumComprobante();
    });

    var subTotal = 0;
    $("#cantidadPago").on('keydown', function (e) {
        if (e.keyCode == 13 || e.keyCode == 9) { // 13 es el código de tecla de Enter
            e.preventDefault(); // evita el comportamiento predeterminado de Enter (enviar formulario)
            $('#recargoPago').focus();
        }
    });
    $("#cantidadPago").blur(function (e) {
        var cantidadPago = $('#cantidadPago').val();
        var tarifa = $('#tarifa').val();
        subTotal = cantidadPago * tarifa;
        $('#subTotal').val(subTotal.toLocaleString('es-PY', { style: 'currency', currency: 'PYG' }));
    });
    $("#recargoPago").on('keyup', function (e) {
        var recargo = 0;
        var total = 0;
        recargo = parseInt($("#recargoPago").val());
        total = recargo + subTotal;
        if ($("#recargoPago").val() !== '') {
            $("#totalDeuda").val(total.toLocaleString('es-PY', { style: 'currency', currency: 'PYG' }));
        }
    });
    $("#recargoPago").blur(function (e) {
        var recargo = 0;
        var total = 0;
        recargo = parseInt($("#recargoPago").val());
        total = recargo + subTotal;
        $("#totalDeuda").val(total.toLocaleString('es-PY', { style: 'currency', currency: 'PYG' }));
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

    function guardar(datos, url) {
        $.ajax({
            url: url,
            data: datos,
            type: "post",
            //            contentType: 'application/json', // tipo datos que se envia 
            //            dataType: "json", //tipo de datos que espera recibir

            success: function (response) {
                mostrarAlerta(response, url, 'success');
//                limpiar(campos);
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
    //Filtar tabla de comprobante

    $("#txtBuscar").keyup(function (e) {
        var filtro = $("#txtBuscar").val();
        if (filtro.length > 1 || filtro.length === 0) {
            var numeroPagina = 0;
            var catidadRegistro = 10;
            var url = '/comprobante/filtrar';
            var datos = { filtro: filtro, numeroPagina: numeroPagina, catidadRegistro: catidadRegistro };
            $.ajax({
                url: url,
                data: datos,
                type: "post",
                dataType: "json",
                //                contentType: 'application/json',
                success: function (data) {
                    //                    console.log(data);
                    $("tbody").empty();
                    $.each(data, function (llave, valor) {
                        var datosTabla = `
                                 <tr> 
                                    <td> ${valor.tipoFactura} </td>
                                    <td> ${valor.sucursal}- ${valor.puntoExpedicion}- ${valor.numeroComprobante}</td>
                                    <td> ${valor.cuentaCorriente} </td>
                                    <td> ${valor.usuario} </td>
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
                                        data-id='${valor.sucursal.codigoSucursal}-${valor.codigoPuntoExpedicion}-${valor.codigoTipoFactura}-${valor.codigoSerie}-${valor.numeroComprobante}'>
                                        <i class="btn-close fa-regular fa-pen-to-square"></i>
                                    </a>  
                                 </tr> `;
                        $("tbody").append(datosTabla);
                    });

                },
                error: function (jqXHR, textStatus, errorThrown) {
                    alert('Error...' + jqXHR);
                }
            });
        }
    });
    //esta funciona redireciona a la pagina de inicio con el cambiando la cantidad de
    // registro que con el valor seleccionado en el select 
    $("#cantElemento").change(function () {
        var cantElemento = $(this).val();
        var url = new URL(window.location.href);
        var searchParams = new URLSearchParams(url.search);
        searchParams.set('page', 0);
        searchParams.set('cantElemento', cantElemento);
        url.search = searchParams.toString();
        window.location.href = url;
    });

});





