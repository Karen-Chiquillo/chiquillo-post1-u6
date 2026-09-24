package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
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
                         SelectorEstrategiaDescuento selector,
                         PedidoRepository repository,
                         NotificacionPedidoService notificacion,
                         JdbcTemplate jdbcTemplate) {
        // Enlaza la cadena: ValidadorStock -> ValidadorCliente
        this.primerValidador = stock;
        this.primerValidador.encadenar(cliente);

        this.selector = selector;
        this.repository = repository;
        this.notificacion = notificacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        log.info("Iniciando procesamiento de pedido para cliente [{}]", request.getClienteId());

        // 1. Cadena de validación con corte anticipado
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);

        if (contexto.isRechazado()) {
            return ResultadoPedido.rechazado(contexto.getMotivoRechazo());
        }

        // 2. Consulta de precios y subtotal
        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        // 3. Estrategia de descuento según tipo de cliente
        double descuento = selector.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double impuesto = (subtotal - (subtotal * descuento)) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        // 4. Persistencia mediante repositorio
        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);

        // 5. Notificación mediante servicio
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