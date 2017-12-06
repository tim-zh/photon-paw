package tim.zh;

public class Main {
  public static void main(String[] args) {
    GiveMeUi ui = new GiveMeUi();
    ui
        .port(8080)
        .resourcesRoot(".")
        .handler("a", msg -> {
          System.out.println(msg);
          ui.send("b", msg + "!");
        })
        .start()
        .println("press enter to stop")
        .openBrowser()
        .waitForInput()
        .stop();
  }
}
