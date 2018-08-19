## Photos

To serve photos, add the following `photos.xml` to your `$JETTY_BASE/webapps` dir, replacing `{path_to_root_photoDir}` 
with the actual path:

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">
<Configure class="org.eclipse.jetty.servlet.ServletContextHandler">
  <Set name="contextPath">/photos</Set>
  <Set name="handler">
    <New class="org.eclipse.jetty.server.handler.ResourceHandler">
        <Set name="resourceBase">{path_to_root_photoDir}</Set>
    </New>
  </Set>
  <Call name="addFilter">
      <Arg>
          <New class="org.eclipse.jetty.servlet.FilterHolder">
              <Arg><New class="org.eclipse.jetty.servlets.CrossOriginFilter"></New></Arg>
              <Call name="setInitParameter">
                  <Arg>allowedOrigins</Arg>
                  <Arg>GET,POST,HEAD,OPTIONS,PUT,DELETE</Arg>
              </Call>
              <Call name="setInitParameter">
                  <Arg>allowedHeaders</Arg>
                  <Arg>X-Requested-With,Content-Type,Accept,Origin,fims-app</Arg>
              </Call>
          </New>
      </Arg>
      <Arg>/*</Arg>
      <Arg><Call class="java.util.EnumSet" name="of"><Arg><Get class="javax.servlet.DispatcherType" name="REQUEST" /></Arg></Call></Arg>
  </Call>
</Configure>
```