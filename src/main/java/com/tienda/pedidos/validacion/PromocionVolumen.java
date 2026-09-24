package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.stereotype.Component;

@Component
public class PromocionVolumen extends ValidadorPedido {

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        int totalUnidades = contexto.getRequest().getItems().stream()
                .mapToInt(ItemPedido::getCantidad)
                .sum();

        if (totalUnidades > 20) {
            contexto.aplicarDescuentoCampana(0.12);
        }
    }
}