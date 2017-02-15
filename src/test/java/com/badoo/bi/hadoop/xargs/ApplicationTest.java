package com.badoo.bi.hadoop.xargs;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Created by krash on 01.02.17.
 */
public class ApplicationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Properties properties;

    @Before
    public void setUp() {
        properties = new Properties();
        properties.setProperty("spark.master", "local[4]");
        properties.setProperty("spark.app.name", getClass().getName());
        properties.setProperty("spark.driver.allowMultipleContexts", "true");// just for parallel tests running
        properties.setProperty("spark.ui.enabled", "false");
    }

    @Test
    public void testFail() throws Exception {
        thrown.expect(ExecutionException.class);
        thrown.expectMessage("Failed to execute all the jobs");
        new Application(properties, () -> Lists.newArrayList("non-exists")).run();
    }

    @Test
    public void testOk() throws Exception {
        new Application(properties, () -> Lists.newArrayList("/bin/ls -alth /", "/bin/date")).run();
    }
}