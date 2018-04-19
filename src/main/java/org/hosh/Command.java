package org.hosh;

import java.io.IOException;
import java.util.List;

public interface Command {

    void run(List<String> args);

}
