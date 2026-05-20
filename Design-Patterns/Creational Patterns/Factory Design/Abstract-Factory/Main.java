// PRODUCT INTERFACES
interface Button {
    void render();
}

interface Checkbox {
    void check();
}

// CONCRETE PRODUCTS (WINDOWS FAMILY)
class WindowsButton implements Button {
    public void render() {
        System.out.println("Rendering a Windows-style button.");
    }
}

class WindowsCheckbox implements Checkbox {
    public void check() {
        System.out.println("Checking a Windows-style checkbox.");
    }
}

// CONCRETE PRODUCTS (MAC FAMILY)

class MacButton implements Button {
    public void render() {
        System.out.println("Rendering a Mac-style button.");
    }
}

class MacCheckbox implements Checkbox {
    public void check() {
        System.out.println("Checking a Mac-style checkbox.");
    }
}

// ABSTRACT FACTORY INTERFACE
interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

// CONCRETE FACTORIES
class WindowsFactory implements UIFactory {
    public Button createButton() {
        return new WindowsButton();
    }
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}

class MacFactory implements UIFactory {
    public Button createButton() {
        return new MacButton();
    }
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}

//CLIENT CODE APPLICATION
class Application {
    private Button button;
    private Checkbox checkbox;

    public Application(UIFactory factory) {
        this.button = factory.createButton();
        this.checkbox = factory.createCheckbox();
    }

    public void paint() {
        button.render();
        checkbox.check();
    }
}

//RUNNABLE ENTRY POINT
public class Main {
    public static void main(String[] args) {
        UIFactory factory;
        
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            factory = new MacFactory();
        } else {
            factory = new WindowsFactory();
        }

        Application app = new Application(factory);
        app.paint();
    }
}