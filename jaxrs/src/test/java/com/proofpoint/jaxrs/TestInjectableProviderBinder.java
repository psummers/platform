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

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.servlet.http.HttpServletRequest;
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
import static org.testng.Assert.assertTrue;

public class TestInjectableProviderBinder
{
    InjectedResource resource;
    TestingHttpServer server;
    JettyHttpClient client;
    private static final String INJECTED_MESSAGE = "Hello, World!";

    @BeforeMethod
    public void setup()
            throws Exception
    {
        resource = new InjectedResource();
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
                            .setUri(server.getBaseUrl().resolve("/injectedcontext"))
                            .setMethod("GET")
                            .build();
        StringResponse response = client.execute(request, createStringResponseHandler());
        assertEquals(response.getStatusCode(), Status.OK.getStatusCode(), "Status code");
        assertTrue(response.getBody().contains(INJECTED_MESSAGE), response.getBody());
    }

    private static TestingHttpServer createServer(final InjectedResource resource)
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
                        jaxrsBinder(binder).registerContextBinderInstance(InjectedContextObjectProvider.injectedContextBinder());
                    }
                }).getInstance(TestingHttpServer.class);
    }

    @Path("/injectedcontext")
    public static class InjectedResource
    {
        @GET
        public Response getInjectectedContext(@Context InjectedContextObject thing)
        {
            return Response.ok(thing.getMessage()).build();
        }
    }

    public static class InjectedContextObjectProvider
        implements Factory<InjectedContextObject>
    {

        private final HttpServletRequest request;

        @Inject
        public InjectedContextObjectProvider(HttpServletRequest request)
        {
            this.request = request;
        }

        @Override
        public InjectedContextObject provide()
        {
            return new InjectedContextObject(request.toString());
        }

        @Override
        public void dispose(InjectedContextObject instance)
        {
        }

        public static AbstractBinder injectedContextBinder()
        {
            return new AbstractBinder()
                {
                    @Override
                    protected void configure()
                    {
                        bindFactory(InjectedContextObjectProvider.class).to(InjectedContextObject.class);
                    }
                };
        }
    }

    private static class InjectedContextObject
    {
        private final String path;

        public InjectedContextObject(String path)
        {
            this.path = path;
        }

        public String getMessage()
        {
            return String.format("%s: %s", path, INJECTED_MESSAGE);
        }
    }
}
