package cli.ui_components;

public interface InputComponent extends Component {
    static void setFocus(InputComponent component, boolean focused) {
        switch (component) {
            case TextInput textInput -> textInput.isFocused = focused;
            case InputSwitcher inputSwitcher -> inputSwitcher.isFocused = focused;
            default -> throw new AssertionError("Unknown component type: " + component);
        }
    }
}
