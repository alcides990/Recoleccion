
export function limpiar(campos) {
    if (campos.length > 0) {
        campos.forEach(campo => {
            $(campo).val("");
        });
    }
}

export function tabulador(campoActual, campoDestino) {
    $(campoActual).keydown(function (event) {
        if (event.keyCode === 13) {
            event.preventDefault();
            $(campoDestino).focus();
        }
    });
}

export function tabular(campoDestino) {
    $(campoDestino).focus();
}

export function consultar(datos, url, contentType = 'application/json') {
    console.log();
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
                    tipo: 'danger'
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
                    tipo: 'danger'
                });
                reject(mensajeError);
            }
        });
    });
}

export function eliminar(mensaje = 'Seguro que desea eliminar este registro??') {
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

export function getReporte(datos, url) {
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
//            window.location.href = (url);// abrir en la misma pestaña
            window.open(url);
        },
        error: function (xhr) {
            var mensajeError = 'Error al imprimir reporte ' + xhr.responseText;
            mostrarAlerta({
                mensaje: mensajeError,
                url: url,
                tipo: 'danger'
            });
        }
    });
}


export function mostrarAlerta(opciones) {
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
            ${mensaje} 
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
        $('#contenedor-alertas').fadeOut(4000, function () {
            if (recargar) {
                window.location.reload();
            }
        });
    }
}

export function confirmacioModal(titulo, mensaje) {
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
    $("#cantidadRegistro").val(page.cantidadRegistro);
    $("#paginador ul a").click(function () {
        let numeroPagina = $(this).text();
        console.log(numeroPagina);
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
