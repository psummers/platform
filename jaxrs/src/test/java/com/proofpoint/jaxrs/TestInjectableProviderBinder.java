package com.proofpoint.jaxrs;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.proofpoint.http.client.Request;
import com.proofpoint.http.client.StringResponseHandler.StringResponse;
import com.proofpoint.http.client.jetty.JettyHttpClient;
import com.proofpoint.http.server.testing.TestingHttpServer;
import com.proofpoint.http.server.testing.TestingHttpServerModule;
import com.proofpoint.json.JsonModule;
import com.proofpoint.node.ApplicationNameModule;
import com.proofpoint.node.testing.TestingNodeModule;
import com.proofpoint.reporting.ReportingModule;
import com.proofpoint.testing.Closeables;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.management.MBeanServer;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static com.proofpoint.http.client.StringResponseHandler.createStringResponseHandler;
import static com.proofpoint.jaxrs.JaxrsBinder.jaxrsBinder;
import static com.proofpoint.jaxrs.JaxrsModule.explicitJaxrsModule;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class TestInjectableProviderBinder
{
    MyResource resource;
    TestingHttpServer server;
    JettyHttpClient client;
    private static final String MYTHING_MESSAGE = "Hello, World!";

    @BeforeMethod
    public void setup()
            throws Exception
    {
        resource = new MyResource();
        server = createServer(resource);

        client = new JettyHttpClient();

        server.start();
    }

    @AfterMethod
    public void teardown()
            throws Exception
    {
        try {
            if (server != null) {
                server.stop();
            }
        }
        catch (Throwable ignored) {
        }
        Closeables.closeQuietly(client);
    }

    @Test
    public void testInjectableProvider()
    {
        Request request = Request.builder()
                            .setUri(server.getBaseUrl().resolve("/mything"))
                            .setMethod("GET")
                            .build();
        StringResponse response = client.execute(request, createStringResponseHandler());
        assertEquals(response.getStatusCode(), Status.OK.getStatusCode(), "Status code");
        assertEquals(response.getBody(), MYTHING_MESSAGE, response.getBody());
    }

    private static TestingHttpServer createServer(final MyResource resource)
    {
        return Guice.createInjector(
                new ApplicationNameModule("test-application"),
                new TestingNodeModule(),
                explicitJaxrsModule(),
                new JsonModule(),
                new ReportingModule(),
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        binder.bind(MBeanServer.class).toInstance(mock(MBeanServer.class));
                    }
                },
                new TestingHttpServerModule(),
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        jaxrsBinder(binder).bindInstance(resource);
                        jaxrsBinder(binder).bindInjectableProviderBinderInstance(MyThingProvider.myThingInjectableProviderBinder());
                    }
                }).getInstance(TestingHttpServer.class);
    }

    @Path("/mything")
    public static class MyResource
    {
        @GET
        public Response getContextInjectable(@Context MyThing thing)
        {
            return Response.ok(thing.getMessage()).build();
        }
    }

    public static class MyThingProvider
        implements Factory<MyThing>
    {

        @Override
        public MyThing provide()
        {
            return new MyThing();
        }

        @Override
        public void dispose(MyThing instance)
        {
        }

        public static AbstractBinder myThingInjectableProviderBinder()
        {
            return new AbstractBinder()
                {
                    @Override
                    protected void configure()
                    {
                        bindFactory(MyThingProvider.class).to(MyThing.class);
                    }
                };
        }
    }

    private static class MyThing
    {
        public String getMessage()
        {
            return MYTHING_MESSAGE;
        }
    }
}
