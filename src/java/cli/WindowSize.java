package cli;

import lombok.val;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.Linker.Option.firstVariadicArg;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static cli.LibC.STDIN_FILENO;
import static cli.LibC.linker;
import static cli.LibC.lookup;

public class WindowSize {
    private static final long TIOCGWINSZ = 0x40087468L;
    private static final MemoryLayout winsizeLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_SHORT.withName("ws_row"),
        ValueLayout.JAVA_SHORT.withName("ws_col"),
        ValueLayout.JAVA_SHORT.withName("ws_xpixel"),
        ValueLayout.JAVA_SHORT.withName("ws_ypixel")
    );

    public static Size getWindowSize() {
        try (val arena = Arena.ofConfined()) {
            val winsize = arena.allocate(winsizeLayout);
            val returnCode = ioctl(STDIN_FILENO, TIOCGWINSZ, winsize);
            if (returnCode != 0) {
                throw new RuntimeException("There was a problem calling ioctl(): " + returnCode);
            }

            return new Size(
                winsize.get(ValueLayout.JAVA_SHORT, winsizeLayout.byteOffset(groupElement("ws_col"))),
                winsize.get(ValueLayout.JAVA_SHORT, winsizeLayout.byteOffset(groupElement("ws_row")))
            );
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static int ioctl(int fd, long request, MemorySegment winsize) {
        try {
            val ioctlHandle = linker.downcallHandle(
                lookup.find("ioctl").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS
                ), firstVariadicArg(2)
            );
            return (int) ioctlHandle.invokeExact(fd, request, winsize);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
