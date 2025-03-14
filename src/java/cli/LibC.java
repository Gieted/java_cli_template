package cli;

import java.lang.foreign.*;

public class LibC {
    public static final Linker linker = Linker.nativeLinker();
    public static final SymbolLookup lookup = linker.defaultLookup();
    public static final int STDIN_FILENO = 0;
}
