package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class ValidadorCliente extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public ValidadorCliente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        String tipoCliente;
        try {
            tipoCliente = jdbcTemplate.queryForObject(
                    "SELECT tipo_cliente FROM clientes WHERE id = ?",
                    String.class, clienteId);
        } catch (Exception excepcion) {
            tipoCliente = null;
        }

        if (tipoCliente == null) {
            contexto.rechazar("Cliente no registrado");
            return;
        }

        contexto.setTipoCliente(tipoCliente);

        if (tipoCliente.equals("MOROSO")) {
            Double deudaPendiente = jdbcTemplate.queryForObject(
                    "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                    Double.class, clienteId);

            boolean fueraDeHorarioDeCorte = LocalTime.now().isBefore(LocalTime.of(20, 0));
            if (deudaPendiente != null && deudaPendiente > 0 && fueraDeHorarioDeCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deudaPendiente);
            }
        }
    }
}