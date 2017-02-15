package com.badoo.bi.hadoop.xargs;

import lombok.extern.log4j.Log4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.PropertyConfigurator;
import org.apache.spark.api.java.function.VoidFunction;

import java.io.IOException;
import java.util.Arrays;

/**
 * Executor of one command
 * Created by krash on 01.02.17.
 */
@Log4j
public class JobExecutor implements VoidFunction<String> {

    private boolean loggingReConfig = false;

    @Override
    public void call(String command) throws Exception {

        if (null == command || command.isEmpty()) {
            throw new NullArgumentException("Command can not be empty");
        }

        if (loggingReConfig) {
            PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResourceAsStream("com.badoo/production-log4j.properties"));
        }

        log.info("Going to launch '" + command + "'");
        Process process = null;
        try {

            CommandLine line = CommandLine.parse(command);

            ProcessBuilder builder = getProcessBuilder();
            // quotes removal in bash-style in order to pass correctly to execve()
            String[] mapped = Arrays.stream(line.toStrings()).map(s -> s.replace("\'", "")).toArray(String[]::new);
            builder.command(mapped);
            process = builder.start();

            int exitCode = process.waitFor();
            log.info("Process " + command + " finished with code " + exitCode);
            if (0 != exitCode) {
                throw new InstantiationException("Process " + command + " exited with non-zero exit code (" + exitCode + ")");
            }
        } catch (InterruptedException err) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (IOException err) {
            throw new InstantiationException(err.getMessage());
        }
    }

    ProcessBuilder getProcessBuilder() {
        return new ProcessBuilder().inheritIO();
    }

    void setLoggingReConfig(boolean loggingReConfig) {
        this.loggingReConfig = loggingReConfig;
    }
}
