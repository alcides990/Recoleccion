
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

export function getReporte(datos, url, contenedorAlertas = '#contenedor-alertas', boton = null) {
    const cuerpo = objectToQueryString(datos);
    const token = $("#token").val();
    const $boton = boton ? $(boton) : $(document.activeElement).filter('button[type="submit"]');
    const contenidoOriginal = $boton.length ? $boton.html() : '';
    const $formulario = $boton.closest('form');
    const $cancelar = $formulario.find('button[type="button"][data-bs-dismiss="modal"]').last();
    const contenidoCancelarOriginal = $cancelar.length ? $cancelar.html() : '';
    const controlador = new AbortController();
    let solicitudFinalizada = false;

    const restaurarControles = () => {
        if ($boton.length) {
            $boton.prop('disabled', false).html(contenidoOriginal);
        }
        if ($cancelar.length) {
            $cancelar.prop('disabled', false).html(contenidoCancelarOriginal);
        }
    };

    $boton.prop('disabled', true)
            .html('<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Generando reporte…');
    $cancelar.html('<i class="fa-solid fa-xmark"></i> Cancelar generación')
            .off('click.cancelarReporte')
            .on('click.cancelarReporte', () => {
                if (!solicitudFinalizada) {
                    controlador.abort();
                    restaurarControles();
                }
            });

    return fetch(url, {
        headers: {
            'X-CSRF-TOKEN': token,
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        method: 'POST',
        body: cuerpo,
        signal: controlador.signal
    }).then(async response => {
        const contentType = response.headers.get('content-type') || '';
        if (response.ok && contentType.includes('application/pdf')) {
            const reporte = await response.blob();
            const reporteUrl = URL.createObjectURL(reporte);
            const ancho = Math.min(1200, Math.round(window.screen.availWidth * 0.88));
            const alto = Math.min(900, Math.round(window.screen.availHeight * 0.88));
            const izquierda = Math.max(0, Math.round((window.screen.availWidth - ancho) / 2));
            const arriba = Math.max(0, Math.round((window.screen.availHeight - alto) / 2));
            const opcionesVentana = `popup=yes,width=${ancho},height=${alto},left=${izquierda},top=${arriba},resizable=yes,scrollbars=yes`;
            const nombreVentana = `reporte_${Date.now()}`;
            const visorPdf = window.open(reporteUrl, nombreVentana, opcionesVentana);
            if (!visorPdf) {
                URL.revokeObjectURL(reporteUrl);
                mostrarAlerta({
                    mensaje: 'El reporte terminó, pero el navegador bloqueó la nueva ventana. Permita ventanas emergentes e intente nuevamente.',
                    url: url,
                    tipo: 'warning',
                    contenedor: contenedorAlertas,
                    time: 8000
                });
                return false;
            }
            try {
                visorPdf.resizeTo(ancho, alto);
                visorPdf.moveTo(izquierda, arriba);
            } catch (e) {
                // El navegador puede restringir el tamaño o la posición de ventanas emergentes.
            }
            visorPdf.focus();
            window.setTimeout(() => URL.revokeObjectURL(reporteUrl), 60000);
            return true;
        }

        let mensaje = 'No fue posible generar el reporte.';
        try {
            const data = contentType.includes('application/json')
                    ? await response.json() : {mensaje: await response.text()};
            mensaje = data.mensaje || data.message || mensaje;
        } catch (e) {
            // Se conserva el mensaje general cuando la respuesta no es legible.
        }
        mostrarAlerta({
            mensaje: mensaje,
            url: url,
            tipo: response.status === 409 ? 'warning' : 'danger',
            contenedor: contenedorAlertas,
            time: response.status === 409 ? 7000 : 10000
        });
        return false;
    }).catch(error => {
        if (error.name === 'AbortError') {
            return false;
        }
        const mensajeError = 'Error al generar el reporte: ' + error.message;
        mostrarAlerta({
            mensaje: mensajeError,
            url: url,
            tipo: 'danger',
            contenedor: contenedorAlertas,
            time: 5000
        });
        return false;
    }).finally(() => {
        solicitudFinalizada = true;
        $cancelar.off('click.cancelarReporte');
        restaurarControles();
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
