$(document).ready(function () {

    $("#buscarUsuario").autocomplete({
        
        source: function (request, response) {
            $.ajax({
                url: "/usuario/buscar/" + request.term,
                dataType: "json",
                data: {
                    term: request.term
                },
                success: function (data) {
                    response($.map(data, function (item) {
                        return {
                            codigoUsuario: item.codigoUsuario,
                            numeroDocumento: item.numeroDocumento,
                            label: item.numeroDocumento + ' ' + item.nombre + ' ' + item.apellido,
                            usuario: item.nombre + ' ' + item.apellido
                        };
                    }));
                }
            });
        },
        minLength: 1,
        select: function (event, ui) {
            $("#buscarUsuario").val(ui.item.usuario);
            $("#numeroDocumento").val(ui.item.numeroDocumento);
            $("#codigoUsuario").val(ui.item.codigoUsuario);
            return false;
        }
    });
    
//Filtar tabla de usuario

    $("#txtBuscar").keyup(function (e) {
        var filtro = $("#txtBuscar").val();
        if (filtro.length > 1 || filtro.length === 0) {
            var numeroPagina = 0;
            var catidadRegistro = 5;
            var url = '/usuario/listar/pagina';
            var datos = {filtro: filtro, numeroPagina: numeroPagina, catidadRegistro: catidadRegistro};
            $.ajax({
                url: url,
                data: JSON.stringify(datos),
                type: "post",
                dataType: "json",
                contentType: 'application/json',
                success: function (data) {
//                    console.log(data);
                    $("tbody").empty();
                    $.each(data.content, function (llave, valor) {
                        var cargaTabla =
                                " <tr> " +
                                " <td> " + valor.codigoUsuario +
                                "</td><td>" + valor.numeroDocumento +
                                " </td><td>" + valor.nombre +' '+valor.apellido +
                                "</td><td>" + valor.celular +
                                " </td><td>" + valor.telefono +
                                "</td><td>" + valor.barrio +
                                " </td><td>" + valor.direccion +
                                " </td><td>" + valor.nombreSucursal +
                                "</td> <td> <a href='/usuario/editar/" + valor.codigoUsuario + " '  class='btn btn-info btn-md' /> <i class='fa-regular fa-pen-to-square'> </i>  \n\
                     <a href='/usuario/eliminar/" + valor.codigoUsuario + " '  class='btn btn-danger btn-md'>  <i class='fa-solid fa-trash-can'></i> </a> \n\
                    </td>" +
                                " </tr>";

                        $("#tbody").append(cargaTabla);
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