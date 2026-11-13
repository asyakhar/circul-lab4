package ru.ifmo.soa.ticket.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;
import ru.ifmo.soa.ticket.service.TicketQueryService;
import ru.ifmo.soa.ticket.service.TicketStore;

import javax.xml.transform.Source;
import java.util.Map;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

class TicketEndpointTest {

    private static final Map<String, String> NAMESPACES = Map.of(
            "t", TicketEndpoint.NAMESPACE_URI
    );

    private AnnotationConfigApplicationContext applicationContext;
    private MockWebServiceClient client;

    @BeforeEach
    void setUp() {
        applicationContext = new AnnotationConfigApplicationContext(
                WebServiceConfig.class,
                TestConfig.class
        );
        client = MockWebServiceClient.createClient(applicationContext);
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void createsAndReadsTicket() throws Exception {
        Source createRequest = xml("""
                <t:createTicketRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:ticket>
                    <t:name>Билет в партер</t:name>
                    <t:coordinates><t:x>10.5</t:x><t:y>20</t:y></t:coordinates>
                    <t:price>2500.5</t:price>
                    <t:comment>Место рядом со сценой</t:comment>
                    <t:type>VIP</t:type>
                    <t:event>
                      <t:name>Вечер оперы</t:name>
                      <t:ticketsCount>120</t:ticketsCount>
                      <t:eventType>OPERA</t:eventType>
                    </t:event>
                  </t:ticket>
                </t:createTicketRequest>
                """);

        client.sendRequest(withPayload(createRequest))
                .andExpect(xpath("/t:createTicketResponse/t:ticket/t:id", NAMESPACES).evaluatesTo("1"))
                .andExpect(xpath("/t:createTicketResponse/t:ticket/t:name", NAMESPACES)
                        .evaluatesTo("Билет в партер"));

        Source getRequest = xml("""
                <t:getTicketByIdRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>1</t:id>
                </t:getTicketByIdRequest>
                """);

        client.sendRequest(withPayload(getRequest))
                .andExpect(xpath("/t:getTicketByIdResponse/t:ticket/t:id", NAMESPACES).evaluatesTo("1"))
                .andExpect(xpath("/t:getTicketByIdResponse/t:etag", NAMESPACES)
                        .evaluatesTo("\"ticket-1-v1\""));
    }

    @Test
    void returnsStructuredFaultForInvalidId() throws Exception {
        Source request = xml("""
                <t:getTicketByIdRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>-1</t:id>
                </t:getTicketByIdRequest>
                """);

        client.sendRequest(withPayload(request))
                .andExpect(clientOrSenderFault())
                .andExpect(xpath("//*[local-name()='detail']/t:status", NAMESPACES).evaluatesTo("422"))
                .andExpect(xpath("//*[local-name()='detail']/t:code", NAMESPACES)
                        .evaluatesTo("VALIDATION_ERROR"));
    }

    @Test
    void listsUpdatesPatchesAndDeletesTicket() throws Exception {
        createTicket("Первый билет", "Исходный комментарий", "USUAL");

        client.sendRequest(withPayload(xml("""
                <t:getTicketsRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:page>0</t:page>
                  <t:size>10</t:size>
                </t:getTicketsRequest>
                """)))
                .andExpect(xpath("count(/t:getTicketsResponse/t:tickets/t:ticket)", NAMESPACES)
                        .evaluatesTo("1"))
                .andExpect(xpath("/t:getTicketsResponse/t:tickets/t:ticket/t:name", NAMESPACES)
                        .evaluatesTo("Первый билет"));

        client.sendRequest(withPayload(xml("""
                <t:updateTicketRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>1</t:id>
                  <t:ticket>
                    <t:name>Обновлённый билет</t:name>
                    <t:coordinates><t:x>15.0</t:x><t:y>25</t:y></t:coordinates>
                    <t:price>3200</t:price>
                    <t:comment>Обновлённый комментарий</t:comment>
                    <t:type>BUDGETARY</t:type>
                  </t:ticket>
                </t:updateTicketRequest>
                """)))
                .andExpect(xpath("/t:updateTicketResponse/t:ticket/t:name", NAMESPACES)
                        .evaluatesTo("Обновлённый билет"));

        client.sendRequest(withPayload(xml("""
                <t:patchTicketRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>1</t:id>
                  <t:ticket><t:price>4100</t:price></t:ticket>
                </t:patchTicketRequest>
                """)))
                .andExpect(xpath("/t:patchTicketResponse/t:ticket/t:price", NAMESPACES)
                        .evaluatesTo("4100.0"))
                .andExpect(xpath("/t:patchTicketResponse/t:ticket/t:name", NAMESPACES)
                        .evaluatesTo("Обновлённый билет"));

        client.sendRequest(withPayload(xml("""
                <t:deleteTicketRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>1</t:id>
                </t:deleteTicketRequest>
                """)))
                .andExpect(xpath("/t:deleteTicketResponse/t:deleted", NAMESPACES)
                        .evaluatesTo("true"));

        assertNotFound(1);
    }

    @Test
    void executesAdditionalTicketQueries() throws Exception {
        createTicket("VIP билет", "опера у сцены", "VIP");
        createTicket("Дешёвый билет", "опера на балконе", "CHEAP");

        client.sendRequest(withPayload(xml("""
                <t:getTicketWithMaxTypeRequest xmlns:t="http://soa.ifmo.ru/ticket/ws"/>
                """)))
                .andExpect(xpath("/t:getTicketWithMaxTypeResponse/t:ticket/t:id", NAMESPACES)
                        .evaluatesTo("2"))
                .andExpect(xpath("/t:getTicketWithMaxTypeResponse/t:ticket/t:type", NAMESPACES)
                        .evaluatesTo("CHEAP"));

        client.sendRequest(withPayload(xml("""
                <t:getTicketsByCommentSubstringRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:substring>опера</t:substring>
                </t:getTicketsByCommentSubstringRequest>
                """)))
                .andExpect(xpath(
                        "count(/t:getTicketsByCommentSubstringResponse/t:tickets/t:ticket)",
                        NAMESPACES
                ).evaluatesTo("2"));

        client.sendRequest(withPayload(xml("""
                <t:getTicketsWithTypeGreaterThanRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:type>USUAL</t:type>
                </t:getTicketsWithTypeGreaterThanRequest>
                """)))
                .andExpect(xpath(
                        "count(/t:getTicketsWithTypeGreaterThanResponse/t:tickets/t:ticket)",
                        NAMESPACES
                ).evaluatesTo("1"))
                .andExpect(xpath(
                        "/t:getTicketsWithTypeGreaterThanResponse/t:tickets/t:ticket/t:type",
                        NAMESPACES
                ).evaluatesTo("CHEAP"));
    }

    @Test
    void returnsStructuredFaultForEmptyPatch() throws Exception {
        createTicket("Билет", "Комментарий", "VIP");

        client.sendRequest(withPayload(xml("""
                <t:patchTicketRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>1</t:id>
                  <t:ticket/>
                </t:patchTicketRequest>
                """)))
                .andExpect(clientOrSenderFault())
                .andExpect(xpath("//*[local-name()='detail']/t:status", NAMESPACES)
                        .evaluatesTo("422"))
                .andExpect(xpath("//*[local-name()='detail']/t:field", NAMESPACES)
                        .evaluatesTo("ticket"));
    }

    private void createTicket(String name, String comment, String type) throws Exception {
        client.sendRequest(withPayload(xml("""
                <t:createTicketRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:ticket>
                    <t:name>%s</t:name>
                    <t:coordinates><t:x>10.5</t:x><t:y>20</t:y></t:coordinates>
                    <t:price>2500.5</t:price>
                    <t:comment>%s</t:comment>
                    <t:type>%s</t:type>
                  </t:ticket>
                </t:createTicketRequest>
                """.formatted(name, comment, type))))
                .andExpect(xpath("/t:createTicketResponse/t:ticket/t:name", NAMESPACES)
                        .evaluatesTo(name));
    }

    private void assertNotFound(long id) throws Exception {
        client.sendRequest(withPayload(xml("""
                <t:getTicketByIdRequest xmlns:t="http://soa.ifmo.ru/ticket/ws">
                  <t:id>%d</t:id>
                </t:getTicketByIdRequest>
                """.formatted(id))))
                .andExpect(clientOrSenderFault())
                .andExpect(xpath("//*[local-name()='detail']/t:status", NAMESPACES)
                        .evaluatesTo("404"))
                .andExpect(xpath("//*[local-name()='detail']/t:code", NAMESPACES)
                        .evaluatesTo("NOT_FOUND"));
    }

    private Source xml(String value) {
        return new StringSource(value);
    }

    @Configuration
    static class TestConfig {

        @Bean
        TicketStore ticketStore() {
            return new TicketStore();
        }

        @Bean
        TicketQueryService ticketQueryService() {
            return new TicketQueryService();
        }

        @Bean
        TicketSoapMapper ticketSoapMapper() {
            return new TicketSoapMapper();
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }

        @Bean
        TicketEndpoint ticketEndpoint(
                TicketStore store,
                TicketQueryService queryService,
                TicketSoapMapper mapper,
                LocalValidatorFactoryBean validator
        ) {
            return new TicketEndpoint(store, queryService, mapper, validator);
        }
    }
}
