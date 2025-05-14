package org.whiteboard.server.service;

import org.whiteboard.common.rmi.IClientCallback;

import java.rmi.RemoteException;
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

    public void assertRegistered(String sender) throws RemoteException {
        if (!getClients().containsKey(sender)) {
            throw new RemoteException("User '" + sender + "' is not in this whiteboard.");
        }
    }
}
