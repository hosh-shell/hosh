package org.hosh;

import org.jline.terminal.Terminal;
import java.util.List;

public interface Command {

    void run(Terminal terminal, List<String> args);

}
