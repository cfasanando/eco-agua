package com.ecoamazonas.eco_agua.container;

import java.time.LocalDate;

public class ClientContainerBalanceRow {

    private final Long clientId;
    private final String clientName;
    private final String profileName;
    private final String phone;
    private final String address;
    private final int balance;
    private final LocalDate lastMovementDate;
    private final boolean active;

    public ClientContainerBalanceRow(
            Long clientId,
            String clientName,
            String profileName,
            String phone,
            String address,
            int balance,
            LocalDate lastMovementDate,
            boolean active
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.profileName = profileName;
        this.phone = phone;
        this.address = address;
        this.balance = balance;
        this.lastMovementDate = lastMovementDate;
        this.active = active;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public int getBalance() {
        return balance;
    }

    public LocalDate getLastMovementDate() {
        return lastMovementDate;
    }

    public boolean isActive() {
        return active;
    }
}
