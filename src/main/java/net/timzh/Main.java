package net.timzh;

public class Main {
    public static void main(String[] args) {
        PhotonPaw ui = new PhotonPaw();
        ui
                .ports(8081, 8082)
                .resourcesRoot(".")
                .handleCommand("a", msg -> {
                    System.out.println(msg);
                    ui.send("b", msg + "!");
                })
                .handleQuery("c", msg -> {
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
