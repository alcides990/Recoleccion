package ama.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecorridoCobradorService {

    private final JdbcTemplate jdbcTemplate;
    private static final long PERMANENCIA_MINIMA_SEGUNDOS = 20L * 60L;

    @Transactional
    public Map<String,Object> registrarDispositivo(String id,String nombre,Integer sucursal){validarIdDispositivo(id);jdbcTemplate.update("""
            INSERT INTO dispositivos_cobrador(id_dispositivo,codigo_sucursal,nombre_dispositivo,ultima_conexion,activo)
            VALUES(?,?,?,CURRENT_TIMESTAMP,1)
            ON DUPLICATE KEY UPDATE nombre_dispositivo=VALUES(nombre_dispositivo),ultima_conexion=CURRENT_TIMESTAMP
            """,id,sucursal,nombre==null||nombre.isBlank()?"Teléfono Android":nombre.trim());return estadoDispositivo(id,sucursal);}

    @Transactional(readOnly=true)
    public List<Map<String,Object>> listarDispositivos(Integer sucursal){return jdbcTemplate.queryForList("""
            SELECT d.id_dispositivo AS idDispositivo,d.nombre_dispositivo AS nombreDispositivo,
                   d.codigo_cobrador AS codigoCobrador,CONCAT(COALESCE(c.nombre,''),' ',COALESCE(c.apellido,'')) AS cobrador,
                   d.ultima_conexion AS ultimaConexion,d.activo
            FROM dispositivos_cobrador d LEFT JOIN cobradores c ON c.codigo_cobrador=d.codigo_cobrador
            WHERE d.codigo_sucursal=? ORDER BY d.ultima_conexion DESC
            """,sucursal);}

    @Transactional
    public Map<String, Object> asignarDispositivo(String idDispositivo,
            Integer codigoCobrador, Integer codigoSucursal) {
        validarIdDispositivo(idDispositivo);
        if (codigoCobrador != null) {
            validarCobradorActivo(codigoCobrador, codigoSucursal);
        }

        int modificados = jdbcTemplate.update("""
                UPDATE dispositivos_cobrador
                SET codigo_cobrador = ?
                WHERE id_dispositivo = ? AND codigo_sucursal = ? AND activo = 1
                """, codigoCobrador, idDispositivo, codigoSucursal);
        if (modificados == 0) {
            throw new IllegalArgumentException(
                    "El dispositivo no existe o está desactivado");
        }
        return estadoDispositivo(idDispositivo, codigoSucursal);
    }

    private void validarCobradorActivo(Integer codigoCobrador, Integer codigoSucursal) {
        Integer coincidencias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM cobradores
                WHERE codigo_cobrador = ? AND codigo_sucursal = ? AND codigo_estado = 1
                """, Integer.class, codigoCobrador, codigoSucursal);
        if (coincidencias == null || coincidencias == 0) {
            throw new IllegalArgumentException(
                    "El cobrador no existe o no está activo");
        }
    }

    @Transactional
    public Map<String, Object> cambiarEstadoDispositivo(String id, boolean activo,
            Integer sucursal) {
        validarIdDispositivo(id);
        int modificados = jdbcTemplate.update("""
                UPDATE dispositivos_cobrador
                SET activo = ?
                WHERE id_dispositivo = ? AND codigo_sucursal = ?
                """, activo, id, sucursal);
        if (modificados == 0) {
            throw new IllegalArgumentException("Dispositivo no encontrado");
        }
        return consultarDispositivo(id, sucursal);
    }

    @Transactional
    public Map<String, Object> recorridoActivoDispositivo(String idDispositivo,
            Integer codigoSucursal) {
        Map<String, Object> dispositivo = estadoDispositivo(
                idDispositivo, codigoSucursal);
        jdbcTemplate.update("""
                UPDATE dispositivos_cobrador
                SET ultima_conexion = CURRENT_TIMESTAMP
                WHERE id_dispositivo = ?
                """, idDispositivo);

        Object codigoCobrador = dispositivo.get("codigoCobrador");
        if (codigoCobrador == null) {
            return Map.of("activo", false, "vinculado", false);
        }

        List<Map<String, Object>> recorridos = jdbcTemplate.queryForList("""
                SELECT r.codigo_recorrido AS codigoRecorrido,
                       CONCAT(c.nombre, ' ', COALESCE(c.apellido, '')) AS cobrador
                FROM recorridos_cobrador r
                JOIN cobradores c ON c.codigo_cobrador = r.codigo_cobrador
                WHERE r.codigo_cobrador = ? AND r.estado = 'ACTIVO'
                ORDER BY r.fecha_inicio DESC
                LIMIT 1
                """, codigoCobrador);
        if (recorridos.isEmpty()) {
            return Map.of("activo", false, "vinculado", true);
        }

        Map<String, Object> resultado = new LinkedHashMap<>(recorridos.get(0));
        resultado.put("activo", true);
        resultado.put("vinculado", true);
        return resultado;
    }

    @Transactional
    public Map<String,Object> estadoPermanenciasDispositivo(String id,Integer sucursal){
        Map<String,Object> device=consultarDispositivo(id,sucursal);
        jdbcTemplate.update("UPDATE dispositivos_cobrador SET ultima_conexion=CURRENT_TIMESTAMP WHERE id_dispositivo=?",id);
        boolean activo=estaActivo(device.get("activo"));Object cobrador=device.get("codigoCobrador");
        Map<String,Object> estado=new LinkedHashMap<>();estado.put("dispositivoActivo",activo);
        estado.put("vinculado",cobrador!=null);estado.put("codigoCobrador",cobrador);
        estado.put("habilitado",activo&&cobrador!=null);return estado;
    }

    @Transactional
    public Map<String,Object> registrarPermanenciaDispositivo(String id,String sync,
            BigDecimal lat,BigDecimal lon,LocalDateTime llegada,LocalDateTime salida,
            Integer sucursal,Integer usuarioSistema){
        validarIdDispositivo(id);
        Map<String,Object> device=estadoDispositivo(id,sucursal);Object cobrador=device.get("codigoCobrador");
        if(cobrador==null)throw new IllegalStateException("El dispositivo no está asociado a un cobrador");
        if(lat==null||lon==null||lat.compareTo(BigDecimal.valueOf(-90))<0||lat.compareTo(BigDecimal.valueOf(90))>0
                ||lon.compareTo(BigDecimal.valueOf(-180))<0||lon.compareTo(BigDecimal.valueOf(180))>0)
            throw new IllegalArgumentException("Coordenadas inválidas");
        if(llegada==null||salida==null||salida.isBefore(llegada))throw new IllegalArgumentException("Fechas de permanencia inválidas");
        long segundos=Duration.between(llegada,salida).getSeconds();
        if(segundos<PERMANENCIA_MINIMA_SEGUNDOS)throw new IllegalArgumentException("La permanencia debe ser de al menos 20 minutos");
        String identificador=sync==null?"":sync.trim();
        try{identificador=UUID.fromString(identificador).toString();}catch(Exception e){throw new IllegalArgumentException("Identificador de sincronización inválido");}
        Integer codigoCobrador=((Number)cobrador).intValue();String observacion="PERMANENCIA AUTOMÁTICA "+identificador;
        List<Long> existentes=jdbcTemplate.queryForList("SELECT codigo_recorrido FROM recorridos_cobrador WHERE codigo_cobrador=? AND observacion=? ORDER BY codigo_recorrido DESC LIMIT 1",Long.class,codigoCobrador,observacion);
        Long recorrido;
        if(existentes.isEmpty()){
            jdbcTemplate.update("""
                    INSERT INTO recorridos_cobrador
                        (codigo_cobrador,estado,observacion,fecha_inicio,fecha_fin,
                         codigo_usuario_registro,codigo_usuario_inicio,codigo_usuario_fin,
                         origen_registro,origen_inicio,origen_fin)
                    VALUES (?,'FINALIZADO',?,?,?,?,?,?,'APP','APP','APP')
                    """,codigoCobrador,observacion,llegada,salida,usuarioSistema,usuarioSistema,usuarioSistema);
            recorrido=jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        }else recorrido=existentes.get(0);
        String llegadaId=UUID.nameUUIDFromBytes((identificador+":llegada").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        String salidaId=UUID.nameUUIDFromBytes((identificador+":salida").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        guardarPuntoPermanencia(llegadaId,recorrido,id,lat,lon,llegada);
        guardarPuntoPermanencia(salidaId,recorrido,id,lat,lon,salida);
        jdbcTemplate.update("UPDATE recorridos_cobrador SET fecha_inicio=LEAST(fecha_inicio,?),fecha_fin=GREATEST(fecha_fin,?) WHERE codigo_recorrido=?",llegada,salida,recorrido);
        return Map.of("registrado",true,"codigoRecorrido",recorrido,"duracionSegundos",segundos);
    }

    private void guardarPuntoPermanencia(String sync,Long recorrido,String dispositivo,BigDecimal lat,BigDecimal lon,LocalDateTime fecha){
        jdbcTemplate.update("""
                INSERT INTO puntos_recorrido_cobrador
                    (id_sincronizacion,codigo_recorrido,id_dispositivo,latitud,longitud,
                     precision_metros,velocidad_metros_segundo,fecha_dispositivo,origen)
                VALUES (?,?,?,?,?,NULL,NULL,?,'APP')
                ON DUPLICATE KEY UPDATE fecha_dispositivo=VALUES(fecha_dispositivo),latitud=VALUES(latitud),longitud=VALUES(longitud)
                """,sync,recorrido,dispositivo,lat,lon,fecha);
    }

    @Transactional
    public boolean registrarPuntoDispositivo(String idDispositivo, Long codigoRecorrido,
            String idSincronizacion, BigDecimal latitud, BigDecimal longitud,
            BigDecimal precision, BigDecimal velocidad, BigDecimal rumbo,
            LocalDateTime fecha, Integer codigoSucursal) {
        Map<String, Object> dispositivo = estadoDispositivo(
                idDispositivo, codigoSucursal);
        Object codigoCobrador = dispositivo.get("codigoCobrador");
        if (codigoCobrador == null) {
            throw new IllegalStateException(
                    "El dispositivo no está vinculado a un cobrador");
        }

        Integer recorridosValidos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM recorridos_cobrador
                WHERE codigo_recorrido = ? AND codigo_cobrador = ? AND estado = 'ACTIVO'
                """, Integer.class, codigoRecorrido, codigoCobrador);
        if (recorridosValidos == null || recorridosValidos == 0) {
            throw new IllegalStateException(
                    "El recorrido ya no está activo para este dispositivo");
        }

        boolean registrado = registrarPunto(codigoRecorrido, idSincronizacion,
                latitud, longitud, precision, velocidad, rumbo, fecha,
                codigoSucursal, "APP");
        if (registrado) {
            jdbcTemplate.update("""
                    UPDATE puntos_recorrido_cobrador
                    SET id_dispositivo = ?
                    WHERE id_sincronizacion = ?
                    """, idDispositivo, idSincronizacion);
        }
        return registrado;
    }

    @Transactional
    public Map<String, Object> registrarPuntosDispositivo(String idDispositivo,
            Long codigoRecorrido,
            List<ama.controladorMVC.SeguimientoMovilController.Punto> puntos,
            Integer codigoSucursal) {
        if (puntos == null || puntos.isEmpty()) {
            throw new IllegalArgumentException("No se recibieron puntos GPS");
        }
        if (puntos.size() > 100) {
            throw new IllegalArgumentException(
                    "El lote no puede superar 100 puntos GPS");
        }

        int registrados = 0;
        int duplicados = 0;
        for (ama.controladorMVC.SeguimientoMovilController.Punto punto : puntos) {
            boolean registrado = registrarPuntoDispositivo(
                    idDispositivo,
                    codigoRecorrido,
                    punto.idSincronizacion(),
                    punto.latitud(),
                    punto.longitud(),
                    punto.precisionMetros(),
                    punto.velocidadMetrosSegundo(),
                    punto.rumboGrados(),
                    punto.fechaDispositivo(),
                    codigoSucursal);
            if (registrado) {
                registrados++;
            } else {
                duplicados++;
            }
        }
        return Map.of(
                "registrado", registrados > 0,
                "recibidos", puntos.size(),
                "registrados", registrados,
                "duplicados", duplicados);
    }

    private Map<String, Object> estadoDispositivo(String idDispositivo,
            Integer codigoSucursal) {
        Map<String, Object> dispositivo = consultarDispositivo(
                idDispositivo, codigoSucursal);
        if (!estaActivo(dispositivo.get("activo"))) {
            throw new IllegalStateException(
                    "El dispositivo está desactivado por ROOT");
        }
        return dispositivo;
    }

    private boolean estaActivo(Object valor) {
        if (Boolean.TRUE.equals(valor)) {
            return true;
        }
        return valor instanceof Number
                && ((Number) valor).intValue() == 1;
    }

    private Map<String, Object> consultarDispositivo(String idDispositivo,
            Integer codigoSucursal) {
        validarIdDispositivo(idDispositivo);
        List<Map<String, Object>> dispositivos = jdbcTemplate.queryForList("""
                SELECT id_dispositivo AS idDispositivo,
                       codigo_cobrador AS codigoCobrador,
                       nombre_dispositivo AS nombreDispositivo,
                       activo
                FROM dispositivos_cobrador
                WHERE id_dispositivo = ? AND codigo_sucursal = ?
                """, idDispositivo, codigoSucursal);
        if (dispositivos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dispositivo no registrado en esta sucursal");
        }
        return dispositivos.get(0);
    }

    private void validarIdDispositivo(String idDispositivo) {
        if (idDispositivo == null
                || !idDispositivo.matches("[A-Za-z0-9._-]{8,64}")) {
            throw new IllegalArgumentException(
                    "Identificador de dispositivo inválido");
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Integer codigoSucursal) {
        return jdbcTemplate.queryForList("""
                SELECT r.codigo_recorrido AS codigoRecorrido,
                       r.codigo_cobrador AS codigoCobrador,
                       CONCAT(c.nombre, ' ', COALESCE(c.apellido, '')) AS cobrador,
                       r.estado, r.observacion,
                       r.fecha_registro AS fechaRegistro,
                       r.fecha_inicio AS fechaInicio,
                       r.fecha_fin AS fechaFin,
                       ur.usuario AS usuarioRegistro,
                       ui.usuario AS usuarioInicio,
                       uf.usuario AS usuarioFin,
                       r.origen_registro AS origenRegistro,
                       r.origen_inicio AS origenInicio,
                       r.origen_fin AS origenFin,
                       (SELECT COUNT(*) FROM puntos_recorrido_cobrador p
                         WHERE p.codigo_recorrido = r.codigo_recorrido) AS cantidadPuntos,
                       (SELECT p.latitud FROM puntos_recorrido_cobrador p
                         WHERE p.codigo_recorrido = r.codigo_recorrido
                         ORDER BY p.fecha_dispositivo DESC, p.codigo_punto DESC LIMIT 1) AS ultimaLatitud,
                       (SELECT p.longitud FROM puntos_recorrido_cobrador p
                         WHERE p.codigo_recorrido = r.codigo_recorrido
                         ORDER BY p.fecha_dispositivo DESC, p.codigo_punto DESC LIMIT 1) AS ultimaLongitud,
                       (SELECT p.fecha_dispositivo FROM puntos_recorrido_cobrador p
                         WHERE p.codigo_recorrido = r.codigo_recorrido
                         ORDER BY p.fecha_dispositivo DESC, p.codigo_punto DESC LIMIT 1) AS ultimaFechaPunto
                FROM recorridos_cobrador r
                JOIN cobradores c ON c.codigo_cobrador = r.codigo_cobrador
                JOIN usuarios_sistema ur
                  ON ur.codigo_usuario_sistema = r.codigo_usuario_registro
                LEFT JOIN usuarios_sistema ui
                  ON ui.codigo_usuario_sistema = r.codigo_usuario_inicio
                LEFT JOIN usuarios_sistema uf
                  ON uf.codigo_usuario_sistema = r.codigo_usuario_fin
                WHERE c.codigo_sucursal = ?
                ORDER BY CASE r.estado WHEN 'ACTIVO' THEN 1 WHEN 'PENDIENTE' THEN 2 ELSE 3 END,
                         COALESCE(r.fecha_inicio, r.fecha_registro) DESC,
                         r.codigo_recorrido DESC
                LIMIT 200
                """, codigoSucursal);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPuntos(Long codigoRecorrido,
            Integer codigoSucursal) {
        verificarRecorrido(codigoRecorrido, codigoSucursal, false);
        return jdbcTemplate.queryForList("""
                SELECT codigo_punto AS codigoPunto, id_dispositivo AS idDispositivo,
                       latitud, longitud,
                       precision_metros AS precisionMetros,
                       velocidad_metros_segundo AS velocidadMetrosSegundo,
                       rumbo_grados AS rumboGrados,
                       fecha_dispositivo AS fechaDispositivo,
                       fecha_recepcion AS fechaRecepcion,
                       origen
                FROM puntos_recorrido_cobrador
                WHERE codigo_recorrido = ?
                ORDER BY fecha_dispositivo, codigo_punto
                """, codigoRecorrido);
    }

    @Transactional(readOnly = true)
    public Map<String,Object> listarPuntosPagina(Long recorrido,Integer sucursal,int pagina,int tamano){
        verificarRecorrido(recorrido,sucursal,false);int size=Math.max(10,Math.min(tamano,200));int page=Math.max(0,pagina);Long total=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM puntos_recorrido_cobrador WHERE codigo_recorrido=?",Long.class,recorrido);long count=total==null?0:total;List<Map<String,Object>> content=jdbcTemplate.queryForList("""
                SELECT codigo_punto AS codigoPunto,id_dispositivo AS idDispositivo,latitud,longitud,
                       precision_metros AS precisionMetros,velocidad_metros_segundo AS velocidadMetrosSegundo,
                       rumbo_grados AS rumboGrados,
                       fecha_dispositivo AS fechaDispositivo,fecha_recepcion AS fechaRecepcion,origen
                FROM puntos_recorrido_cobrador WHERE codigo_recorrido=?
                ORDER BY fecha_dispositivo DESC,codigo_punto DESC LIMIT ? OFFSET ?
                """,recorrido,size,page*size);Map<String,Object> result=new java.util.LinkedHashMap<>();result.put("contenido",content);result.put("pagina",page);result.put("tamano",size);result.put("totalElementos",count);result.put("totalPaginas",count==0?0:(count+size-1)/size);return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> listarTrazoLimitado(Long recorrido,Integer sucursal){verificarRecorrido(recorrido,sucursal,false);return jdbcTemplate.queryForList("""
            SELECT latitud,longitud,fecha_dispositivo AS fechaDispositivo
            FROM (SELECT latitud,longitud,fecha_dispositivo,codigo_punto FROM puntos_recorrido_cobrador
                  WHERE codigo_recorrido=? ORDER BY fecha_dispositivo DESC,codigo_punto DESC LIMIT 5000) recientes
            ORDER BY fecha_dispositivo,codigo_punto
            """,recorrido);}

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPermanencias(Long recorrido, Integer sucursal) {
        verificarRecorrido(recorrido, sucursal, false);
        List<PuntoGps> puntos = jdbcTemplate.query("""
                SELECT latitud, longitud, fecha_dispositivo
                FROM puntos_recorrido_cobrador
                WHERE codigo_recorrido = ?
                ORDER BY fecha_dispositivo, codigo_punto
                """, (rs, fila) -> new PuntoGps(
                    rs.getDouble("latitud"), rs.getDouble("longitud"),
                    rs.getTimestamp("fecha_dispositivo").toLocalDateTime()), recorrido);
        if (puntos.size() < 2) return List.of();

        List<LugarServicio> lugares = jdbcTemplate.query("""
                SELECT u.cuenta_corriente, u.latitud, u.longitud,
                       CONCAT(COALESCE(us.nombre, ''), ' ', COALESCE(us.apellido, '')) AS titular,
                       s.direccion
                FROM ubicaciones_servicio u
                JOIN servicios s ON s.cuenta_corriente = u.cuenta_corriente
                               AND s.codigo_sucursal = u.codigo_sucursal
                JOIN usuarios us ON us.codigo_usuario = s.codigo_usuario
                WHERE s.codigo_sucursal = ?
                """, (rs, fila) -> new LugarServicio(
                    rs.getString("cuenta_corriente"), rs.getDouble("latitud"),
                    rs.getDouble("longitud"), rs.getString("titular").trim(),
                    rs.getString("direccion")), sucursal);

        List<Map<String, Object>> resultado = new ArrayList<>();
        int indice = 0;
        while (indice < puntos.size()) {
            PuntoGps inicial = puntos.get(indice);
            int siguiente = indice + 1;
            double sumaLatitud = inicial.latitud();
            double sumaLongitud = inicial.longitud();
            while (siguiente < puntos.size()
                    && distanciaMetros(inicial.latitud(), inicial.longitud(),
                            puntos.get(siguiente).latitud(), puntos.get(siguiente).longitud()) <= 35) {
                sumaLatitud += puntos.get(siguiente).latitud();
                sumaLongitud += puntos.get(siguiente).longitud();
                siguiente++;
            }
            PuntoGps ultimo = puntos.get(Math.max(indice, siguiente - 1));
            long segundos = Duration.between(inicial.fecha(), ultimo.fecha()).getSeconds();
            int muestras = siguiente - indice;
            if (muestras >= 2 && segundos >= 60) {
                double latitud = sumaLatitud / muestras;
                double longitud = sumaLongitud / muestras;
                LugarCercano cercano = lugarMasCercano(latitud, longitud, lugares);
                Map<String, Object> permanencia = new LinkedHashMap<>();
                permanencia.put("latitud", latitud);
                permanencia.put("longitud", longitud);
                permanencia.put("llegada", inicial.fecha());
                permanencia.put("salida", ultimo.fecha());
                permanencia.put("segundos", segundos);
                permanencia.put("minutos", Math.max(1, Math.round(segundos / 60.0)));
                permanencia.put("muestras", muestras);
                permanencia.put("cuentaCorriente", cercano == null ? null : cercano.lugar().cuentaCorriente());
                permanencia.put("titular", cercano == null ? null : cercano.lugar().titular());
                permanencia.put("direccion", cercano == null ? null : cercano.lugar().direccion());
                permanencia.put("distanciaServicioMetros", cercano == null ? null : Math.round(cercano.distancia()));
                resultado.add(permanencia);
            }
            indice = Math.max(siguiente, indice + 1);
        }
        return resultado;
    }

    private LugarCercano lugarMasCercano(double latitud, double longitud,
            List<LugarServicio> lugares) {
        LugarServicio mejor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (LugarServicio lugar : lugares) {
            double distancia = distanciaMetros(latitud, longitud,
                    lugar.latitud(), lugar.longitud());
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                mejor = lugar;
            }
        }
        return mejor != null && menorDistancia <= 75
                ? new LugarCercano(mejor, menorDistancia) : null;
    }

    private static double distanciaMetros(double latitud1, double longitud1,
            double latitud2, double longitud2) {
        double diferenciaLatitud = Math.toRadians(latitud2 - latitud1);
        double diferenciaLongitud = Math.toRadians(longitud2 - longitud1);
        double a = Math.pow(Math.sin(diferenciaLatitud / 2), 2)
                + Math.cos(Math.toRadians(latitud1)) * Math.cos(Math.toRadians(latitud2))
                * Math.pow(Math.sin(diferenciaLongitud / 2), 2);
        return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record PuntoGps(double latitud, double longitud, LocalDateTime fecha) {}
    private record LugarServicio(String cuentaCorriente, double latitud,
            double longitud, String titular, String direccion) {}
    private record LugarCercano(LugarServicio lugar, double distancia) {}

    @Transactional
    public Map<String, Object> crear(Integer codigoCobrador, String observacion,
            Integer codigoUsuarioSistema, Integer codigoSucursal, String origen) {
        List<Map<String, Object>> cobradores = jdbcTemplate.queryForList("""
                SELECT codigo_cobrador FROM cobradores
                WHERE codigo_cobrador = ? AND codigo_sucursal = ? AND codigo_estado = 1
                FOR UPDATE
                """, codigoCobrador, codigoSucursal);
        if (cobradores.isEmpty()) {
            throw new IllegalArgumentException("El cobrador no existe o no está activo");
        }
        Integer abiertos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM recorridos_cobrador
                WHERE codigo_cobrador = ? AND estado IN ('PENDIENTE', 'ACTIVO')
                """, Integer.class, codigoCobrador);
        if (abiertos != null && abiertos > 0) {
            throw new IllegalStateException("El cobrador ya posee un recorrido pendiente o activo");
        }
        jdbcTemplate.update("""
                INSERT INTO recorridos_cobrador
                    (codigo_cobrador, estado, observacion,
                     codigo_usuario_registro, origen_registro)
                VALUES (?, 'PENDIENTE', ?, ?, ?)
                """, codigoCobrador, limpiarObservacion(observacion),
                codigoUsuarioSistema, normalizarOrigen(origen));
        Long codigoRecorrido = jdbcTemplate.queryForObject(
                "SELECT LAST_INSERT_ID()", Long.class);
        return consultarUno(codigoRecorrido, codigoSucursal);
    }

    @Transactional
    public Map<String, Object> iniciar(Long codigoRecorrido,
            Integer codigoUsuarioSistema, Integer codigoSucursal, String origen) {
        Map<String, Object> recorrido = verificarRecorrido(
                codigoRecorrido, codigoSucursal, true);
        if (!"PENDIENTE".equals(recorrido.get("estado"))) {
            throw new IllegalStateException("Solo se puede iniciar un recorrido pendiente");
        }
        Integer codigoCobrador = ((Number) recorrido.get("codigoCobrador")).intValue();
        validarTelefonoVinculado(codigoCobrador, codigoSucursal);
        Integer activos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM recorridos_cobrador
                WHERE codigo_cobrador = ? AND estado = 'ACTIVO'
                  AND codigo_recorrido <> ?
                """, Integer.class, codigoCobrador, codigoRecorrido);
        if (activos != null && activos > 0) {
            throw new IllegalStateException("El cobrador ya posee otro recorrido activo");
        }
        jdbcTemplate.update("""
                UPDATE recorridos_cobrador
                SET estado = 'ACTIVO', fecha_inicio = CURRENT_TIMESTAMP,
                    codigo_usuario_inicio = ?, origen_inicio = ?
                WHERE codigo_recorrido = ? AND estado = 'PENDIENTE'
                """, codigoUsuarioSistema, normalizarOrigen(origen), codigoRecorrido);
        return consultarUno(codigoRecorrido, codigoSucursal);
    }

    private void validarTelefonoVinculado(Integer codigoCobrador, Integer codigoSucursal) {
        Integer telefonosActivos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dispositivos_cobrador
                WHERE codigo_cobrador = ? AND codigo_sucursal = ? AND activo = 1
                """, Integer.class, codigoCobrador, codigoSucursal);
        if (telefonosActivos == null || telefonosActivos == 0) {
            throw new IllegalStateException(
                    "El cobrador no tiene un teléfono activo vinculado");
        }
    }

    @Transactional
    public Map<String, Object> finalizar(Long codigoRecorrido,
            Integer codigoUsuarioSistema, Integer codigoSucursal, String origen) {
        Map<String, Object> recorrido = verificarRecorrido(
                codigoRecorrido, codigoSucursal, true);
        if (!"ACTIVO".equals(recorrido.get("estado"))) {
            throw new IllegalStateException("Solo se puede finalizar un recorrido activo");
        }
        jdbcTemplate.update("""
                UPDATE recorridos_cobrador
                SET estado = 'FINALIZADO', fecha_fin = CURRENT_TIMESTAMP,
                    codigo_usuario_fin = ?, origen_fin = ?
                WHERE codigo_recorrido = ? AND estado = 'ACTIVO'
                """, codigoUsuarioSistema, normalizarOrigen(origen), codigoRecorrido);
        return consultarUno(codigoRecorrido, codigoSucursal);
    }

    @Transactional
    public boolean registrarPunto(Long codigoRecorrido, String idSincronizacion,
            BigDecimal latitud,
            BigDecimal longitud, BigDecimal precisionMetros,
            BigDecimal velocidadMetrosSegundo, BigDecimal rumboGrados,
            LocalDateTime fechaDispositivo, Integer codigoSucursal, String origen) {
        validarCoordenadas(latitud, longitud, precisionMetros,
                velocidadMetrosSegundo, rumboGrados);
        String identificador = normalizarIdSincronizacion(idSincronizacion);
        Map<String, Object> recorrido = verificarRecorrido(
                codigoRecorrido, codigoSucursal, true);

        Integer existente = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM puntos_recorrido_cobrador
                WHERE id_sincronizacion = ?
                """, Integer.class, identificador);
        if (existente != null && existente > 0) {
            return false;
        }
        if (!"ACTIVO".equals(recorrido.get("estado"))) {
            throw new IllegalStateException("El recorrido no está activo");
        }
        jdbcTemplate.update("""
                INSERT INTO puntos_recorrido_cobrador
                    (id_sincronizacion, codigo_recorrido,
                     latitud, longitud, precision_metros,
                     velocidad_metros_segundo, rumbo_grados,
                     fecha_dispositivo, origen)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, identificador, codigoRecorrido,
                latitud, longitud, precisionMetros,
                velocidadMetrosSegundo, rumboGrados,
                fechaDispositivo == null ? LocalDateTime.now() : fechaDispositivo,
                normalizarOrigen(origen));
        return true;
    }

    private Map<String, Object> verificarRecorrido(Long codigoRecorrido,
            Integer codigoSucursal, boolean bloquear) {
        if (codigoRecorrido == null) {
            throw new IllegalArgumentException("Indique el recorrido");
        }
        String sql = """
                SELECT r.codigo_recorrido AS codigoRecorrido,
                       r.codigo_cobrador AS codigoCobrador,
                       r.estado
                FROM recorridos_cobrador r
                JOIN cobradores c ON c.codigo_cobrador = r.codigo_cobrador
                WHERE r.codigo_recorrido = ? AND c.codigo_sucursal = ?
                """ + (bloquear ? " FOR UPDATE" : "");
        List<Map<String, Object>> recorridos = jdbcTemplate.queryForList(
                sql, codigoRecorrido, codigoSucursal);
        if (recorridos.isEmpty()) {
            throw new IllegalArgumentException("Recorrido no encontrado");
        }
        return recorridos.get(0);
    }

    private Map<String, Object> consultarUno(Long codigoRecorrido,
            Integer codigoSucursal) {
        return listar(codigoSucursal).stream()
                .filter(item -> ((Number) item.get("codigoRecorrido")).longValue()
                        == codigoRecorrido.longValue())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recorrido no encontrado"));
    }

    private String limpiarObservacion(String observacion) {
        if (observacion == null || observacion.isBlank()) return null;
        String limpia = observacion.trim();
        if (limpia.length() > 500) {
            throw new IllegalArgumentException("La observación no puede superar 500 caracteres");
        }
        return limpia;
    }

    private void validarCoordenadas(BigDecimal latitud, BigDecimal longitud,
            BigDecimal precisionMetros, BigDecimal velocidadMetrosSegundo,
            BigDecimal rumboGrados) {
        if (latitud == null || longitud == null) {
            throw new IllegalArgumentException("Indique la latitud y la longitud");
        }
        if (latitud.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitud.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90");
        }
        if (longitud.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitud.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180");
        }
        if (precisionMetros != null && precisionMetros.signum() < 0) {
            throw new IllegalArgumentException("La precisión no puede ser negativa");
        }
        if (velocidadMetrosSegundo != null && velocidadMetrosSegundo.signum() < 0) {
            throw new IllegalArgumentException("La velocidad no puede ser negativa");
        }
        if (rumboGrados != null && (rumboGrados.signum() < 0
                || rumboGrados.compareTo(BigDecimal.valueOf(360)) >= 0)) {
            throw new IllegalArgumentException("El rumbo debe estar entre 0 y 359.99 grados");
        }
    }

    private String normalizarOrigen(String origen) {
        return "APP".equalsIgnoreCase(origen) ? "APP" : "WEB";
    }

    private String normalizarIdSincronizacion(String idSincronizacion) {
        if (idSincronizacion == null || idSincronizacion.isBlank()) {
            throw new IllegalArgumentException("Falta el identificador de sincronización");
        }
        try {
            return UUID.fromString(idSincronizacion.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El identificador de sincronización no es válido");
        }
    }
}
