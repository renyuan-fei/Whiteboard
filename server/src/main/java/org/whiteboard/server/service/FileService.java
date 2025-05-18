package org.whiteboard.server.service;

import org.whiteboard.common.action.Action;

import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * File service used to manage file uploads, downloads and Update.
 */
public class FileService extends Service {

    private final List<Action> actionHistory = Collections.synchronizedList(new ArrayList<>());

    public FileService() {
        super();
    }

    /**
     * Add a new action
     *
     * @param action action
     */
    public void addAction(Action action) {
        this.actionHistory.add(action);
    }

    /**
     * Get canvas data from the server.
     */
    public String getCanvasData() throws RemoteException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            synchronized (actionHistory) {
                oos.writeObject(new ArrayList<>(actionHistory));
            }
            oos.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException ex) {
            throw new RemoteException("Error: Fail to export canvas data", ex);
        }
    }


    /**
     * Clean up the data on the server.
     */
    public void cleanData() {
        this.actionHistory.clear();
    }

    /**
     * Upload a canvas data and import it to the server.
     */
    public void importCanvas(String canvasData) throws RemoteException {
        if (canvasData == null || canvasData.isEmpty()) {
            throw new RemoteException("Error: Canvas data is empty");
        }
        byte[] data = Base64.getDecoder().decode(canvasData);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            @SuppressWarnings("unchecked")
            List<Action> actions = (List<Action>) ois.readObject();

            List<Action> imported = new ArrayList<>();
            for (Object item : actions) {
                if (item instanceof Action action) {
                    imported.add(action);
                }
            }

            synchronized (actionHistory) {
                actionHistory.clear();
                actionHistory.addAll(imported);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RemoteException("Error: Fail to import canvas data", e);
        }
    }


    public void shutdown() {
        System.out.println("Shutting down FileService...");
        actionHistory.clear();
        System.out.println("FileService shut down.");
    }

}