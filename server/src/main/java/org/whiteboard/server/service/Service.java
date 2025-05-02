package org.whiteboard.server.service;

import org.whiteboard.common.rmi.IClientCallback;

import java.util.Map;

public abstract class Service {
    private Map<String, IClientCallback> clients;

    protected Service() {
    }

    public Map<String, IClientCallback> getClients() {
        return clients;
    }

    public void setClients(Map<String, IClientCallback> clients) {
        this.clients = clients;
    }
}
