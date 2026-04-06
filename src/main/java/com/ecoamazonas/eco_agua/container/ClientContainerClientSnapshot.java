package com.ecoamazonas.eco_agua.container;

import com.ecoamazonas.eco_agua.client.Client;

import java.util.List;

public class ClientContainerClientSnapshot {

    private final Client client;
    private final int balance;
    private final List<ClientContainerMovement> movements;

    public ClientContainerClientSnapshot(Client client, int balance, List<ClientContainerMovement> movements) {
        this.client = client;
        this.balance = balance;
        this.movements = movements;
    }

    public Client getClient() {
        return client;
    }

    public int getBalance() {
        return balance;
    }

    public List<ClientContainerMovement> getMovements() {
        return movements;
    }
}
