package cli.ui_components;

public interface Component {
    static void onInput(Component component, String input) {
        switch (component) {
            case TextInput textInput -> textInput.onInput(input);
            case InputSwitcher inputSwitcher -> inputSwitcher.onInput(input);
            default -> throw new AssertionError("Unknown component type: " + component);
        }
    }

    static void draw(Component component) {
        switch (component) {
            case TextInput textInput -> textInput.draw();
            case InputSwitcher inputSwitcher -> inputSwitcher.draw();
            default -> throw new AssertionError("Unknown component type: " + component);
        }
    }
}
