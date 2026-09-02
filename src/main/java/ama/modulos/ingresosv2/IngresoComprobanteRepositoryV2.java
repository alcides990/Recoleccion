package ama.modulos.ingresosv2;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngresoComprobanteRepositoryV2 extends JpaRepository<Comprobante, ComprobantePK> {

    @Query("""
            select distinct year(c.fechaPago)
            from Comprobante c
            where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
              and c.estado.codigoEstado = 1
            order by year(c.fechaPago) desc
            """)
    List<Integer> aniosDisponibles(@Param("sucursal") Integer sucursal);

    @Query("""
            select new ama.modulos.ingresosv2.IngresoPeriodoV2(
                day(c.fechaPago), sum(c.totalImporte), count(c))
            from Comprobante c
            where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
              and c.estado.codigoEstado = 1
              and year(c.fechaPago) = :anio
              and month(c.fechaPago) = :mes
            group by day(c.fechaPago)
            order by day(c.fechaPago)
            """)
    List<IngresoPeriodoV2> ingresosPorDia(@Param("sucursal") Integer sucursal,
            @Param("anio") Integer anio, @Param("mes") Integer mes);

    @Query("""
            select new ama.modulos.ingresosv2.IngresoPeriodoV2(
                month(c.fechaPago), sum(c.totalImporte), count(c))
            from Comprobante c
            where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
              and c.estado.codigoEstado = 1
              and year(c.fechaPago) = :anio
            group by month(c.fechaPago)
            order by month(c.fechaPago)
            """)
    List<IngresoPeriodoV2> ingresosPorMes(@Param("sucursal") Integer sucursal,
            @Param("anio") Integer anio);

    @Query("""
            select new ama.modulos.ingresosv2.IngresoPeriodoV2(
                year(c.fechaPago), sum(c.totalImporte), count(c))
            from Comprobante c
            where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
              and c.estado.codigoEstado = 1
            group by year(c.fechaPago)
            order by year(c.fechaPago)
            """)
    List<IngresoPeriodoV2> ingresosPorAnio(@Param("sucursal") Integer sucursal);

    @Query("""
            select new ama.modulos.ingresosv2.IngresoCobradorV2(
                concat(coalesce(c.cobrador.nombre, ''), concat(' ', coalesce(c.cobrador.apellido, ''))),
                sum(c.totalImporte), count(c))
            from Comprobante c
            where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
              and c.estado.codigoEstado = 1
              and year(c.fechaPago) = :anio
              and (:mes is null or month(c.fechaPago) = :mes)
            group by c.cobrador.codigoCobrador, c.cobrador.nombre, c.cobrador.apellido
            order by sum(c.totalImporte) desc
            """)
    List<IngresoCobradorV2> ingresosPorCobrador(@Param("sucursal") Integer sucursal,
            @Param("anio") Integer anio, @Param("mes") Integer mes);

    @Query("""
            select new ama.modulos.ingresosv2.IngresoCobradorV2(
                concat(coalesce(c.cobrador.nombre, ''), concat(' ', coalesce(c.cobrador.apellido, ''))),
                sum(c.totalImporte), count(c))
            from Comprobante c
            where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
              and c.estado.codigoEstado = 1
            group by c.cobrador.codigoCobrador, c.cobrador.nombre, c.cobrador.apellido
            order by sum(c.totalImporte) desc
            """)
    List<IngresoCobradorV2> ingresosPorCobradorHistorico(@Param("sucursal") Integer sucursal);

    @Query(value = """
            select trim(concat(coalesce(cob.nombre, ''), ' ', coalesce(cob.apellido, ''))) as nombre,
                   sum(c.total_importe) as importe, count(*) as cantidad
            from comprobantes c
            join cobradores cob on cob.codigo_cobrador = c.codigo_cobrador
            where c.codigo_sucursal = :sucursal and c.codigo_estado = 1
              and c.fecha_pago between :desde and :hasta
            group by cob.codigo_cobrador, cob.nombre, cob.apellido
            order by importe desc
            """, nativeQuery = true)
    List<IngresoResumenProjectionV2> resumenPorCobrador(@Param("sucursal") Integer sucursal,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = """
            select mp.metodo_pago as nombre, sum(dp.importe) as importe, count(*) as cantidad
            from detalle_pago dp
            join metodos_pago mp on mp.codigo_metodo_pago = dp.codigo_metodo_pago
            join comprobantes c on c.numero_comprobante = dp.numero_comprobante
              and c.codigo_sucursal = dp.codigo_sucursal
              and c.codigo_punto_expedicion = dp.codigo_punto_expedicion
              and c.codigo_tipo_comprobante = dp.codigo_tipo_comprobante
              and c.codigo_serie = dp.codigo_serie
            where c.codigo_sucursal = :sucursal and c.codigo_estado = 1
              and c.fecha_pago between :desde and :hasta
            group by mp.codigo_metodo_pago, mp.metodo_pago
            order by importe desc
            """, nativeQuery = true)
    List<IngresoResumenProjectionV2> resumenPorMedioPago(@Param("sucursal") Integer sucursal,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = """
            select cob.codigo_cobrador as codigoCobrador,
                   trim(concat(coalesce(cob.nombre, ''), ' ', coalesce(cob.apellido, ''))) as cobrador,
                   mp.metodo_pago as medioPago, sum(dp.importe) as importe, count(*) as cantidad
            from detalle_pago dp
            join metodos_pago mp on mp.codigo_metodo_pago = dp.codigo_metodo_pago
            join comprobantes c on c.numero_comprobante = dp.numero_comprobante
              and c.codigo_sucursal = dp.codigo_sucursal
              and c.codigo_punto_expedicion = dp.codigo_punto_expedicion
              and c.codigo_tipo_comprobante = dp.codigo_tipo_comprobante
              and c.codigo_serie = dp.codigo_serie
            join cobradores cob on cob.codigo_cobrador = c.codigo_cobrador
            where c.codigo_sucursal = :sucursal and c.codigo_estado = 1
              and c.fecha_pago between :desde and :hasta
            group by cob.codigo_cobrador, cob.nombre, cob.apellido,
                     mp.codigo_metodo_pago, mp.metodo_pago
            order by cobrador, importe desc
            """, nativeQuery = true)
    List<IngresoCobradorMedioProjectionV2> detallePorCobradorYMedio(
            @Param("sucursal") Integer sucursal,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = """
            select pe.codigo_punto_expedicion as codigoPuntoExpedicion,
                   pe.punto_expedicion as puntoExpedicion,
                   cob.codigo_cobrador as codigoCobrador,
                   trim(concat(coalesce(cob.nombre, ''), ' ', coalesce(cob.apellido, ''))) as cobrador,
                   sum(c.total_importe) as importe, count(*) as cantidad
            from comprobantes c
            join puntos_expedicion pe on pe.codigo_sucursal = c.codigo_sucursal
              and pe.codigo_punto_expedicion = c.codigo_punto_expedicion
            join cobradores cob on cob.codigo_cobrador = c.codigo_cobrador
            where c.codigo_sucursal = :sucursal and c.codigo_usuario_sistema = :usuarioSistema
              and c.codigo_estado = 1 and c.fecha_pago between :desde and :hasta
            group by pe.codigo_punto_expedicion, pe.punto_expedicion,
                     cob.codigo_cobrador, cob.nombre, cob.apellido
            order by pe.codigo_punto_expedicion, importe desc
            """, nativeQuery = true)
    List<IngresoCobradorPuntoProjectionV2> resumenPorUsuarioPuntoYCobrador(
            @Param("sucursal") Integer sucursal, @Param("usuarioSistema") Integer usuarioSistema,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
