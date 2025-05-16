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

    public enum ExportType {
        PDF,
        PNG,
    }

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
    public void importCanvas(String username, String canvasData) throws RemoteException {
        assertRegistered(username);

        try {
            byte[] data = Base64.getDecoder().decode(canvasData);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {

                @SuppressWarnings("unchecked")
                List<Action> importedActions = (List<Action>) ois.readObject();

                synchronized (actionHistory) {
                    actionHistory.clear();
                    actionHistory.addAll(importedActions);
                }

                System.out.println("Canvas imported successfully by user: " + username);
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new RemoteException("ERROR: Failed to import canvas data", ex);
        }

    }

    /**
     * Export the canvas to a file.
     */
    public void exportCanvas() {

    }

    /**
     * Export the canvas to a PDF file.
     */
    public void exportPDF() {

    }

    /**
     * Export the canvas to a PNG file.
     */
    public void exportPNG() {

    }

    public void shutdown() {
        System.out.println("Shutting down FileService...");
        actionHistory.clear();
        System.out.println("FileService shut down.");
    }

}