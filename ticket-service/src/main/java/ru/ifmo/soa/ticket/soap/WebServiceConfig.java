package ru.ifmo.soa.ticket.soap;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapFaultDetail;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Properties;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        PayloadValidatingInterceptor validator = new PayloadValidatingInterceptor();
        validator.setXsdSchema(ticketsSchema());
        validator.setValidateRequest(true);
        validator.setValidateResponse(true);
        interceptors.add(validator);
    }

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext
    ) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "tickets")
    public DefaultWsdl11Definition ticketsWsdl(XsdSchema ticketsSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("TicketsPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace(TicketEndpoint.NAMESPACE_URI);
        definition.setSchema(ticketsSchema);
        return definition;
    }

    @Bean
    public XsdSchema ticketsSchema() {
        return new SimpleXsdSchema(
                new ClassPathResource("xsd/tickets.xsd")
        );
    }

    @Bean
    public SoapFaultMappingExceptionResolver soapFaultResolver() {
        SoapFaultMappingExceptionResolver resolver =
                new SoapFaultMappingExceptionResolver() {
                    @Override
                    protected void customizeFault(
                            Object endpoint,
                            Exception exception,
                            SoapFault fault
                    ) {
                        if (!(exception instanceof TicketSoapFaultException ticketFault)) {
                            return;
                        }
                        SoapFaultDetail detail = fault.addFaultDetail();
                        addDetail(detail, "status", Integer.toString(ticketFault.getStatus()));
                        addDetail(detail, "code", ticketFault.getCode());
                        addDetail(detail, "message", ticketFault.getMessage());
                        if (ticketFault.getField() != null) {
                            addDetail(detail, "field", ticketFault.getField());
                        }
                    }
                };

        Properties mappings = new Properties();
        mappings.setProperty(
                TicketSoapFaultException.class.getName(),
                SoapFaultDefinition.CLIENT.toString()
        );
        resolver.setExceptionMappings(mappings);

        SoapFaultDefinition defaultFault = new SoapFaultDefinition();
        defaultFault.setFaultCode(SoapFaultDefinition.SERVER);
        resolver.setDefaultFault(defaultFault);
        resolver.setOrder(1);
        return resolver;
    }

    private static void addDetail(
            SoapFaultDetail detail,
            String name,
            String value
    ) {
        detail.addFaultDetailElement(new QName(TicketEndpoint.NAMESPACE_URI, name))
                .addText(value);
    }
}
