package service;

import application.Server;
import service.peer.message.PeerServiceMessage;

public abstract class Service implements Runnable {
    protected boolean stop = false;

    public void processMessage(PeerServiceMessage message) {
    }

    public void stop() {
        stop = true;
        Server.removeFromServicesRunning(this);
    }
}
