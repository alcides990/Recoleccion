
//Funcion para limppiar campos
function limpiar(campos) {
    if (campos.length > 0) {
        campos.forEach(campo => {
            $(campo).val("");
        });
    }
}
//Funcion tabulador
function  tabulador(campoActual, campoDestino) {
    $(campoActual).keypress(function (event) {
        if (event.keyCode === 13) {
            event.preventDefault();
            $(campoDestino).focus();
        }
    });
}
;
//Funcion tabulador
function  tabular(campoDestino) {
    $(campoDestino).focus();
}
;


function consultar(datos, url) {
    return new Promise(function (resolve, reject) {
        let token = $("#token").val();
        $.ajax({
            headers: {
                'X-CSRF-TOKEN': token
            },
            url: url,
            data: JSON.stringify(datos),
            type: "post",
            contentType: 'application/json',
            dataType: "json",
            success: function (response) {
                resolve(response);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                var mensajeError = jqXHR.responseText;
                if (jqXHR.responseJSON && jqXHR.responseJSON.hasOwnProperty("message")) {
                    mensajeError = jqXHR.responseJSON.message;
                }
                mostrarAlerta(mensajeError, url, 'danger');

                reject(errorThrown);
            }
        });
    });
}
function guardar(datos, url, contentType) {
    if (contentType === 'application/json') {
        datos = JSON.stringify(datos);
    }
    let token = $("#token").val();
    $.ajax({
        url: url,
        headers: {
            'X-CSRF-TOKEN': token
        },
        data: datos,
        type: "post",
        contentType: contentType, // tipo datos que se envia 
//        dataType: "json", //tipo de datos que espera recibir

        success: function (response) {
            mostrarAlerta(response, url, 'success', false, true);
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

function eliminar() {
    $(document).on('click', '#eliminar', function (event) {
//        event.preventDefault();
        var url = $(this).data('url');
        var id = $(this).data('id');
        $('#confirmacionModal').modal('show');
        $('#confirmacionModal').on('click', '#confirmarBoton', function () {
            $('#confirmacionModal').modal('hide');
            let token = $("#token").val();
            $.ajax({
                headers: {
                    'X-CSRF-TOKEN': token
                },
                url: url + id,
                async: false,
                method: 'post',
                cache: false,
//                     dataType: "json", //tipo de datos que espera recibir
                success: function (response) {
                    mostrarAlerta(response, url, 'success', false, true);
                    return false;
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    var mensajeError = jqXHR.responseText;
                    if (jqXHR.responseJSON && jqXHR.responseJSON.hasOwnProperty("message")) {
                        mensajeError = jqXHR.responseJSON.message;
                    }
                    if(jqXHR.status===403){
                        mensajeError="Acceso denegado, no tiene acceso a este recurso!!";
                    }
                    mostrarAlerta(mensajeError, url, 'danger');
                }
            });
        });
    });
}

function getReporte(datos, url) {
//    console.log(datos);
    let token = $("#token").val();
    $.ajax({
        headers: {
            'X-CSRF-TOKEN': token
        },
        url: url,
//            data: JSON.stringify(datos),
        data: datos,
        type: "post",
//        responseType: "arraybuffer",
        xhrFields: {
            responseType: "blob" // Especificar el tipo de respuesta como Blob
        },
        success: function (response, status, xhr) {
//            let blob = new Blob([response], {type: "application/pdf"});
            var url = URL.createObjectURL(new Blob([response], {type: "application/pdf"}));
            window.location.href = (url);// abrir en la misma pestaña
//            window.open(url);

        },
        error: function (xhr, textStatus, error) {
            var mensajeError = 'Error al imprimir reporte ' + xhr.responseText;
            mostrarAlerta(mensajeError, url, 'danger');
        }
    });
}

//Funcion para mostrar mensaje de alerta

function mostrarAlerta(mensaje, url, tipo, redirigir = false, recargar = false) {
    $('#contenedor-alertas').empty();
    var alerta =
            `<div class="alert modal-header 
             alert-${tipo} alert-dismissible fade show" role="alert"> 
            ${ mensaje} 
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close">
            <span aria-hidden="true"> &times; </span>
            </button>
            </div>`;
    $('#contenedor-alertas').append(alerta);
    $('#contenedor-alertas').fadeIn('slow');

    //alerta para registro  redirigir pagina
    if (redirigir && tipo != 'danger') {
        $('#contenedor-alertas').fadeOut(3000, function () {
            window.location = '/' + url.split('/')[1] + '/listar';
        });
    } else if (!redirigir && tipo != 'danger') {
        $('#contenedor-alertas').fadeOut(3000, function () {
            if (recargar) {
                window.location.reload();
            }
        });
}
}
