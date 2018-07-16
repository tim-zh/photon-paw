# PhotonPaw

#### Backend Example:
```java
PhotonPaw paw = new PhotonPaw();
paw
    .ports(19081, 19082)
    .resourcesRoot(".")
    .bindPath("/test", "text/javascript", () -> "alert('custom routing')")
    .handleCommand("a", msg -> System.out.println("command received"))
    .handleQuery("b", msg -> "server response")
    .defaultHandler((event, msg) -> System.out.println("unknown event " + event + " " + msg))
    .start(() -> paw.send("b", "server command"))
    .println("press enter to stop")
    .openBrowser()
    .waitForInput()
    .stop();
```

#### Frontend Example:
```html
<!DOCTYPE html>
<html>
<head>
  <script src="photonpaw_client.js"></script>
</head>
<body>
<script>
PhotonPaw
    .handleCommand("a", msg => alert("command received"))
    .setDefaultHandler((event, msg) => "unknown event " + event + " " + msg)
    .start(() => {
        PhotonPaw.ask("b", "frontend query").then(response => alert("query response received"));
        PhotonPaw.send("c", "ui command");
    });
</script>
</body>
</html>
```