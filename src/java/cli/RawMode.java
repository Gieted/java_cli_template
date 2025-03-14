package cli;

import lombok.SneakyThrows;
import lombok.val;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static cli.LibC.STDIN_FILENO;
import static cli.LibC.linker;
import static cli.LibC.lookup;

public class RawMode {
    private static final long OPOST = 1L;
    private static final int TCSAFLUSH = 2;
    private static final MemoryLayout termiosLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_LONG.withName("c_iflag"),
        ValueLayout.JAVA_LONG.withName("c_oflag"),
        ValueLayout.JAVA_LONG.withName("c_cflag"),
        ValueLayout.JAVA_LONG.withName("c_lflag"),
        MemoryLayout.sequenceLayout(19, ValueLayout.JAVA_BYTE).withName("c_cc")
    );

    private static MemorySegment originalAttributes;

    public static void enableRawMode() {
        try (var arena = Arena.ofConfined()) {
            val termios = arena.allocate(termiosLayout);

            var returnCode = tcgetattr(STDIN_FILENO, termios);
            if (returnCode != 0) {
                throw new RuntimeException("There was a problem calling tcgetattr(): " + returnCode);
            }

            originalAttributes = Arena.global().allocate(termiosLayout);
            originalAttributes.copyFrom(termios);
            cfmakeraw(termios);

            val oFlagOffset = termiosLayout.byteOffset(groupElement("c_oflag"));
            var oFlag = termios.get(ValueLayout.JAVA_LONG, oFlagOffset);
            oFlag |= OPOST;
            termios.set(ValueLayout.JAVA_LONG, oFlagOffset, oFlag);

            returnCode = tcsetattr(STDIN_FILENO, TCSAFLUSH, termios);
            if (returnCode != 0) {
                System.err.println("There was a problem calling tcsetattr(): " + returnCode);
                System.exit(returnCode);
            }
        }
    }

    public static void disableRawMode() {
        if (originalAttributes == null) {
            return;
        }

        tcsetattr(STDIN_FILENO, TCSAFLUSH, originalAttributes);
    }

    @SneakyThrows
    private static int tcgetattr(int fd, MemorySegment termios) {
        val tcgetattr = linker.downcallHandle(
            lookup.find("tcgetattr").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        );
        return (int) tcgetattr.invokeExact(fd, termios);
    }

    @SneakyThrows
    private static int tcsetattr(int fd, int optional_actions, MemorySegment termios) {
        val tcsetattrHandle = linker.downcallHandle(
            lookup.find("tcsetattr").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS
            )
        );
        return (int) tcsetattrHandle.invokeExact(fd, optional_actions, termios);
    }

    @SneakyThrows
    private static void cfmakeraw(MemorySegment termios) {
        val cfmakerawHandle = linker.downcallHandle(
            lookup.find("cfmakeraw").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        cfmakerawHandle.invokeExact(termios);
    }
}
