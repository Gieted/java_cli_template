package cli;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Size {
    public int width;
    public int height;

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
