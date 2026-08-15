import {consultar, guardar, eliminarRegistro, limpiar, mostrarAlerta, tabulador, tabular, datePickerInit}
           from  '/js/modulos.js';

          if (document.getElementById('tabla-servicio').dataset.serverSide !== 'true') {
              import('/js/buscarServicios.js');
          }

          eliminarRegistro('Seguro que desea eliminar el cuenta de usuario');
          tabulador('#cuentaCorriente', '#buscarUsuario');
          tabulador('#buscarUsuario', '#fechaInicio');

          datePickerInit('#fechaInicio');
          datePickerInit('#suspensionFecha');
          datePickerInit('#exoneracionFecha');

          $('#tabla').DataTable({
              language: {
                  url: '/i18n/es-ES.json'
              }
          });
          var accion = "";
          var campos = ["#cuentaCorriente", "#fechaInicio", "#buscarUsuario", "#numeroDocumento"];

          const formatoNumero = new Intl.NumberFormat('es-PY', {maximumFractionDigits: 0});
          const formatoFecha = fecha => {
              if (!fecha) return 'Sin pagos';
              const [anio, mes, dia] = fecha.split('-');
              return `${dia}-${mes}-${anio}`;
          };

          $(document).on('click', '.estado-cuenta', async function () {
              const cuentaCorriente = $(this).data('id');
              const modal = new bootstrap.Modal(document.getElementById('estadoCuentaModal'));
              $('#estadoCuentaError').addClass('d-none').text('');
              $('#estadoCuentaContenido').addClass('d-none');
              $('#verEstadoCuentaCompleto').attr('href', '/servicio/estadoCuenta?cuentaCorriente=' + encodeURIComponent(cuentaCorriente));
              $('#estadoSuspensiones,#estadoExoneraciones,#estadoPagos')
                      .attr('data-id', cuentaCorriente)
                      .data('id', cuentaCorriente);
              modal.show();

              try {
                  const respuesta = await fetch('/servicio/estadoCuenta/resumen?cuentaCorriente='
                          + encodeURIComponent(cuentaCorriente) + '&_=' + Date.now(), {cache: 'no-store'});
                  if (!respuesta.ok) {
                      throw new Error(await respuesta.text() || 'No se pudo consultar el estado de cuenta');
                  }
                  const resumen = await respuesta.json();
                  $('#resumenCuenta').text(resumen.cuentaCorriente);
                  $('#resumenUsuario').text(resumen.usuario);
                  $('#resumenUltimoPago').val(formatoFecha(resumen.ultimoPago));
                  $('#resumenPagoDesde').val(resumen.pagoDesde || '');
                  $('#resumenCantidadPeriodos').val(resumen.cantidadPeriodos);
                  $('#resumenTarifa').val(formatoNumero.format(resumen.tarifa));
                  $('#resumenSubtotal').val(formatoNumero.format(resumen.subTotal));
                  $('#resumenSaldo').val(formatoNumero.format(resumen.saldoAnterior));
                  $('#resumenRecargo').val(formatoNumero.format(resumen.recargo));
                  $('#resumenTotal').val(formatoNumero.format(resumen.totalDeuda));
                  $('#estadoCuentaContenido').removeClass('d-none');
              } catch (error) {
                  $('#estadoCuentaError').removeClass('d-none').text(error.message);
              }
          });

          $("#servicioModal").on('shown.bs.modal', function () {
              tabular("#cuentaCorriente");
          });

          $("#btnAgrerar").click(function (event) {
              event.preventDefault();
              limpiar(campos);
              $("#confirmacionModalLabel").text("Nuevo servicio");
              $("#cuentaCorriente").prop("readonly", false);
              $("#fechaInicio").prop("disabled", false);
              $("#ocupado").val('OCUPADO');
              $("#accion").val("agregar");
              $("#servicioModal").modal('show');
          });

          $(document).on('click', '#editar', function (event) {
              $("#confirmacionModalLabel").text("Editar servicio");
              $("#cuentaCorriente").prop("readonly", true);
              $("#fechaInicio").prop("disabled", true);

              var url = '/servicio/editar';
              var servicio = {cuentaCorriente: $(this).data('id')};

              consultar(servicio, url)
                      .then(function (respuesta) {
                          $("#cuentaCorriente").val(respuesta.cuentaCorriente);
                          $("#fechaInicio").val(respuesta.fechaInicio);
                          $("#ocupado").val(respuesta.ocupado || 'OCUPADO');
                          $("#numeroDocumento").val(respuesta.usuario.numeroDocumento);
                          $("#codigoUsuario").val(respuesta.usuario.codigoUsuario);
                          $("#buscarUsuario").val(respuesta.usuario.nombre + ' ' + respuesta.usuario.apellido);
                          $("#categoria").val(respuesta.categoria.codigoCategoria);
                          $("#estado").val(respuesta.estado.codigoEstado);
                          $("#direccion").val(respuesta.direccion);
                          $("#accion").val("editar");
                      });
          });
          $('#frm-servicio').submit(function (event) {
              event.preventDefault();
              var servicio = {
                  cuentaCorriente: $("#cuentaCorriente").val(),
                  direccion: $("#direccion").val(),
                  fechaInicio: $("#fechaInicio").val(),
                  ocupado: $("#ocupado").val(),
                  usuario: {
                      codigoUsuario: $("#codigoUsuario").val()
                  },
                  categoria: {
                      codigoCategoria: $("#categoria").val()
                  },
                  sucursal: {
                      codigoSucursal: $("#sucursal").val()
                  },
                  estado: {
                      codigoEstado: $("#estado").val()
                  }
              };
              accion = $("#accion").val();
              var contentType = 'application/json';
              var url = "/servicio/guardar/" + accion;
              guardar(servicio, url, contentType).then((data) => {
                  limpiar(campos);
                  $("#servicioModal").modal('hide');
                  mostrarAlerta({
                      mensaje: data,
                      url: url,
                      tipo: 'success',
                      recargar: true
                  });
              });
              ;

          });

          let cuentaHistorial = '';
          let tablaPagos = null;
          const modalHistorial = new bootstrap.Modal(document.getElementById('historialServicioModal'));
          const tokenCsrf = $('#token').val();
          const escaparHtml = valor => $('<div>').text(valor ?? '').html();
          const hoyIso = () => new Date().toISOString().slice(0, 10);

          function prepararHistorial(titulo, panel) {
              $('#historialServicioTitulo').text(titulo);
              $('#historialServicioAlerta').addClass('d-none').text('');
              $('#panelSuspensiones,#panelExoneraciones,#panelPagos').addClass('d-none');
              $(panel).removeClass('d-none');
              const estadoCuentaElemento = document.getElementById('estadoCuentaModal');
              const estadoCuentaInstancia = bootstrap.Modal.getInstance(estadoCuentaElemento);
              if (estadoCuentaElemento.classList.contains('show') && estadoCuentaInstancia) {
                  $(estadoCuentaElemento).one('hidden.bs.modal', () => modalHistorial.show());
                  estadoCuentaInstancia.hide();
              } else {
                  modalHistorial.show();
              }
          }

          async function apiHistorial(url, opciones = {}) {
              const respuesta = await fetch(url, opciones);
              if (!respuesta.ok) throw new Error(await respuesta.text() || 'No se pudo completar la operación');
              const tipo = respuesta.headers.get('content-type') || '';
              return tipo.includes('json') ? respuesta.json() : respuesta.text();
          }

          async function cargarSuspensiones(cuenta) {
              cuentaHistorial = cuenta;
              const datos = await apiHistorial('/servicio/historial/suspensiones?cuentaCorriente=' + encodeURIComponent(cuenta));
              $('#suspensionesCuerpo').html(datos.length ? datos.map(item => `<tr>
                  <td>${escaparHtml(item.fechaDesde)}</td><td>${escaparHtml(item.fechaHasta || 'Activa')}</td>
                  <td><span class="badge bg-info text-dark">${escaparHtml(item.cantidadPeriodosPendientes)} mes(es)</span></td>
                  <td>${escaparHtml(item.motivo)}</td><td>${escaparHtml(item.usuario)}</td>
                  <td>${item.fechaHasta ? '' : '<button class="btn btn-success btn-sm finalizar-suspension"><i class="fa-solid fa-play"></i> Reactivar</button>'}</td>
              </tr>`).join('') : '<tr><td colspan="6" class="text-center text-muted">Sin suspensiones registradas</td></tr>');
          }

          $(document).on('click', '.historial-suspensiones', async function () {
              const cuenta = $(this).data('id'); prepararHistorial('Historial de suspensión · ' + cuenta, '#panelSuspensiones');
              $('#suspensionFecha').val(hoyIso());
              try { await cargarSuspensiones(cuenta); } catch (e) { $('#historialServicioAlerta').removeClass('d-none').text(e.message); }
          });

          $('#formSuspension').on('submit', async function (event) {
              event.preventDefault();
              try {
                  await apiHistorial('/servicio/historial/suspensiones', {method:'POST', headers:{'Content-Type':'application/json','X-CSRF-TOKEN':tokenCsrf}, body:JSON.stringify({cuentaCorriente:cuentaHistorial,fechaDesde:$('#suspensionFecha').val(),motivo:$('#suspensionMotivo').val()})});
                  $('#suspensionMotivo').val(''); await cargarSuspensiones(cuentaHistorial);
              } catch (e) { $('#historialServicioAlerta').removeClass('d-none').text(e.message); }
          });

          $(document).on('click', '.finalizar-suspension', async function () {
              try {
                  await apiHistorial('/servicio/historial/suspensiones/finalizar', {method:'POST', headers:{'Content-Type':'application/json','X-CSRF-TOKEN':tokenCsrf}, body:JSON.stringify({cuentaCorriente:cuentaHistorial,fechaHasta:hoyIso()})});
                  await cargarSuspensiones(cuentaHistorial);
              } catch (e) { $('#historialServicioAlerta').removeClass('d-none').text(e.message); }
          });

          async function cargarExoneraciones(cuenta) {
              cuentaHistorial = cuenta;
              const datos = await apiHistorial('/servicio/historial/exoneraciones?cuentaCorriente=' + encodeURIComponent(cuenta));
              $('#exoneracionesCuerpo').html(datos.length ? datos.map(item => `<tr><td>${escaparHtml(item.fechaDesdeAnterior)}</td><td>${escaparHtml(item.fechaDesdeNueva)}</td><td><span class="badge bg-warning text-dark">${escaparHtml(item.cantidadPeriodos)} mes(es)</span></td><td>${escaparHtml(item.motivo)}</td><td>${escaparHtml(item.usuario)}</td></tr>`).join('') : '<tr><td colspan="5" class="text-center text-muted">Sin exoneraciones registradas</td></tr>');
          }

          $(document).on('click', '.historial-exoneraciones', async function () {
              const cuenta = $(this).data('id'); prepararHistorial('Historial de exoneración · ' + cuenta, '#panelExoneraciones');
              $('#exoneracionFecha').val(hoyIso());
              try { await cargarExoneraciones(cuenta); } catch (e) { $('#historialServicioAlerta').removeClass('d-none').text(e.message); }
          });

          $('#formExoneracion').on('submit', async function (event) {
              event.preventDefault();
              try {
                  await apiHistorial('/servicio/historial/exoneraciones', {method:'POST', headers:{'Content-Type':'application/json','X-CSRF-TOKEN':tokenCsrf}, body:JSON.stringify({cuentaCorriente:cuentaHistorial,fechaDesdeNueva:$('#exoneracionFecha').val(),motivo:$('#exoneracionMotivo').val()})});
                  $('#exoneracionMotivo').val(''); await cargarExoneraciones(cuentaHistorial);
              } catch (e) { $('#historialServicioAlerta').removeClass('d-none').text(e.message); }
          });

          $(document).on('click', '.historial-pagos', function () {
              cuentaHistorial = $(this).data('id'); prepararHistorial('Historial de pagos · ' + cuentaHistorial, '#panelPagos');
              if (tablaPagos) tablaPagos.destroy();
              tablaPagos = $('#tablaHistorialPagos').DataTable({processing:true,serverSide:true,searchDelay:300,pageLength:10,order:[[1,'desc']],
                  ajax:{url:'/servicio/historial/pagos',type:'GET',
                      data:d=>{d.cuentaCorriente=cuentaHistorial;},
                      dataSrc:json=>{
                          if (!json || !Array.isArray(json.data)) throw new Error('Respuesta inválida del historial de pagos');
                          $('#historialServicioAlerta').addClass('d-none').text('');
                          return json.data;
                      },
                      error:xhr=>$('#historialServicioAlerta').removeClass('d-none')
                              .text(xhr.responseText || 'No se pudo cargar el historial de pagos')},
                  columns:[{data:'numeroComprobante'},{data:'fechaPago'},{data:'periodoPago',defaultContent:''},{data:'cantidadPago'},{data:'totalImporte',render:v=>formatoNumero.format(v||0)},{data:'estado'}],
                  language:{url:'/i18n/es-ES.json'}, responsive:true
              });
          });

          $('#btnExtractoPdf').on('click', async function () {
              const boton = $(this);
              const contenidoOriginal = boton.html();
              const estado = $('#extractoEstado');
              boton.prop('disabled', true).html('<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> <span>Generando…</span>');
              estado.removeClass('d-none alert-danger alert-success').addClass('alert-info')
                      .html('<i class="fa-solid fa-circle-notch fa-spin"></i> Generando el extracto completo. Espere un momento…');
              const visorPdf = window.open('', '_blank');
              if (visorPdf) {
                  visorPdf.document.write('<!doctype html><html><head><title>Generando extracto</title></head><body style="font-family:sans-serif;padding:2rem"><p>Generando extracto PDF…</p></body></html>');
              }
              try {
                  const cuerpo = new URLSearchParams({cuentaCorriente:cuentaHistorial,_csrf:tokenCsrf});
                  const respuesta = await fetch('/servicio/extractoCuenta',{method:'POST',headers:{'X-CSRF-TOKEN':tokenCsrf,'Content-Type':'application/x-www-form-urlencoded'},body:cuerpo});
                  if (!respuesta.ok) {
                      const tipo = respuesta.headers.get('content-type') || '';
                      const error = tipo.includes('json') ? await respuesta.json() : await respuesta.text();
                      throw new Error(typeof error === 'string' ? error : (error.mensaje || error.message || 'No se pudo generar el extracto PDF'));
                  }
                  const urlPdf = URL.createObjectURL(await respuesta.blob());
                  if (visorPdf) {
                      visorPdf.location.replace(urlPdf);
                  } else {
                      window.open(urlPdf, '_blank');
                  }
                  setTimeout(() => URL.revokeObjectURL(urlPdf), 60000);
                  estado.removeClass('alert-info').addClass('alert-success').html('<i class="fa-solid fa-circle-check"></i> Extracto generado y abierto para visualizar.');
              } catch(e) {
                  if (visorPdf && !visorPdf.closed) visorPdf.close();
                  estado.removeClass('alert-info').addClass('alert-danger').html('<i class="fa-solid fa-triangle-exclamation"></i> ' + escaparHtml(e.message || e));
              } finally {
                  boton.prop('disabled', false).html(contenidoOriginal);
              }
          });
