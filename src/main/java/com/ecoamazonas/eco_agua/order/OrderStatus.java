package com.ecoamazonas.eco_agua.order;

public enum OrderStatus {
    REQUESTED,  // pedido tomado, pendiente de entrega/pago
    PAID,       // entregado y pagado
    CREDIT,     // entregado y fiado
    CANCELED    // anulado (no se entregó)
}
