# Configuración por cliente desde base de datos

## Principio principal

Cada cliente tiene su propia base de datos. Por eso la identidad y los módulos deben configurarse en la base de datos del cliente.

```text
.properties        -> conexión técnica y puerto
platform_setting   -> textos, WhatsApp, etiquetas, branding
system modules     -> module.*.enabled en platform_setting
```

## Pantallas principales

```text
/admin/platform-settings
/admin/system-modules
```

## Configuración mínima por cliente

En `/admin/platform-settings` configurar:

- Nombre del negocio.
- Subtítulo del sistema.
- WhatsApp público.
- Logo si aplica.
- Etiqueta de producto/catálogo.
- Etiqueta de cliente/clientes.
- Etiqueta de proveedor/proveedores.
- Etiqueta de insumo/material.
- Etiqueta de envase/empaque.
- Etiqueta de producción/preparación.
- Textos del portal público.
- Textos de login.

## Módulos por cliente

En `/admin/system-modules` activar solo lo que el cliente usará.

### Agua Eco recomendado

- Dashboard.
- Estado del negocio.
- Ventas / CRM.
- Clientes.
- Delivery.
- Reposición.
- Inventario.
- Productos.
- Insumos.
- Envases.
- Producción.
- Finanzas.
- Contabilidad interna.
- Marketing.
- RRHH.
- Portal público/catálogo si se usará.

### Productos de la Selva Belén recomendado

- Dashboard.
- Ventas / CRM.
- Clientes.
- Pedidos.
- Inventario / catálogo.
- Productos.
- Proveedores.
- Compras/egresos.
- Delivery si se controlará reparto.
- Marketing.
- Portal público.
- Blog.
- RRHH si se usará personal.
- Finanzas internas.

Desactivar inicialmente si no se usará:

- Envases.
- Reposición automática específica de agua.
- Producción de agua.

Si Belén usa preparación/empaque, se puede activar producción con etiqueta visible como `Preparación`.

## No volver a hardcodear clientes

No agregar en Java botones como:

```text
Aplicar identidad Cliente X
Aplicar perfil Cliente X
```

Para un cliente nuevo se debe:

1. Crear su base de datos.
2. Ejecutar estructura inicial.
3. Levantar instancia con `.properties` técnico.
4. Configurar `/admin/platform-settings`.
5. Configurar `/admin/system-modules`.
6. Probar rutas principales.
