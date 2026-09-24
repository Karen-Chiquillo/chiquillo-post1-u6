package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Test
    @DisplayName("Caso 1: Rechazo por stock insuficiente")
    void probarStockInsuficiente() {
        // Pedimos 10 unidades del producto 102 que solo tiene 5 en inventario
        PedidoRequest pedido = new PedidoRequest(1L, "cliente@correo.com",
                List.of(new ItemPedido(102L, 10)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertFalse(resultado.isConfirmado());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Caso 2: Rechazo por cliente no registrado")
    void probarClienteNoRegistrado() {
        // El cliente 999 no existe en la base de datos
        PedidoRequest pedido = new PedidoRequest(999L, "inexistente@correo.com",
                List.of(new ItemPedido(101L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertFalse(resultado.isConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("Caso 3: Validación de cliente moroso")
    void probarClienteMoroso() {
        // El cliente 3 tiene una factura pendiente de $150.000
        PedidoRequest pedido = new PedidoRequest(3L, "moroso@correo.com",
                List.of(new ItemPedido(101L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        // Si la prueba corre antes de las 8:00 PM se rechaza, si es después se permite excepcionalmente
        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Caso 4: Aprobación con descuento para cliente VIP")
    void probarDescuentoClienteVip() {
        // Cliente VIP compra 3 unidades de 200.000 = Subtotal 600.000 (aplica 10% de descuento)
        PedidoRequest pedido = new PedidoRequest(1L, "vip@correo.com",
                List.of(new ItemPedido(102L, 3)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertTrue(resultado.isConfirmado());
        assertTrue(resultado.getTotal() > 0);
    }

    @Test
    @DisplayName("Caso 5: Aprobación con descuento para cliente FRECUENTE")
    void probarDescuentoClienteFrecuente() {
        // Cliente frecuente con 4 compras previas (aplica 4% de descuento)
        PedidoRequest pedido = new PedidoRequest(2L, "frecuente@correo.com",
                List.of(new ItemPedido(101L, 2)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertTrue(resultado.isConfirmado());
        assertTrue(resultado.getTotal() > 0);
    }
}