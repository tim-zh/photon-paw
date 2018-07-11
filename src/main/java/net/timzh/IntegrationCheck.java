package net.timzh;

public class IntegrationCheck {
    public static void main(String[] args) {
        PhotonPaw ui = new PhotonPaw();
        ui
                .ports(19081, 19082)
                .resourcesRoot(".")
                .handleCommand("a", msg -> {
                    System.out.println(msg + " done 1/3");
                    ui.send("b", "backend command");
                })
                .handleQuery("c", msg -> {
                    System.out.println(msg + " done 2/3");
                    return "backend query";
                })
                .defaultHandler((event, msg) -> {
                    System.out.println(msg + " done 3/3");
                    ui.send("d", "backend default handler");
                })
                .bindPath("/bind", "text/javascript", () -> "done('1/4 bind path')")
                .start()
                .println("press enter to stop")
                .openBrowser()
                .waitForInput()
                .stop();
    }
}
