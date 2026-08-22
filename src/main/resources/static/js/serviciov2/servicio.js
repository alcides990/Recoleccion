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

          const modalUbicacion = new bootstrap.Modal(document.getElementById('ubicacionServicioModal'));
          let cargandoCoordenadas = false;

          function mostrarAlertaUbicacion(mensaje, tipo = 'danger') {
              $('#ubicacionServicioAlerta')
                      .removeClass('d-none alert-danger alert-success alert-info alert-warning')
                      .addClass('alert-' + tipo)
                      .text(mensaje);
          }

          function ocultarAlertaUbicacion() {
              $('#ubicacionServicioAlerta').addClass('d-none').text('');
          }

          function textoCoordenadas(latitud, longitud) {
              if (latitud === null || latitud === undefined
                      || longitud === null || longitud === undefined) return 'Sin ubicación';
              return Number(latitud).toFixed(7) + ', ' + Number(longitud).toFixed(7);
          }

          function fechaHoraUbicacion(valor) {
              if (!valor) return '';
              const fecha = new Date(valor);
              return Number.isNaN(fecha.getTime()) ? escaparHtml(valor)
                      : fecha.toLocaleString('es-PY');
          }

          function actualizarEnlaceMapa() {
              const latitud = $('#ubicacionLatitud').val();
              const longitud = $('#ubicacionLongitud').val();
              const enlace = $('#abrirUbicacionMapa');
              if (latitud !== '' && longitud !== '') {
                  enlace.attr('href', 'https://www.google.com/maps?q='
                          + encodeURIComponent(latitud + ',' + longitud))
                          .removeClass('disabled').attr('aria-disabled', 'false');
              } else {
                  enlace.removeAttr('href').addClass('disabled').attr('aria-disabled', 'true');
              }
          }

          function pintarUbicacion(datos) {
              const actual = datos.actual;
              cargandoCoordenadas = true;
              $('#ubicacionLatitud').val(actual ? actual.latitud : '');
              $('#ubicacionLongitud').val(actual ? actual.longitud : '');
              $('#ubicacionPrecision').val(actual && actual.precisionMetros !== null
                      ? actual.precisionMetros : '');
              $('#ubicacionMetodo').val(actual ? actual.metodo : 'MANUAL');
              cargandoCoordenadas = false;
              $('#ubicacionActualizada').text(actual
                      ? 'Última actualización: ' + fechaHoraUbicacion(actual.fechaActualizacion)
                          + ' · ' + actual.usuario
                      : 'Todavía no tiene una ubicación registrada');
              actualizarEnlaceMapa();

              const historial = Array.isArray(datos.historial) ? datos.historial : [];
              $('#ubicacionHistorialCuerpo').html(historial.length
                      ? historial.map(item => '<tr>'
                          + '<td>' + escaparHtml(fechaHoraUbicacion(item.fechaModificacion)) + '</td>'
                          + '<td>' + escaparHtml(textoCoordenadas(item.latitudAnterior, item.longitudAnterior)) + '</td>'
                          + '<td><strong>' + escaparHtml(textoCoordenadas(item.latitudNueva, item.longitudNueva)) + '</strong>'
                          + (item.precisionNuevaMetros !== null
                              ? '<small class="d-block text-muted">± ' + escaparHtml(item.precisionNuevaMetros) + ' m</small>' : '') + '</td>'
                          + '<td><span class="badge bg-secondary">' + escaparHtml(item.metodo) + '</span>'
                          + '<small class="d-block text-muted">' + escaparHtml(item.origen) + '</small></td>'
                          + '<td>' + escaparHtml(item.usuario) + '</td></tr>').join('')
                      : '<tr><td colspan="5" class="text-center text-muted py-4">Sin modificaciones registradas</td></tr>');
          }

          async function apiUbicacion(url, opciones = {}) {
              const respuesta = await fetch(url, opciones);
              if (!respuesta.ok) {
                  const tipo = respuesta.headers.get('content-type') || '';
                  if (tipo.includes('json')) {
                      const error = await respuesta.json();
                      throw new Error(error.message || error.detail || 'No se pudo completar la operación');
                  }
                  throw new Error(await respuesta.text() || 'No se pudo completar la operación');
              }
              return respuesta.json();
          }

          async function cargarUbicacion(cuentaCorriente) {
              ocultarAlertaUbicacion();
              $('#ubicacionHistorialCuerpo').html('<tr><td colspan="5" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span> Cargando…</td></tr>');
              const datos = await apiUbicacion('/servicio/ubicacion?cuentaCorriente='
                      + encodeURIComponent(cuentaCorriente));
              pintarUbicacion(datos);
          }

          $(document).on('click', '.ubicacion-servicio', async function () {
              const cuentaCorriente = String($(this).data('id') || '');
              $('#ubicacionCuenta').val(cuentaCorriente);
              $('#ubicacionLatitud,#ubicacionLongitud,#ubicacionPrecision').val('');
              $('#ubicacionMetodo').val('MANUAL');
              actualizarEnlaceMapa();
              modalUbicacion.show();
              try {
                  await cargarUbicacion(cuentaCorriente);
              } catch (error) {
                  mostrarAlertaUbicacion(error.message);
              }
          });

          $('#ubicacionLatitud,#ubicacionLongitud').on('input', function () {
              if (!cargandoCoordenadas) {
                  $('#ubicacionMetodo').val('MANUAL');
                  $('#ubicacionPrecision').val('');
              }
              actualizarEnlaceMapa();
          });

          $('#obtenerUbicacionActual').on('click', function () {
              if (!navigator.geolocation) {
                  mostrarAlertaUbicacion('Este dispositivo no permite obtener la ubicación.');
                  return;
              }
              const boton = $(this);
              boton.prop('disabled', true)
                      .html('<span class="spinner-border spinner-border-sm"></span> Obteniendo ubicación…');
              mostrarAlertaUbicacion('Espere mientras se obtiene una posición precisa.', 'info');
              navigator.geolocation.getCurrentPosition(posicion => {
                  cargandoCoordenadas = true;
                  $('#ubicacionLatitud').val(posicion.coords.latitude.toFixed(7));
                  $('#ubicacionLongitud').val(posicion.coords.longitude.toFixed(7));
                  $('#ubicacionPrecision').val(Number(posicion.coords.accuracy).toFixed(2));
                  $('#ubicacionMetodo').val('GPS');
                  cargandoCoordenadas = false;
                  actualizarEnlaceMapa();
                  mostrarAlertaUbicacion('Ubicación obtenida. Revise las coordenadas y presione Guardar ubicación.', 'success');
                  boton.prop('disabled', false)
                          .html('<i class="fa-solid fa-crosshairs"></i> Usar ubicación actual');
              }, error => {
                  const mensajes = {
                      1: 'Permiso de ubicación denegado.',
                      2: 'No fue posible determinar la ubicación.',
                      3: 'Se agotó el tiempo para obtener la ubicación.'
                  };
                  mostrarAlertaUbicacion(mensajes[error.code] || 'No fue posible obtener la ubicación.');
                  boton.prop('disabled', false)
                          .html('<i class="fa-solid fa-crosshairs"></i> Usar ubicación actual');
              }, {enableHighAccuracy: true, timeout: 20000, maximumAge: 0});
          });

          $('#formUbicacionServicio').on('submit', async function (event) {
              event.preventDefault();
              const boton = $('#guardarUbicacionServicio');
              const contenido = boton.html();
              boton.prop('disabled', true)
                      .html('<span class="spinner-border spinner-border-sm"></span> Guardando…');
              ocultarAlertaUbicacion();
              try {
                  const precision = $('#ubicacionPrecision').val();
                  const datos = await apiUbicacion('/servicio/ubicacion', {
                      method: 'POST',
                      headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': tokenCsrf},
                      body: JSON.stringify({
                          cuentaCorriente: $('#ubicacionCuenta').val(),
                          latitud: Number($('#ubicacionLatitud').val()),
                          longitud: Number($('#ubicacionLongitud').val()),
                          precisionMetros: precision === '' ? null : Number(precision),
                          metodo: $('#ubicacionMetodo').val()
                      })
                  });
                  pintarUbicacion(datos);
                  mostrarAlertaUbicacion('Ubicación guardada y registrada en el historial.', 'success');
              } catch (error) {
                  mostrarAlertaUbicacion(error.message);
              } finally {
                  boton.prop('disabled', false).html(contenido);
              }
          });
