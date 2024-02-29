
//Funcion para limppiar campos
function limpiar(campos) {
    if (campos.length > 0) {
        campos.forEach(campo => {
            $(campo).val("");
        });
    }
}

function  tabulador(campoActual, campoDestino) {
    $(campoActual).keydown(function (event) {
        if (event.keyCode === 13) {
            event.preventDefault();
            $(campoDestino).focus();
        }
    });
}

function  tabular(campoDestino) {
    $(campoDestino).focus();
}

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

                reject(mensajeError);
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
            mostrarAlerta({
                mensaje: response,
                url: url,
                tipo: 'success',
                redirigir: false,
                recargar: true
            });
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

function eliminar(mensaje = 'Seguro que desea eliminar este registro??') {
    $(document).on('click', '#eliminar', function (event) {
        confirmacioModal('Eliminacion de Registro!', mensaje);
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
                success: function (response) {
                    mostrarAlerta({
                        mensaje: response,
                        url: url,
                        tipo: 'success',
                        recargar: true
                    });
                    return false;
                },
                error: function (error) {
                    var mensajeError = error.responseText;
                    if (error.responseJSON && error.responseJSON.hasOwnProperty("message")) {
                        mensajeError = error.responseJSON.message;
                    }
                    if (error.status === 403) {
                        mensajeError = "Acceso denegado, no tiene acceso a este recurso!!";
                    }
                    mostrarAlerta({
                        mensaje: mensajeError,
                        url: url,
                        tipo: 'danger'
                    });
                }
            });
        });
    });
}

function getReporte(datos, url) {
    let token = $("#token").val();
    $.ajax({
        headers: {
            'X-CSRF-TOKEN': token
        },
        url: url,
        data: datos,
        type: "post",
        xhrFields: {
            responseType: "blob"
        },
        success: function (response, status, xhr) {
            var url = URL.createObjectURL(new Blob([response], {type: "application/pdf"}));
            window.location.href = (url);// abrir en la misma pestaña
//            window.open(url);
        },
        error: function (xhr, textStatus, error) {
            var mensajeError = 'Error al imprimir reporte ' + xhr.responseText;
            mostrarAlerta(mensajeError, url, 'danger');
            mostrarAlerta({
                mensaje: mensajeError,
                url: url,
                tipo: 'danger'
            });
        }
    });
}


function mostrarAlerta(opciones) {
    const {
        mensaje,
        url,
        tipo,
        redirigir = false,
        recargar = false
    } = opciones;

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

function confirmacioModal(titulo, mensaje) {
    $("#confirmacionModal").remove();
    var frm = `
 <div class="modal fade" id="confirmacionModal" tabindex="-1" aria-labelledby="confirmacionModalLabel"
                 aria-hidden="true">
                <div class="modal-dialog ">
                    <div class="modal-content bg-color ">
                        <div class="modal-header">
                            <h5 class="modal-title text-center" id="confirmacionModalLabel"> ${titulo}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close">
                                <span aria-hidden="true">&times;</span>
                            </button>

                        </div>
                        <div class="alert modal-body alert-dismissible fade show " role="alert">
                            ${mensaje}
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                            <button type="button" class="btn btn-primario" id="confirmarBoton">Aceptar</button>
                        </div>
                    </div>
                </div>
            </div>`;
    $("body").append(frm);
}
