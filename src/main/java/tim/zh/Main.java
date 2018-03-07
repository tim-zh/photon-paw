package tim.zh;

public class Main {
    public static void main(String[] args) {
        GiveMeUi ui = new GiveMeUi();
        ui
                .ports(8081, 8082)
                .resourcesRoot(".")
                .subscribeCommand("a", msg -> {
                    System.out.println(msg);
                    ui.send("b", msg + "!");
                })
                .subscribeQuery("c", msg -> {
                    System.out.println(msg);
                    return msg + "!!!";
                })
                .start()
                .println("press enter to stop")
                .openBrowser()
                .waitForInput()
                .stop();
    }
}
