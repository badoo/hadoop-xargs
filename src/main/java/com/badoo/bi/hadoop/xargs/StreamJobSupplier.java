package com.badoo.bi.hadoop.xargs;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by krash on 01.02.17.
 */
public class StreamJobSupplier implements Supplier<Collection<String>> {

    private final InputStream stream;

    private Runnable preCallback;

    public StreamJobSupplier(InputStream stream) {
        this.stream = stream;
    }

    @Override
    @SneakyThrows
    public Collection<String> get() {

        if (null != preCallback) {
            preCallback.run();
        }

        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        try (BufferedReader buffered = new BufferedReader(reader)) {
            List<String> retval = new ArrayList<>();
            while (true) {
                String line = buffered.readLine();
                if (null == line || line.isEmpty()) {
                    return retval;
                }
                retval.add(line);
            }
        }

    }

    public void setPreCallback(Runnable preCallback) {
        this.preCallback = preCallback;
    }
}
