package com.illtamer.infinite.bot.expansion.message;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamSupplier {

    InputStream get() throws IOException;

}
