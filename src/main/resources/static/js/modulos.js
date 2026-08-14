
export function limpiar(campos) {
    if (campos.length > 0) {
        campos.forEach(campo => $(campo).val(""));
    }
}

export function tabulador(campoActual, campoDestino) {
    $(campoActual).keydown(function (event) {
        if (event.keyCode === 13) {
            event.preventDefault();
            $(campoDestino).focus();
            if ($(campoDestino).attr("type") === "text") {
                let value = $(campoDestino).val();
                if (value !== '') {
                    $(campoDestino)[0].setSelectionRange(value.length, value.length);
                }
            }
        }
    });
}

export function tabular(campoDestino) {
    $(campoDestino).focus();
    if ($(campoDestino).attr("type") === "text") {
        let value = $(campoDestino).val();
        if (value !== '') {
            $(campoDestino)[0].setSelectionRange(value.length, value.length);
        }
    }
}
import "/js/datepicker-es.js";
export function datePickerInit(campo) {
    $.datepicker.setDefaults($.datepicker.regional["es"]);
    $(campo).datepicker({
        changeYear: true
    }
    );
}
export function consultar(datos, url, contentType = 'application/json') {
    return new Promise(function (resolve, reject) {
        let token = $("#token").val();
        if (contentType === 'application/json') {
            datos = JSON.stringify(datos);
        }
        $.ajax({
            headers: {
                'X-CSRF-TOKEN': token
            },
            url: url,
            data: datos,
            type: "post",
            contentType: contentType,
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
                    tipo: 'danger',
                    time: 5000
                });
                reject(mensajeError);
            }
        });
    });
}
export function consulta(datos, url) {
    return new Promise(function (resolve, reject) {
        let token = $("#token").val();

        $.ajax({
            headers: {
                'X-CSRF-TOKEN': token
            },
            url: url,
            data: datos,
            type: "get",
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
                    tipo: 'danger',
                    time: 5000
                });
                reject(mensajeError);
            }
        });
    });
}
export function guardar(datos, url, contentType) {
    return new Promise(function (resolve, reject) {

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
            contentType: contentType,
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
                    tipo: 'danger',
                    time: 15000
                });
                reject(mensajeError);
            }
        });
    });
}
export function modificar(datos, url, contentType) {
    return new Promise(function (resolve, reject) {

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
            type: "put",
            contentType: contentType,
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
                    tipo: 'danger',
                    time: 5000
                });
                reject(mensajeError);
            }
        });
    });
}

export function eliminarRegistro (mensaje = 'Seguro que desea eliminar este registro??') {
    $(document).on('click', '#eliminar', function (event) {
        confirmacioModal('Eliminacion de Registro!', mensaje);
        var url = $(this).data('url');
        let id = $(this).data('id');
        $('#confirmacionModal').modal('show');
        $('#confirmacionModal').off('click.confirmacion', '#confirmarBoton')
                .one('click.confirmacion', '#confirmarBoton', function () {
            $('#confirmacionModal').modal('hide');
            let token = $("#token").val();
            $.ajax({
                headers: {
                    'X-CSRF-TOKEN': token
                },
                url: url,
                method: 'DELETE',
                data: {
                    id: id
                },
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
                    console.log(error);
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
                        tipo: 'danger',
                        time: 50000
                    });
                }
            });
        });
    });
}
export function eliminar(mensaje = 'Seguro que desea eliminar este registro??') {
    $(document).on('click', '#eliminar', function (event) {
        confirmacioModal('Eliminacion de Registro!', mensaje);
        var url = $(this).data('url');
        var id = $(this).data('id');
        $('#confirmacionModal').modal('show');
        $('#confirmacionModal').off('click.confirmacion', '#confirmarBoton')
                .one('click.confirmacion', '#confirmarBoton', function () {
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
                        tipo: 'danger',
                        time: 50000
                    });
                }
            });
        });
    });
}
function objectToQueryString(obj) {
    return Object.keys(obj).map(key => `${encodeURIComponent(key)}=${encodeURIComponent(obj[key])}`).join('&');
}

export function getReporte(datos, url, contenedorAlertas = '#contenedor-alertas') {
    datos = objectToQueryString(datos);
//    console.log(datos);
    let token = $("#token").val();
    fetch(url, {
        headers: {
            'X-CSRF-TOKEN': token,
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        method: 'POST',
        body: datos
    }).then(response => {
        if (response.status === 200) {
            response.blob().then(report => {
                let url = URL.createObjectURL(report);
//                window.open(url);
                window.location.href = url;
            });
        } else if (response.status === 409) {
            response.json().then(data => {
                mostrarAlerta({
                    mensaje: data.mensaje,
                    url: url,
                    tipo: 'warning',
                    contenedor: contenedorAlertas,
                    time: 7000

                });
            });
        } else if (response.status === 500) {
            response.json().then(data => {
                mostrarAlerta({
                    mensaje: data.mensaje,
                    url: url,
                    tipo: 'danger',
                    contenedor: contenedorAlertas,
                    time: 10000

                });
            });
        }

    }).catch(error => {
        var mensajeError = 'Error al imprimir reporte ' + error;
        mostrarAlerta({
            mensaje: mensajeError,
            url: url,
            tipo: 'danger',
            contenedor: contenedorAlertas,
            time: 5000

        });
    });
}

export function mostrarAlerta(opciones) {
    const {
        mensaje,
        url,
        tipo,
        contenedor = '#contenedor-alertas',
        redirigir = false,
        recargar = false,
        time = 3000
    } = opciones;
    $(contenedor).empty();
    var alerta =
            `<div class="alert modal-header
             alert-${tipo} alert-dismissible fade show" role="alert"> 
            ${mensaje} 
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close">
            <span aria-hidden="true"> &times; </span>
            </button>
            </div>`;
    $(contenedor).append(alerta);
    $(contenedor).fadeIn('slow');
    //alerta para registro  redirigir pagina
    if (redirigir) {
        $(contenedor).fadeOut(time, function () {
            window.location = '/' + url.split('/')[1] + '/listar';
        });
    } else if (!redirigir) {
        $(contenedor).fadeOut(time, function () {
            if (recargar) {
                window.location.reload();
            }
        });
    }
}

export function confirmacioModal(titulo, mensaje) {
    $("#confirmacionModal").remove();
    var frm = `
        <div class="modal fade confirmacion-modal" id="confirmacionModal" tabindex="-1"
             aria-labelledby="confirmacionModalLabel" aria-describedby="confirmacionModalMensaje"
             aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content confirmacion-modal-content">
                    <div class="confirmacion-modal-icon" aria-hidden="true">
                        <i class="fa-solid fa-triangle-exclamation"></i>
                    </div>
                    <div class="modal-body confirmacion-modal-body">
                        <span class="confirmacion-modal-kicker">Confirmación requerida</span>
                        <h5 class="modal-title" id="confirmacionModalLabel">${titulo}</h5>
                        <p id="confirmacionModalMensaje">${mensaje}</p>
                    </div>
                    <button type="button" class="btn-close confirmacion-modal-close"
                            data-bs-dismiss="modal" aria-label="Cerrar"></button>
                    <div class="modal-footer confirmacion-modal-footer">
                        <button type="button" class="btn btn-light confirmacion-cancelar" data-bs-dismiss="modal">
                            <i class="fa-solid fa-xmark"></i> No, cancelar
                        </button>
                        <button type="button" class="btn btn-primario confirmacion-aceptar" id="confirmarBoton">
                            <i class="fa-solid fa-check"></i> Sí, continuar
                        </button>
                    </div>
                </div>
            </div>
        </div>`;
    $("body").append(frm);
}

export function generarPaginacion(page) {
    let paginador = ` <ul class="pagination " ">
        <ul class="pagination " ">
                <li class=" ${page.first ? 'page-item disabled' : 'page-item'}" >
                    <a class="page-link" >Primera</a>
                </li>
                 <li class="${!page.hasPrevious ? 'page-item disabled' : 'page-item'} " >
                    <a class="page-link" ">&laquo;</a>
                </li>
                ${addPages(page)}
                  <li class="${page.last ? 'page-item disabled' : 'page-item'}" >
                    <a class="page-link"}>&raquo;</a>
                </li>

                <li class="${page.last ? 'page-item disabled' : 'page-item'}">
                    <a class="page-link"> &Uacute;ltima</a>
                </li>
        
                <li class="page-item"> 
                    <select  class="page-link"  id="cantidadRegistro">
                        <option value="10" >10</option>
                        <option value="20">20</option>
                        <option value="50">50</option>
                    </select>
                </li>
            </ul>`;
    $("#paginador").append(paginador);


}

export function addPages(page) {
    let filtro = $("#txtBuscar").val();
    let paginas = [];
    page.paginas.forEach((item) => {
        let  pagina = ` <li class="page-item ${item.actual ? 'page-item active' : 'page-item'}"">
                         <a class="page-link" > ${item.numero} </a>
                       </li>`;
        paginas.push(pagina);
    });
    return paginas;

}
