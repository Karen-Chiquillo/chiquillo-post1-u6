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
        // Producto 102 solo tiene 5 unidades en inventario; solicitamos 10
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
        // Cliente 3 tiene deuda pendiente
        PedidoRequest pedido = new PedidoRequest(3L, "moroso@correo.com",
                List.of(new ItemPedido(101L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        // Se valida la respuesta del gestor ante cliente moroso
        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado());
    }

    @Test
    @DisplayName("Caso 4: Aprobación con cliente VIP")
    void probarClientePromocionVip() {
        PedidoRequest pedido = new PedidoRequest(1L, "vip@correo.com",
                List.of(new ItemPedido(101L, 2)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertTrue(resultado.isConfirmado());
        assertTrue(resultado.getTotal() > 0);
    }

    @Test
    @DisplayName("Caso 5: Aprobación con cliente FRECUENTE")
    void probarClienteFrecuente() {
        PedidoRequest pedido = new PedidoRequest(2L, "frecuente@correo.com",
                List.of(new ItemPedido(101L, 2)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertTrue(resultado.isConfirmado());
        assertTrue(resultado.getTotal() > 0);
    }

    @Test
    @DisplayName("Caso 6: Campaña Black Friday (25% activo)")
    void probarCampanaBlackFriday() {
        // Cliente estándar (ID 4) sin NIT: Subtotal 100.000, 25% descuento = 75.000 + IVA (14.250) = 89.250
        PedidoRequest pedido = new PedidoRequest(4L, "estandar@correo.com",
                List.of(new ItemPedido(101L, 2)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertTrue(resultado.isConfirmado());
        assertEquals(89250.0, resultado.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Caso 7: Descuento por Volumen (> 20 unidades)")
    void probarDescuentoPorVolumen() {
        // Cliente estándar comprando 25 unidades del producto 101
        PedidoRequest pedido = new PedidoRequest(4L, "estandar@correo.com",
                List.of(new ItemPedido(101L, 25)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(pedido);

        assertTrue(resultado.isConfirmado());
        assertTrue(resultado.getTotal() > 0);
    }
}