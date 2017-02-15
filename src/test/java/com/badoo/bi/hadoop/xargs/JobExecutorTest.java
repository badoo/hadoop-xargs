package com.badoo.bi.hadoop.xargs;

import lombok.SneakyThrows;
import org.apache.commons.lang.NullArgumentException;
import org.apache.hadoop.util.ThreadUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by krash on 01.02.17.
 */
public class JobExecutorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private JobExecutor executor;

    @Before
    public void setUp() {
        JobExecutor mock = new JobExecutor();
        mock = Mockito.spy(mock);
        // do not pollute STDOUT
        ProcessBuilder builder = new ProcessBuilder();
        builder.inheritIO();
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        Mockito.when(mock.getProcessBuilder()).thenReturn(builder);
        executor = mock;
    }

    @Test
    public void testOk() throws Exception {
        executor.call("/usr/bin/php -r 'var_dump(\"1,2\");'");
    }

    @Test
    public void testNonExisting() throws Exception {
        thrown.expect(InstantiationException.class);
        String command = "some-not-existing";
        thrown.expectMessage(command);
        executor.call(command);
    }

    @Test
    public void testNonExistingDirectory() throws Exception {
        thrown.expect(InstantiationException.class);
        String command = "/bin/ls /" + UUID.randomUUID().toString();
        thrown.expectMessage("exited with non-zero"); // program started, but failed with non-zero
        thrown.expectMessage(command);
        executor.call(command);
    }

    /**
     * This test ensures, that, when interrupted, thread will kill underlying process
     * No validation here exist, proved with coverage :)
     */
    @Test
    public void testInterrupted() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                executor.call("/bin/sleep '100'");
            }
        });
        ThreadUtil.sleepAtLeastIgnoreInterrupts(2_000L);
        executorService.shutdownNow();
    }

    @Test(expected = NullArgumentException.class)
    public void testEmptyCommand() throws Exception
    {
        executor.call("");
    }
}