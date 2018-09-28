# [PhotonPaw](http://timzh.net/photon-paw/)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.timzh/photon-paw/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.timzh/photon-paw)

[![CircleCI](https://circleci.com/gh/tim-zh/photon-paw.svg?style=svg)](https://circleci.com/gh/tim-zh/photon-paw)

#### Backend Example:
```java
PhotonPaw paw = new net.timzh.photonpaw.PhotonPaw();
paw
    //optionally configure main port and websocket port
    .ports(19081, 19082)

    //set location of static resources if any
    .resourcesRoot(".")

    //add additional routes if static resources are not enough
    .bindPath("/test", "application/javascript", request -> "alert('custom routing')")

    //subscribe to events from ui by name
    .handleCommand("a", msg -> System.out.println("command received"))

    //subscribe to requests from ui that expect some response
    .handleQuery("b", msg -> "server response")

    //subscribe to events with unknown or dynamic names
    .defaultHandler((event, msg) -> System.out.println("unknown event " + event + " " + msg))

    //execute some code after establishing a connection with ui
    .start(() ->

         //send an event to ui
         paw.send("b", "server command")
    )

    .println("ui server started")
    .openBrowser()
    .waitForTabToUnload()
    .stop();
```

#### Frontend Example:
```html
<!DOCTYPE html>
<html>
<body>
<script src="photonpaw_client.js"></script>
<script>
PhotonPaw
    //subscribe to events from ui server by name
    .handleCommand("a", msg => alert("command received"))

    //subscribe to events with unknown or dynamic names
    .defaultHandler((event, msg) => alert("unknown event " + event + " " + msg))

    //execute some code after establishing the connection with ui server
    .start().then(() => {

        //send an event to ui server
        PhotonPaw.send("b", "ui command");

        //send a request to ui server and process the response
        PhotonPaw.ask("c", "ui query").then(response => alert("query response received"));
    });
</script>
</body>
</html>
```