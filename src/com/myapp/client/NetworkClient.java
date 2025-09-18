package com.myapp.client;

import com.myapp.common.Request;
import com.myapp.common.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkClient {
    private String host;
    private int port;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws Exception {
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized Response send(Request req) throws Exception {
        oos.writeObject(req);
        oos.flush();
        Object o = ois.readObject();
        if (o instanceof Response) return (Response)o;
        throw new RuntimeException("Unexpected response type: " + o.getClass());
    }

    public void close() {
        try { if (ois!=null) ois.close(); } catch(Exception ignored){}
        try { if (oos!=null) oos.close(); } catch(Exception ignored){}
        try { if (socket!=null) socket.close(); } catch(Exception ignored){}
    }
}
