package com.badoo.bi.hadoop.xargs;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collection;

/**
 * Created by krash on 01.02.17.
 */
public class StreamJobSupplierTest {

    @Test
    public void testAllCommandsRead() throws Exception {
        InputStream stream = Resources.newInputStreamSupplier(Resources.getResource("commands.txt")).getInput();
        StreamJobSupplier supplier = new StreamJobSupplier(stream);
        Collection<String> commands = supplier.get();
        Assert.assertNotNull(commands);
        Assert.assertEquals(2, commands.size());
    }
}