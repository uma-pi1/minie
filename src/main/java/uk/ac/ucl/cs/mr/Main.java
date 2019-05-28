package uk.ac.ucl.cs.mr;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * @author Pasquale Minervini
 */

public class Main {

    private static final URI BASE_URI = URI.create("http://localhost:8080/minie/");

    public static void main(String[] args) {
        try {
            System.out.println("MinIE Service");

            final HttpServer server = GrizzlyHttpServerFactory
                    .createHttpServer(BASE_URI, create(), false);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.shutdownNow();
                }
            }));
            server.start();

            System.out.println(String.format("Application started.%n" +
                    "Stop the application using CTRL+C"));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static ResourceConfig create() {
        return new MinIEService();
    }
}
