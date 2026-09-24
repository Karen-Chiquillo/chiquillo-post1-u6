package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GestorPedidos {

    private static final Logger log = LoggerFactory.getLogger(GestorPedidos.class);

    private final ValidadorPedido primerValidador;
    private final SelectorEstrategiaDescuento selector;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;
    private final JdbcTemplate jdbcTemplate;

    public GestorPedidos(ValidadorStock stock,
                         ValidadorCliente cliente,
                         PromocionBlackFriday blackFriday,
                         PromocionCorporativo corporativo,
                         PromocionVolumen volumen,
                         SelectorEstrategiaDescuento selector,
                         PedidoRepository repository,
                         NotificacionPedidoService notificacion,
                         JdbcTemplate jdbcTemplate) {

        // Cadena con Golden Hammer: 2 validadores reales + 3 promociones
        this.primerValidador = stock;
        this.primerValidador.encadenar(cliente)
                            .encadenar(blackFriday)
                            .encadenar(corporativo)
                            .encadenar(volumen);

        this.selector = selector;
        this.repository = repository;
        this.notificacion = notificacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        log.info("Iniciando procesamiento de pedido para cliente [{}]", request.getClienteId());

        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);

        if (contexto.isRechazado()) {
            return ResultadoPedido.rechazado(contexto.getMotivoRechazo());
        }

        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        // Descuento por tipo de cliente (Strategy)
        double descuentoTipoCliente = selector.seleccionar(contexto.getTipoCliente()).calcular(contexto);

        // Comparación con el descuento que escribió la cadena en el contexto
        double descuento = Math.max(descuentoTipoCliente, contexto.getDescuentoCampana());

        double impuesto = (subtotal - (subtotal * descuento)) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);

        log.info("Pedido {} confirmado. Total: {}", pedidoId, total);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;
        for (ItemPedido item : request.getItems()) {
            Double precioUnitario = jdbcTemplate.queryForObject(
                    "SELECT precio FROM productos WHERE id = ?",
                    Double.class, item.getProductoId());
            if (precioUnitario != null) {
                subtotal += precioUnitario * item.getCantidad();
            }
        }
        return subtotal;
    }
}