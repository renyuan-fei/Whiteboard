package org.whiteboard.server.service;

import java.rmi.RemoteException;
import java.util.concurrent.ThreadPoolExecutor;
/**
 * File service used to manage file uploads, downloads and Update.
 */
public class FileService extends Service {

    private ThreadPoolExecutor executor;

    public enum ExportType {
        PDF,
        PNG,
    }

    public FileService() {
        super();
    }

    /**
     * Get canvas data from the server.
     */
    public void getCanvasData() {

    }

    /**
     * Clean up the data on the server.
     */
    public void cleanData() {

    }

    /**
     * Upload a canvas data and import it to the server.
     */
    public void importCanvas(String username, String canvasData) throws RemoteException {
        assertRegistered(username);
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
        System.out.println("FileService shut down.");
    }

}
