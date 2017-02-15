package com.badoo.bi.hadoop.xargs;

import lombok.extern.log4j.Log4j;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Main class
 * Created by krash on 01.02.17.
 */
@Log4j
public class Main {

    public static void main(String... argv) throws Exception {
        try {

            if ("true".equals(System.getProperty("SPARK_SUBMIT"))) {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                PropertyConfigurator.configure(contextClassLoader.getResourceAsStream("com.badoo/production-log4j.properties"));
            }
            Properties properties = new Properties();
            if (argv.length != 0) {
                properties.load(new FileInputStream(argv[0]));
            }

            StreamJobSupplier supplier = new StreamJobSupplier(System.in);
            supplier.setPreCallback(() -> log.info("Expect commands to be passed to STDIN, one per line"));
            new Application(properties, supplier).run();

        } catch (Throwable err) {
            String message = err.getMessage();
            if (err instanceof ExecutionException) {
                err = err.getCause();
            }
            log.error("FATAL ERROR: " + message, err);
            System.exit(1);
        }
    }
}
