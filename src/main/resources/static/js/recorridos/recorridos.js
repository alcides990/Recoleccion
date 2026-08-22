$(function () {
    const token = $('#token').val();
    const modalDetalle = new bootstrap.Modal(document.getElementById('detalleRecorridoModal'));
    const escapar = valor => $('<div>').text(valor ?? '').html();
    const CLAVE_COLA_GPS = 'recoleccion.recorridos.colaGps.v1';
    const CLAVE_SEGUIMIENTO_GPS = 'recoleccion.recorridos.seguimientoGps.v1';
    const INTERVALO_MINIMO_MS = 15000;
    const DISTANCIA_MINIMA_METROS = 20;
    let idVigilanciaGps = null;
    let sincronizandoGps = false;
    let ultimaCapturaGps = null;
    let seguimientoGps = leerLocal(CLAVE_SEGUIMIENTO_GPS, null);
    let recorridoDetalleActual = null;
    let paginaPuntosActual = 0;
    let mapaRecorrido = null;
    let capaTrazoRecorrido = null;
    let capaPuntosRecorrido = null;
    let capaPermanenciasRecorrido = null;
    let marcadoresPermanencia = [];
    let marcadorPuntoSeleccionado = null;
    let ultimosPuntosMapa = [];
    let ultimasPermanenciasMapa = [];

    function leerLocal(clave, valorPredeterminado) {
        try {
            const valor = localStorage.getItem(clave);
            return valor ? JSON.parse(valor) : valorPredeterminado;
        } catch (_) {
            return valorPredeterminado;
        }
    }

    function guardarLocal(clave, valor) {
        localStorage.setItem(clave, JSON.stringify(valor));
    }

    function colaGps() {
        const cola = leerLocal(CLAVE_COLA_GPS, []);
        return Array.isArray(cola) ? cola : [];
    }

    function guardarColaGps(cola) {
        guardarLocal(CLAVE_COLA_GPS, cola);
    }

    function nuevoUuid() {
        if (window.crypto && typeof window.crypto.randomUUID === 'function') {
            return window.crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, caracter => {
            const aleatorio = Math.random() * 16 | 0;
            return (caracter === 'x' ? aleatorio : (aleatorio & 0x3 | 0x8)).toString(16);
        });
    }

    function fechaLocalIso(timestamp) {
        const fecha = new Date(timestamp || Date.now());
        const local = new Date(fecha.getTime() - fecha.getTimezoneOffset() * 60000);
        return local.toISOString().slice(0, 23);
    }

    function distanciaMetros(anterior, actual) {
        if (!anterior) return Number.POSITIVE_INFINITY;
        const radianes = grados => grados * Math.PI / 180;
        const diferenciaLatitud = radianes(actual.latitud - anterior.latitud);
        const diferenciaLongitud = radianes(actual.longitud - anterior.longitud);
        const latitud1 = radianes(anterior.latitud);
        const latitud2 = radianes(actual.latitud);
        const a = Math.sin(diferenciaLatitud / 2) ** 2
                + Math.cos(latitud1) * Math.cos(latitud2)
                * Math.sin(diferenciaLongitud / 2) ** 2;
        return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    function cantidadPendiente(codigoRecorrido) {
        return colaGps().filter(punto => String(punto.codigoRecorrido)
                    === String(codigoRecorrido)).length;
    }

    function refrescarSeguimientoGps(mensaje) {
        const cola = colaGps();
        const pendientes = seguimientoGps
                ? cola.filter(punto => String(punto.codigoRecorrido)
                    === String(seguimientoGps.codigoRecorrido)).length
                : cola.length;
        $('#seguimientoGpsPendientes').text(pendientes + ' pendiente' + (pendientes === 1 ? '' : 's'));
        $('#seguimientoGpsConexion')
                .toggleClass('bg-success', navigator.onLine)
                .toggleClass('bg-secondary', !navigator.onLine)
                .text(navigator.onLine ? 'En línea' : 'Sin conexión');
        if (!seguimientoGps && !cola.length) {
            $('#seguimientoGpsPanel').addClass('d-none');
            return;
        }
        $('#seguimientoGpsPanel').removeClass('d-none');
        if (seguimientoGps) {
            $('#seguimientoGpsTitulo').text('GPS activo · Recorrido #'
                    + seguimientoGps.codigoRecorrido);
            $('#seguimientoGpsDetalle').text(seguimientoGps.cobrador || 'Capturando recorrido');
            $('#detenerSeguimientoGps').removeClass('d-none');
        } else {
            $('#seguimientoGpsTitulo').text('Puntos pendientes de sincronización');
            $('#seguimientoGpsDetalle').text('Se enviarán automáticamente al recuperar la conexión.');
            $('#detenerSeguimientoGps').addClass('d-none');
        }
        if (mensaje) $('#seguimientoGpsUltimo').text(mensaje);
    }

    function detenerVigilanciaGps() {
        if (idVigilanciaGps !== null && navigator.geolocation) {
            navigator.geolocation.clearWatch(idVigilanciaGps);
        }
        idVigilanciaGps = null;
    }

    function detenerSeguimientoGps(mensaje = 'Seguimiento detenido en este dispositivo.') {
        detenerVigilanciaGps();
        seguimientoGps = null;
        localStorage.removeItem(CLAVE_SEGUIMIENTO_GPS);
        ultimaCapturaGps = null;
        refrescarSeguimientoGps(mensaje);
    }

    function capturarPosicionGps(posicion) {
        if (!seguimientoGps) return;
        const actual = {
            latitud: Number(posicion.coords.latitude),
            longitud: Number(posicion.coords.longitude),
            timestamp: Number(posicion.timestamp || Date.now())
        };
        const tiempoTranscurrido = ultimaCapturaGps
                ? actual.timestamp - ultimaCapturaGps.timestamp : Number.POSITIVE_INFINITY;
        if (tiempoTranscurrido < INTERVALO_MINIMO_MS
                && distanciaMetros(ultimaCapturaGps, actual) < DISTANCIA_MINIMA_METROS) return;

        const cola = colaGps();
        if (cola.length >= 5000) {
            alerta('La cola GPS alcanzó su límite. Conecte el dispositivo a Internet antes de continuar.');
            detenerVigilanciaGps();
            return;
        }
        cola.push({
            codigoRecorrido: seguimientoGps.codigoRecorrido,
            idSincronizacion: nuevoUuid(),
            latitud: actual.latitud,
            longitud: actual.longitud,
            precisionMetros: Number.isFinite(posicion.coords.accuracy)
                    ? Number(posicion.coords.accuracy.toFixed(2)) : null,
            velocidadMetrosSegundo: Number.isFinite(posicion.coords.speed)
                    && posicion.coords.speed >= 0
                    ? Number(posicion.coords.speed.toFixed(2)) : null,
            fechaDispositivo: fechaLocalIso(actual.timestamp)
        });
        guardarColaGps(cola);
        ultimaCapturaGps = actual;
        refrescarSeguimientoGps('Última posición: '
                + actual.latitud.toFixed(7) + ', ' + actual.longitud.toFixed(7)
                + ' · ' + new Date(actual.timestamp).toLocaleTimeString('es-PY'));
        sincronizarColaGps();
    }

    function errorPosicionGps(error) {
        const mensajes = {
            1: 'Permiso de ubicación denegado. Habilítelo para registrar el recorrido.',
            2: 'El dispositivo no pudo determinar la ubicación.',
            3: 'Se agotó el tiempo para obtener la ubicación.'
        };
        alerta(mensajes[error.code] || 'No se pudo obtener la ubicación GPS.');
        refrescarSeguimientoGps(mensajes[error.code]);
    }

    function activarVigilanciaGps() {
        detenerVigilanciaGps();
        if (!navigator.geolocation) {
            alerta('Este dispositivo no dispone de geolocalización.');
            detenerSeguimientoGps('Geolocalización no disponible.');
            return;
        }
        idVigilanciaGps = navigator.geolocation.watchPosition(
                capturarPosicionGps, errorPosicionGps,
                {enableHighAccuracy: true, timeout: 30000, maximumAge: 5000});
    }

    function iniciarSeguimientoGps(codigoRecorrido, cobrador) {
        if (!window.isSecureContext && location.hostname !== 'localhost') {
            alerta('El GPS requiere que el sistema se abra mediante HTTPS.');
            return;
        }
        if (seguimientoGps && String(seguimientoGps.codigoRecorrido) !== String(codigoRecorrido)
                && !window.confirm('Ya existe otro seguimiento activo en este dispositivo. ¿Desea reemplazarlo?')) {
            return;
        }
        seguimientoGps = {codigoRecorrido: codigoRecorrido, cobrador: cobrador || '',
            fechaInicio: new Date().toISOString()};
        guardarLocal(CLAVE_SEGUIMIENTO_GPS, seguimientoGps);
        refrescarSeguimientoGps('Solicitando la primera posición…');
        activarVigilanciaGps();
    }

    async function sincronizarColaGps() {
        if (sincronizandoGps || !navigator.onLine) {
            refrescarSeguimientoGps();
            return;
        }
        sincronizandoGps = true;
        try {
            let cola = colaGps();
            while (cola.length && navigator.onLine) {
                const punto = cola[0];
                try {
                    await api('/recorridos/' + encodeURIComponent(punto.codigoRecorrido) + '/puntos', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token,
                            'X-Origen-Cliente': 'APP'},
                        body: JSON.stringify({
                            idSincronizacion: punto.idSincronizacion,
                            latitud: punto.latitud,
                            longitud: punto.longitud,
                            precisionMetros: punto.precisionMetros,
                            velocidadMetrosSegundo: punto.velocidadMetrosSegundo,
                            fechaDispositivo: punto.fechaDispositivo
                        })
                    });
                    cola.shift();
                    guardarColaGps(cola);
                } catch (error) {
                    refrescarSeguimientoGps('Sincronización pendiente: ' + error.message);
                    break;
                }
            }
            if (!cola.length) refrescarSeguimientoGps('Todos los puntos están sincronizados.');
        } finally {
            sincronizandoGps = false;
        }
    }

    function alerta(mensaje, tipo = 'danger') {
        $('#recorridoAlerta').removeClass('d-none alert-danger alert-success alert-info')
                .addClass('alert-' + tipo).text(mensaje);
    }

    function fechaHora(valor) {
        if (!valor) return '—';
        const fecha = new Date(valor);
        return Number.isNaN(fecha.getTime()) ? String(valor) : fecha.toLocaleString('es-PY');
    }

    async function api(url, opciones = {}) {
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

    function badgeEstado(estado) {
        const clase = estado === 'ACTIVO' ? 'bg-success'
                : (estado === 'PENDIENTE' ? 'bg-warning text-dark' : 'bg-secondary');
        return '<span class="badge ' + clase + '">' + escapar(estado) + '</span>';
    }

    function acciones(item) {
        let html = '<button class="btn btn-outline-primary btn-sm ver-recorrido" data-id="'
                + escapar(item.codigoRecorrido) + '" data-cobrador="' + escapar(item.cobrador)
                + '" title="Ver recorrido"><i class="fa-solid fa-map-location-dot"></i> Ver</button> ';
        if (item.estado === 'PENDIENTE') {
            html += '<button class="btn btn-success btn-sm iniciar-recorrido" data-id="'
                    + escapar(item.codigoRecorrido) + '"><i class="fa-solid fa-play"></i> Iniciar</button>';
        } else if (item.estado === 'ACTIVO') {
            html += '<button class="btn btn-danger btn-sm finalizar-recorrido" data-id="'
                    + escapar(item.codigoRecorrido) + '"><i class="fa-solid fa-stop"></i> Finalizar</button>';
        }
        return html;
    }

    function pintarRecorridos(datos) {
        $('#recorridosCuerpo').html(datos.length ? datos.map(item => '<tr>'
                + '<td><strong>#' + escapar(item.codigoRecorrido) + '</strong></td>'
                + '<td>' + escapar(item.cobrador) + (item.observacion
                    ? '<small class="d-block text-muted">' + escapar(item.observacion) + '</small>' : '') + '</td>'
                + '<td>' + badgeEstado(item.estado) + '</td>'
                + '<td>' + escapar(fechaHora(item.fechaInicio)) + '</td>'
                + '<td>' + escapar(fechaHora(item.fechaFin)) + '</td>'
                + '<td><span class="badge bg-info text-dark">' + escapar(item.cantidadPuntos) + '</span>'
                + (item.ultimaFechaPunto ? '<small class="d-block text-muted">' + escapar(fechaHora(item.ultimaFechaPunto)) + '</small>' : '') + '</td>'
                + '<td>' + escapar(item.usuarioRegistro) + '<small class="d-block text-muted">' + escapar(item.origenRegistro) + '</small></td>'
                + '<td class="text-end text-nowrap">' + acciones(item) + '</td></tr>').join('')
                : '<tr><td colspan="8" class="text-center text-muted py-5">No existen recorridos registrados</td></tr>');
    }

    async function cargarRecorridos() {
        $('#recorridosCuerpo').html('<tr><td colspan="8" class="text-center py-5"><span class="spinner-border spinner-border-sm"></span> Cargando…</td></tr>');
        try {
            pintarRecorridos(await api('/recorridos/lista'));
        } catch (error) {
            alerta(error.message);
            $('#recorridosCuerpo').html('<tr><td colspan="8" class="text-center text-danger py-4">No se pudieron cargar los recorridos</td></tr>');
        }
    }

    async function cargarDispositivos() {
        const cuerpo = $('#dispositivosCuerpo').html('<tr><td colspan="6" class="text-center py-4">Cargando dispositivos…</td></tr>');
        try {
            const dispositivos = await api('/recorridos/dispositivos');
            const opciones = $('#recorridoCobrador option').filter('[value]').map(function () {
                return '<option value="' + escapar($(this).val()) + '">' + escapar($(this).text()) + '</option>';
            }).get().join('');
            cuerpo.html(dispositivos.length ? dispositivos.map(d => {
                const activo = d.activo === true || Number(d.activo) === 1;
                const estado = activo
                        ? '<span class="badge bg-success">Activo</span>'
                        : '<span class="badge bg-secondary">Inactivo</span>';
                const accionEstado = activo
                        ? '<button class="btn btn-outline-danger btn-sm cambiar-estado-dispositivo" data-activo="false" data-id="'
                            + escapar(d.idDispositivo) + '"><i class="fa-solid fa-ban"></i> Desactivar</button>'
                        : '<button class="btn btn-outline-success btn-sm cambiar-estado-dispositivo" data-activo="true" data-id="'
                            + escapar(d.idDispositivo) + '"><i class="fa-solid fa-power-off"></i> Activar</button>';
                return '<tr class="' + (activo ? '' : 'table-secondary') + '"><td><strong>'
                        + escapar(d.nombreDispositivo) + '</strong></td><td><code>'
                        + escapar(d.idDispositivo) + '</code></td><td>' + estado
                        + '</td><td><select class="form-select form-select-sm dispositivo-cobrador" data-id="'
                        + escapar(d.idDispositivo) + '" ' + (activo ? '' : 'disabled')
                        + '><option value="">Sin asignar</option>' + opciones + '</select></td><td>'
                        + escapar(fechaHora(d.ultimaConexion)) + '</td><td class="text-nowrap">'
                        + '<button class="btn btn-primario btn-sm guardar-dispositivo" data-id="'
                        + escapar(d.idDispositivo) + '" ' + (activo ? '' : 'disabled')
                        + '><i class="fa-solid fa-link"></i> Vincular</button> ' + accionEstado + '</td></tr>';
            }).join('') : '<tr><td colspan="6" class="text-center text-muted py-4">Abra Configuración en la APK para registrar el teléfono.</td></tr>');
            dispositivos.forEach(d => $('.dispositivo-cobrador[data-id="' + d.idDispositivo + '"]').val(d.codigoCobrador || ''));
        } catch (error) { cuerpo.html('<tr><td colspan="6" class="text-danger text-center py-4">' + escapar(error.message) + '</td></tr>'); }
    }
    $(document).on('click','.guardar-dispositivo',async function(){const id=$(this).data('id');const value=$('.dispositivo-cobrador[data-id="'+id+'"]').val();try{await api('/recorridos/dispositivos/'+encodeURIComponent(id)+'/asignar',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token},body:JSON.stringify({codigoCobrador:value?Number(value):null})});alerta('Teléfono vinculado correctamente.','success');cargarDispositivos();}catch(error){alerta(error.message);}});
    $(document).on('click', '.cambiar-estado-dispositivo', async function () {
        const boton = $(this);
        const id = boton.data('id');
        const activar = String(boton.data('activo')) === 'true';
        if (!activar && !window.confirm('¿Desactivar este dispositivo? La aplicación dejará de consultar y enviar ubicaciones hasta que ROOT lo active nuevamente.')) return;
        boton.prop('disabled', true);
        try {
            await api('/recorridos/dispositivos/' + encodeURIComponent(id) + '/estado', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token},
                body: JSON.stringify({activo: activar})
            });
            alerta(activar ? 'Dispositivo activado correctamente.' : 'Dispositivo desactivado correctamente.', 'success');
            await cargarDispositivos();
        } catch (error) {
            alerta(error.message);
            boton.prop('disabled', false);
        }
    });
    $('#actualizarDispositivos').on('click',cargarDispositivos);

    $('#formNuevoRecorrido').on('submit', async function (event) {
        event.preventDefault();
        const boton = $('#crearRecorrido');
        const contenido = boton.html();
        boton.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span> Registrando…');
        try {
            await api('/recorridos', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token},
                body: JSON.stringify({
                    codigoCobrador: Number($('#recorridoCobrador').val()),
                    observacion: $('#recorridoObservacion').val()
                })
            });
            $('#recorridoObservacion').val('');
            alerta('Recorrido registrado. Ya puede iniciarlo.', 'success');
            await cargarRecorridos();
        } catch (error) {
            alerta(error.message);
        } finally {
            boton.prop('disabled', false).html(contenido);
        }
    });

    async function cambiarEstado(codigo, accion) {
        const texto = accion === 'iniciar' ? 'iniciar' : 'finalizar';
        if (!window.confirm('¿Desea ' + texto + ' este recorrido?')) return;
        try {
            if (accion === 'finalizar') {
                await sincronizarColaGps();
                const pendientes = cantidadPendiente(codigo);
                if (pendientes > 0) {
                    alerta('No se puede finalizar: quedan ' + pendientes
                            + ' punto(s) pendientes de sincronización.');
                    return;
                }
            }
            const actualizado = await api('/recorridos/' + encodeURIComponent(codigo) + '/' + accion, {
                method: 'POST', headers: {'X-CSRF-TOKEN': token}
            });
            alerta('Recorrido ' + (accion === 'iniciar' ? 'iniciado' : 'finalizado') + ' correctamente.', 'success');
            await cargarRecorridos();
        } catch (error) {
            alerta(error.message);
        }
    }

    $(document).on('click', '.iniciar-recorrido', function () {
        cambiarEstado($(this).data('id'), 'iniciar');
    });
    $(document).on('click', '.finalizar-recorrido', function () {
        cambiarEstado($(this).data('id'), 'finalizar');
    });
    $(document).on('click', '.seguir-gps', function () {
        iniciarSeguimientoGps($(this).data('id'), $(this).data('cobrador'));
        cargarRecorridos();
    });
    $('#detenerSeguimientoGps').on('click', function () {
        detenerSeguimientoGps();
        sincronizarColaGps();
        cargarRecorridos();
    });

    function asegurarMapaRecorrido() {
        if (mapaRecorrido) return true;
        if (!window.L) {
            $('#detalleRecorridoAlerta').removeClass('d-none')
                    .text('No se pudo cargar el componente del mapa. Compruebe la conexión a Internet.');
            return false;
        }
        mapaRecorrido = L.map('recorridoMapa', {zoomControl: true, preferCanvas: true})
                .setView([-23.4425, -58.4438], 6);
        L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
            subdomains: 'abcd',
            maxZoom: 20,
            keepBuffer: 4,
            updateWhenIdle: false,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
        }).addTo(mapaRecorrido);
        capaTrazoRecorrido = L.layerGroup().addTo(mapaRecorrido);
        capaPuntosRecorrido = L.layerGroup().addTo(mapaRecorrido);
        capaPermanenciasRecorrido = L.layerGroup().addTo(mapaRecorrido);
        return true;
    }

    function formatoDuracion(segundos) {
        const total = Math.max(0, Math.round(Number(segundos) || 0));
        const horas = Math.floor(total / 3600);
        const minutos = Math.floor((total % 3600) / 60);
        if (horas) return horas + ' h ' + minutos + ' min';
        return Math.max(1, minutos) + ' min';
    }

    function descripcionLugar(item) {
        if (item.cuentaCorriente) {
            return (item.titular || 'Servicio') + ' · Cuenta ' + item.cuentaCorriente;
        }
        return 'Área sin servicio asociado';
    }

    function dibujarMapaRecorrido(puntos, permanencias) {
        ultimosPuntosMapa = Array.isArray(puntos) ? puntos : [];
        ultimasPermanenciasMapa = Array.isArray(permanencias) ? permanencias : [];
        const validos = (puntos || []).filter(p => Number.isFinite(Number(p.latitud))
                    && Number.isFinite(Number(p.longitud)));
        $('#recorridoSinPuntos').toggleClass('d-none', validos.length > 0);
        $('#recorridoCantidadPermanencias').text((permanencias || []).length);
        if (!document.getElementById('detalleRecorridoModal').classList.contains('show')) return;
        if (!asegurarMapaRecorrido()) return;

        capaTrazoRecorrido.clearLayers();
        capaPuntosRecorrido.clearLayers();
        capaPermanenciasRecorrido.clearLayers();
        marcadoresPermanencia = [];
        marcadorPuntoSeleccionado = null;

        let distanciaTotal = 0;
        for (let i = 1; i < validos.length; i++) {
            distanciaTotal += distanciaMetros(validos[i - 1], validos[i]);
        }
        $('#recorridoDistancia').text(distanciaTotal >= 1000
                ? (distanciaTotal / 1000).toFixed(2) + ' km'
                : Math.round(distanciaTotal) + ' m');
        if (validos.length > 1) {
            const inicio = new Date(validos[0].fechaDispositivo).getTime();
            const fin = new Date(validos[validos.length - 1].fechaDispositivo).getTime();
            $('#recorridoDuracion').text(Number.isFinite(inicio) && Number.isFinite(fin)
                    ? formatoDuracion((fin - inicio) / 1000) : '—');
        } else {
            $('#recorridoDuracion').text('—');
        }
        if (!validos.length) {
            mapaRecorrido.setView([-23.4425, -58.4438], 6);
            return;
        }

        const coordenadas = validos.map(p => [Number(p.latitud), Number(p.longitud)]);
        L.polyline(coordenadas, {color: '#fff', weight: 10, opacity: .94,
            lineCap: 'round', lineJoin: 'round'}).addTo(capaTrazoRecorrido);
        L.polyline(coordenadas, {color: '#2563eb', weight: 5, opacity: .85,
            lineCap: 'round', lineJoin: 'round'}).addTo(capaTrazoRecorrido);

        validos.forEach((punto, indice) => {
            L.circleMarker([Number(punto.latitud), Number(punto.longitud)], {
                radius: 5, color: '#fff', weight: 2, opacity: 1,
                fillColor: '#2563eb', fillOpacity: .95
            }).bindPopup('<strong>Punto GPS ' + escapar(indice + 1) + '</strong><br>'
                    + escapar(fechaHora(punto.fechaDispositivo)) + '<br><small>'
                    + escapar(Number(punto.latitud).toFixed(6) + ', '
                        + Number(punto.longitud).toFixed(6)) + '</small>')
                    .addTo(capaPuntosRecorrido);
        });

        const inicio = validos[0];
        const fin = validos[validos.length - 1];
        L.circleMarker([Number(inicio.latitud), Number(inicio.longitud)], {
            radius: 8, color: '#fff', weight: 3, fillColor: '#198754', fillOpacity: 1
        }).bindPopup('<strong>Inicio del recorrido</strong><br>'
                + escapar(fechaHora(inicio.fechaDispositivo))).addTo(capaPuntosRecorrido);
        L.circleMarker([Number(fin.latitud), Number(fin.longitud)], {
            radius: 8, color: '#fff', weight: 3, fillColor: '#dc3545', fillOpacity: 1
        }).bindPopup('<strong>Última posición</strong><br>'
                + escapar(fechaHora(fin.fechaDispositivo))).addTo(capaPuntosRecorrido);

        (permanencias || []).forEach((item, indice) => {
            const lugar = descripcionLugar(item);
            const detalle = item.direccion ? '<br>' + escapar(item.direccion) : '';
            const marcador = L.circleMarker([Number(item.latitud), Number(item.longitud)], {
                radius: 9, color: '#fff', weight: 3, fillColor: '#f59f00', fillOpacity: .95
            }).bindTooltip(escapar(formatoDuracion(item.segundos)), {direction: 'top'})
                    .bindPopup('<strong>' + escapar(lugar) + '</strong>' + detalle
                        + '<hr class="my-2"><span>Llegada: ' + escapar(fechaHora(item.llegada))
                        + '</span><br><span>Salida: ' + escapar(fechaHora(item.salida))
                        + '</span><br><strong>Permanencia: ' + escapar(formatoDuracion(item.segundos))
                        + '</strong>')
                    .addTo(capaPermanenciasRecorrido);
            marcadoresPermanencia[indice] = marcador;
        });

        if (coordenadas.length === 1) {
            mapaRecorrido.setView(coordenadas[0], 17);
        } else {
            mapaRecorrido.fitBounds(L.latLngBounds(coordenadas), {padding: [28, 28], maxZoom: 18});
        }
        window.setTimeout(() => mapaRecorrido.invalidateSize(), 120);
    }

    function pintarPermanencias(permanencias) {
        $('#permanenciasRecorridoCuerpo').html(permanencias.length ? permanencias.map((item, indice) => {
            const lugar = descripcionLugar(item);
            const detalle = item.direccion
                    ? '<small class="d-block text-muted">' + escapar(item.direccion) + '</small>' : '';
            const cercania = item.distanciaServicioMetros !== null
                    ? '<small class="d-block text-muted">A ' + escapar(item.distanciaServicioMetros)
                        + ' m de la ubicación registrada</small>' : '';
            return '<tr><td><strong>' + escapar(lugar) + '</strong>' + detalle + cercania
                    + '<small class="d-block text-muted">' + escapar(Number(item.latitud).toFixed(6)
                        + ', ' + Number(item.longitud).toFixed(6)) + '</small></td><td>'
                    + escapar(fechaHora(item.llegada)) + '</td><td>' + escapar(fechaHora(item.salida))
                    + '</td><td><strong>' + escapar(formatoDuracion(item.segundos)) + '</strong></td><td>'
                    + escapar(item.muestras) + '</td><td class="text-nowrap"><button type="button" data-indice="'
                    + indice + '" class="btn btn-outline-primary btn-sm centrar-permanencia" title="Ver en el mapa">'
                    + '<i class="fa-solid fa-location-crosshairs"></i></button> <a class="btn btn-outline-success btn-sm" '
                    + 'target="_blank" rel="noopener" href="https://www.google.com/maps?q='
                    + encodeURIComponent(item.latitud + ',' + item.longitud)
                    + '" title="Abrir en Google Maps"><i class="fa-solid fa-map-location-dot"></i></a></td></tr>';
        }).join('') : '<tr><td colspan="6" class="text-center text-muted py-3">'
                + 'No se detectaron permanencias de al menos un minuto dentro de un radio de 35 metros.</td></tr>');
    }

    async function actualizarDetalleGeografico() {
        if (recorridoDetalleActual === null) return;
        const boton = $('#actualizarMapaRecorrido');
        boton.prop('disabled', true).find('i').addClass('fa-spin');
        try {
            const codigo = encodeURIComponent(recorridoDetalleActual);
            const [trazo, permanencias] = await Promise.all([
                api('/recorridos/' + codigo + '/trazo'),
                api('/recorridos/' + codigo + '/permanencias')
            ]);
            pintarPermanencias(permanencias);
            dibujarMapaRecorrido(trazo, permanencias);
        } finally {
            boton.prop('disabled', false).find('i').removeClass('fa-spin');
        }
    }

    $(document).on('click', '.ver-recorrido', async function () {
        const codigo = $(this).data('id');
        recorridoDetalleActual = codigo;
        paginaPuntosActual = 0;
        $('#detalleRecorridoTitulo').text('Recorrido #' + codigo + ' · ' + $(this).data('cobrador'));
        $('#detalleRecorridoAlerta').addClass('d-none').text('');
        $('#puntosRecorridoCuerpo').html('<tr><td colspan="7" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span> Cargando…</td></tr>');
        $('#permanenciasRecorridoCuerpo').html('<tr><td colspan="6" class="text-center py-3">Calculando permanencias…</td></tr>');
        ultimosPuntosMapa = [];
        ultimasPermanenciasMapa = [];
        $('#recorridoDistancia').text('0 m');
        $('#recorridoDuracion').text('—');
        $('#recorridoCantidadPermanencias').text('0');
        $('#recorridoSinPuntos').removeClass('d-none');
        if (mapaRecorrido) {
            capaTrazoRecorrido.clearLayers();
            capaPuntosRecorrido.clearLayers();
            capaPermanenciasRecorrido.clearLayers();
        }
        modalDetalle.show();
        try {
            await actualizarDetalleGeografico();
            await cargarPaginaPuntos();
        } catch (error) {
            $('#detalleRecorridoAlerta').removeClass('d-none').text(error.message);
        }
    });

    $(document).on('click', '.centrar-permanencia', function () {
        const indice = Number($(this).data('indice'));
        const marcador = marcadoresPermanencia[indice];
        if (mapaRecorrido && marcador) {
            mapaRecorrido.setView(marcador.getLatLng(), 18, {animate: true});
            marcador.openPopup();
            document.getElementById('recorridoMapa').scrollIntoView({behavior: 'smooth', block: 'center'});
        }
    });
    $(document).on('click', '.centrar-punto-recorrido', function () {
        if (!asegurarMapaRecorrido()) return;
        const latitud = Number($(this).data('latitud'));
        const longitud = Number($(this).data('longitud'));
        if (!Number.isFinite(latitud) || !Number.isFinite(longitud)) return;
        if (marcadorPuntoSeleccionado) capaPuntosRecorrido.removeLayer(marcadorPuntoSeleccionado);
        marcadorPuntoSeleccionado = L.circleMarker([latitud, longitud], {
            radius: 10, color: '#fff', weight: 3, fillColor: '#2563eb', fillOpacity: 1
        }).bindPopup('<strong>Punto GPS seleccionado</strong><br>'
                + escapar(fechaHora($(this).data('fecha'))) + '<br><small>'
                + escapar(latitud.toFixed(6) + ', ' + longitud.toFixed(6)) + '</small>')
                .addTo(capaPuntosRecorrido);
        mapaRecorrido.setView([latitud, longitud], 18, {animate: true});
        marcadorPuntoSeleccionado.openPopup();
        document.getElementById('recorridoMapa').scrollIntoView({behavior: 'smooth', block: 'center'});
    });
    $('#actualizarMapaRecorrido').on('click', async function () {
        try {
            await actualizarDetalleGeografico();
            await cargarPaginaPuntos();
        } catch (error) {
            $('#detalleRecorridoAlerta').removeClass('d-none').text(error.message);
        }
    });
    document.getElementById('detalleRecorridoModal').addEventListener('shown.bs.modal', function () {
        if (!asegurarMapaRecorrido()) return;
        window.setTimeout(() => {
            mapaRecorrido.invalidateSize({pan: false});
            dibujarMapaRecorrido(ultimosPuntosMapa, ultimasPermanenciasMapa);
        }, 80);
    });

    async function cargarPaginaPuntos() {
        if (recorridoDetalleActual === null) return;
        $('#puntosRecorridoCuerpo').html('<tr><td colspan="7" class="text-center py-4">Cargando página…</td></tr>');
        try {
            const respuesta = await api('/recorridos/' + encodeURIComponent(recorridoDetalleActual)
                    + '/puntos-pagina?pagina=' + paginaPuntosActual + '&tamano=100');
            const puntos = respuesta.contenido || [];
            $('#puntosRecorridoCuerpo').html(puntos.length ? puntos.map(punto => '<tr>'
                    + '<td>' + escapar(fechaHora(punto.fechaDispositivo)) + '</td>'
                    + '<td>' + escapar(punto.latitud) + '</td><td>' + escapar(punto.longitud) + '</td>'
                    + '<td>' + escapar(punto.precisionMetros === null ? '—' : '± ' + punto.precisionMetros + ' m') + '</td>'
                    + '<td>' + escapar(punto.velocidadMetrosSegundo === null ? '—' : punto.velocidadMetrosSegundo + ' m/s') + '</td><td><code>' + escapar(punto.idDispositivo || '—') + '</code></td>'
                    + '<td class="text-nowrap"><button type="button" class="btn btn-outline-primary btn-sm centrar-punto-recorrido" '
                    + 'data-latitud="' + escapar(punto.latitud) + '" data-longitud="' + escapar(punto.longitud)
                    + '" data-fecha="' + escapar(punto.fechaDispositivo) + '" title="Ver en el mapa del recorrido">'
                    + '<i class="fa-solid fa-location-crosshairs"></i></button> <a class="btn btn-outline-success btn-sm" '
                    + 'target="_blank" rel="noopener" href="https://www.google.com/maps?q='
                    + encodeURIComponent(punto.latitud + ',' + punto.longitud)
                    + '" title="Abrir en Google Maps"><i class="fa-solid fa-map-location-dot"></i></a></td></tr>').join('')
                    : '<tr><td colspan="7" class="text-center text-muted py-4">Todavía no se recibieron puntos GPS</td></tr>');
            const total = Number(respuesta.totalPaginas || 0);
            $('#puntosPaginaEstado').text('Página ' + (total ? paginaPuntosActual + 1 : 0) + ' de ' + total
                    + ' · ' + Number(respuesta.totalElementos || 0) + ' posiciones');
            $('#puntosPaginaAnterior').prop('disabled', paginaPuntosActual <= 0);
            $('#puntosPaginaSiguiente').prop('disabled', paginaPuntosActual + 1 >= total);
        } catch (error) {
            $('#detalleRecorridoAlerta').removeClass('d-none').text(error.message);
        }
    }
    $('#puntosPaginaAnterior').on('click',function(){if(paginaPuntosActual>0){paginaPuntosActual--;cargarPaginaPuntos();}});
    $('#puntosPaginaSiguiente').on('click',function(){paginaPuntosActual++;cargarPaginaPuntos();});

    $('#actualizarRecorridos').on('click', cargarRecorridos);
    window.addEventListener('online', sincronizarColaGps);
    window.addEventListener('offline', () => refrescarSeguimientoGps('Sin conexión; los puntos quedan guardados en el dispositivo.'));
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) sincronizarColaGps();
    });
    setInterval(sincronizarColaGps, 15000);
    refrescarSeguimientoGps();
    if (seguimientoGps) detenerSeguimientoGps('La ubicación ahora se obtiene del teléfono vinculado al cobrador.');
    sincronizarColaGps();
    cargarRecorridos();
    cargarDispositivos();
});
