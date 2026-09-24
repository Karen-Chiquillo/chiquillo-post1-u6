# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio correspondiente al post-contenido de la Unidad 6 de Patrones de Diseño de Software — Sexto Semestre. Consiste en un único proyecto Spring Boot (`pedidos-service`) estructurado en dos partes:
1. Diagnóstico y refactorización de un antipatrón combinado (*God Object* y *Spaghetti Code*) en la clase `GestorPedidos`.
2. Diagnóstico y corrección de un segundo antipatrón (*Golden Hammer*) introducido al hacer crecer el sistema con nuevas campañas de descuento.

---

## Decisiones de diseño

### Parte 1 — GestorPedidos

#### 1. Diagnóstico de Antipatrones con Evidencia del Código
En la versión inicial de `GestorPedidos.java`, se diagnosticó la coexistencia de dos antipatrones críticos de desarrollo: **God Object** y **Spaghetti Code**.

##### A. Antipatrón God Object (Violación de SRP)
La clase concentra **6 razones distintas para cambiar** dentro de su único método público `procesarPedido(PedidoRequest request)`:
1. **Validación de stock e inventario (Líneas 38-45):** Comprobación de existencia de ítems con consulta directa a la base de datos mediante JDBC.
2. **Validación de cliente y política de mora (Líneas 48-73):** Consulta de existencia de cliente, cálculo de saldos en facturas y evaluación de regla horaria de corte.
3. **Cálculo de subtotal y consulta de precios (Líneas 76-82):** Itera la lista de ítems disparando una consulta SQL individual por cada producto.
4. **Cálculo de descuentos comerciales (Líneas 85-103):** Lógica de negocio anidada y condicionada por tipo de cliente, montos y cantidad acumulada de pedidos.
5. **Persistencia transaccional directa vía JDBC (Líneas 108-124):** Inserción manual de registros en `pedidos` y `detalle_pedido`, y mutación directa de `inventario` sin abstracción de repositorio.
6. **Construcción y despacho de notificaciones (Líneas 126-142):** Generación procedimental del texto del correo electrónico mediante `StringBuilder` y acoplamiento directo con `EmailService`.

Esta concentración de responsabilidades en 144 líneas impide modificar una regla comercial o de persistencia sin releer, entender y arriesgar todo el método completo.

##### B. Antipatrón Spaghetti Code
* **Profundidad de anidamiento condicional (3 niveles):**
  - En la validación de mora (Líneas 57-71), se anidan la verificación de estado moroso, saldo deudor positivo y restricción temporal antes de las 20:00:
    - Nivel 1: `else if (tipoCliente.equals("MOROSO"))`
    - Nivel 2: `if (deudaPendiente != null && deudaPendiente > 0)`
    - Nivel 3: `if (ahora.isBefore(LocalTime.of(20, 0)))`
  - En el cálculo de descuento (Líneas 85-96), se alcanzan hasta 3 niveles de condicionales anidados para resolver la tasa del cliente VIP:
    - Nivel 1: `if (tipoCliente.equals("VIP"))`
    - Nivel 2: `if (subtotal > 1_000_000)`
    - Nivel 3: `else if (subtotal > 500_000)` y `else`

* **Operación simultánea en múltiples niveles de abstracción:**
  En una misma secuencia lineal conviven sentencias SQL crudas de bajo nivel (`SELECT`, `INSERT`, `UPDATE`), lógica intermedia de dominio comercial (tasas de descuento e IVA del 19%) y formateo de alto nivel para presentación al usuario (`StringBuilder`).

* **Rigidez ante cambios (Violación de OCP):**
  Agregar una nueva categoría de cliente exige modificar directamente el cuerpo del método entre las líneas 85 y 103, introduciendo nuevas ramas condicionales en una clase crítica de persistencia y facturación.

---

#### 2. Patrones de Diseño Aplicados y Alternativas Descartadas

* **Chain of Responsibility (Validaciones):**
  - **Decisión:** Se encapsuló la secuencia en `ValidadorStock` y `ValidadorCliente`, vinculados a través de `ValidadorPedido`. Se eligió porque las validaciones presentan una dependencia real de orden secuencial y necesidad estricta de corte anticipado (si no hay stock suficiente, se aborta inmediatamente el flujo sin consultar la mora del cliente).
  - **Alternativa descartada:** Un método `validarTodo()` evaluando una lista de predicados (`List<Predicate<ContextoPedido>>`). Se descartó porque evalúa todas las condiciones aun cuando la primera ya haya fallado, sin corte anticipado limpio ni encapsulamiento polimórfico del motivo de rechazo.

* **Strategy (Cálculo de Descuentos por Tipo de Cliente):**
  - **Decisión:** Se modelaron las reglas en `DescuentoVip`, `DescuentoFrecuente` y `DescuentoEstandar` bajo la interfaz `EstrategiaDescuento`, resueltas mediante `SelectorEstrategiaDescuento`. Se eligió porque las reglas de descuento no tienen orden de precedencia entre sí ni requieren abortar el flujo: siempre aplica exactamente una regla según la categoría del cliente.
  - **Alternativa descartada:** Añadir el descuento como un eslabón adicional dentro de la cadena de validación. Se descartó porque los descuentos calculan valores y no validan ni rechazan pedidos; forzarlo en la cadena acoplaría conceptos dispares e introduciría variables mutables innecesarias.

* **Separación de Capas Cohesivas:**
  - Persistencia delegada a `PedidoRepository` (`@Repository`), eliminando sentencias SQL de la lógica de negocio.
  - Generación y despacho de mensajes delegados a `NotificacionPedidoService` (`@Service`).
  - `GestorPedidos` reducido a un orquestador delgado de alto nivel.

---

#### 3. Comparación de la Salida del Sistema (Antes vs. Después)

| Caso de Prueba | Entrada (Cliente / Ítems) | Salida GestorPedidos Original | Salida Versión Refactorizada | Estado |
| :--- | :--- | :--- | :--- | :---: |
| **1. Stock insuficiente** | Cliente 1, Prod. 102 (cant: 10) | Rechazado: *"Stock insuficiente: producto 102"* | Rechazado: *"Stock insuficiente: producto 102"* | Idéntico |
| **2. Cliente no registrado** | Cliente 999, Prod. 101 (cant: 1) | Rechazado: *"Cliente no registrado"* | Rechazado: *"Cliente no registrado"* | Idéntico  |
| **3. Cliente moroso (<20:00)** | Cliente 3, Prod. 101 (cant: 1) | Rechazado: *"Cliente con deuda pendiente: $150000.0"* | Rechazado: *"Cliente con deuda pendiente: $150000.0"* | Idéntico |
| **4. Descuento VIP** | Cliente 1, Prod. 102 (cant: 3) | Confirmado: Subtotal $600.000, Desc. 10%, Total $642.600 | Confirmado: Subtotal $600.000, Desc. 10%, Total $642.600 | Idéntico |
| **5. Descuento FRECUENTE** | Cliente 2, Prod. 101 (cant: 2) | Confirmado: Subtotal $100.000, Desc. 4%, Total $114.240 | Confirmado: Subtotal $100.000, Desc. 4%, Total $114.240 | Idéntico |

---

### Parte 2 — Crecimiento del proyecto
*(Esta sección se documentará al completar la fase de simulación de promociones)*.

---

## Cómo ejecutar

### Compilación y ejecución de la aplicación:
```bash
./mvnw spring-boot:run
```

### Ejecución de pruebas unitarias:
```bash
./mvnw test
```

---

## Herramientas utilizadas
- Java 17 (OpenJDK) 
- Spring Boot 3 
- Spring Data JDBC / JdbcTemplate 
- H2 In-Memory Database 
- Apache Maven 
- JUnit 5 
- Visual Studio Code 
- Git & GitHub

---

## Conclusiones
La refactorización de `GestorPedidos` demuestra la importancia de aplicar el Principio de Responsabilidad Única (SRP) para erradicar el antipatrón God Object, reduciendo el acoplamiento y distribuyendo el ciclo de vida del pedido en capas especializadas. Asimismo, la eliminación de Spaghetti Code mediante el desacoplamiento de validaciones en Chain of Responsibility y reglas de negocio en Strategy restaura el principio Abierto/Cerrado (OCP), facilitando la extensibilidad sin riesgo de regresión funcional.