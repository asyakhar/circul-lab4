package ru.ifmo.soa.ticket.soap;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketInput;
import ru.ifmo.soa.ticket.model.TicketPatch;
import ru.ifmo.soa.ticket.model.TicketType;
import ru.ifmo.soa.ticket.service.EmptyTicketCollectionException;
import ru.ifmo.soa.ticket.service.InvalidQueryException;
import ru.ifmo.soa.ticket.service.TicketNotFoundException;
import ru.ifmo.soa.ticket.service.TicketQueryService;
import ru.ifmo.soa.ticket.service.TicketStore;
import ru.ifmo.soa.ticket.soap.contract.CreateTicketRequest;
import ru.ifmo.soa.ticket.soap.contract.CreateTicketResponse;
import ru.ifmo.soa.ticket.soap.contract.DeleteTicketRequest;
import ru.ifmo.soa.ticket.soap.contract.DeleteTicketResponse;
import ru.ifmo.soa.ticket.soap.contract.GetTicketByIdRequest;
import ru.ifmo.soa.ticket.soap.contract.GetTicketByIdResponse;
import ru.ifmo.soa.ticket.soap.contract.GetTicketWithMaxTypeRequest;
import ru.ifmo.soa.ticket.soap.contract.GetTicketWithMaxTypeResponse;
import ru.ifmo.soa.ticket.soap.contract.GetTicketsByCommentSubstringRequest;
import ru.ifmo.soa.ticket.soap.contract.GetTicketsByCommentSubstringResponse;
import ru.ifmo.soa.ticket.soap.contract.GetTicketsRequest;
import ru.ifmo.soa.ticket.soap.contract.GetTicketsResponse;
import ru.ifmo.soa.ticket.soap.contract.GetTicketsWithTypeGreaterThanRequest;
import ru.ifmo.soa.ticket.soap.contract.GetTicketsWithTypeGreaterThanResponse;
import ru.ifmo.soa.ticket.soap.contract.ObjectFactory;
import ru.ifmo.soa.ticket.soap.contract.PatchTicketRequest;
import ru.ifmo.soa.ticket.soap.contract.PatchTicketResponse;
import ru.ifmo.soa.ticket.soap.contract.TicketList;
import ru.ifmo.soa.ticket.soap.contract.UpdateTicketRequest;
import ru.ifmo.soa.ticket.soap.contract.UpdateTicketResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Endpoint
public class TicketEndpoint {

    public static final String NAMESPACE_URI = "http://soa.ifmo.ru/ticket/ws";

    private final TicketStore store;
    private final TicketQueryService queryService;
    private final TicketSoapMapper mapper;
    private final Validator validator;
    private final ObjectFactory factory = new ObjectFactory();

    public TicketEndpoint(
            TicketStore store,
            TicketQueryService queryService,
            TicketSoapMapper mapper,
            Validator validator
    ) {
        this.store = store;
        this.queryService = queryService;
        this.mapper = mapper;
        this.validator = validator;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getTicketsRequest")
    @ResponsePayload
    public GetTicketsResponse getTickets(@RequestPayload GetTicketsRequest request) {
        return translateFaults(() -> {
            int page = request.getPage() == null ? 0 : request.getPage();
            int size = request.getSize() == null ? 10 : request.getSize();
            require(page >= 0, "page", "Номер страницы должен быть не меньше 0.");
            require(size >= 1 && size <= 100, "size", "Размер страницы должен быть от 1 до 100.");

            List<Ticket> tickets = store.findAll();
            queryService.filter(tickets, request.getFilter());
            queryService.sort(tickets, request.getSort());

            long from = (long) page * size;
            List<Ticket> pageItems = from >= tickets.size()
                    ? List.of()
                    : tickets.subList((int) from, Math.min((int) from + size, tickets.size()));

            GetTicketsResponse response = factory.createGetTicketsResponse();
            response.setTickets(toSoapList(pageItems));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createTicketRequest")
    @ResponsePayload
    public CreateTicketResponse createTicket(@RequestPayload CreateTicketRequest request) {
        return translateFaults(() -> {
            TicketInput input = mapper.toModel(request.getTicket());
            validate(input, "ticket");
            Ticket created = store.create(input).ticket();
            CreateTicketResponse response = factory.createCreateTicketResponse();
            response.setTicket(mapper.toSoap(created));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getTicketByIdRequest")
    @ResponsePayload
    public GetTicketByIdResponse getTicketById(@RequestPayload GetTicketByIdRequest request) {
        return translateFaults(() -> {
            requirePositiveId(request.getId());
            TicketStore.TicketSnapshot snapshot = store.findById(request.getId())
                    .orElseThrow(() -> new TicketNotFoundException(request.getId()));
            GetTicketByIdResponse response = factory.createGetTicketByIdResponse();
            response.setTicket(mapper.toSoap(snapshot.ticket()));
            response.setEtag(store.buildEtag(snapshot));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "updateTicketRequest")
    @ResponsePayload
    public UpdateTicketResponse updateTicket(@RequestPayload UpdateTicketRequest request) {
        return translateFaults(() -> {
            requirePositiveId(request.getId());
            TicketInput input = mapper.toModel(request.getTicket());
            validate(input, "ticket");
            Ticket updated = store.replace(request.getId(), input)
                    .orElseThrow(() -> new TicketNotFoundException(request.getId()))
                    .ticket();
            UpdateTicketResponse response = factory.createUpdateTicketResponse();
            response.setTicket(mapper.toSoap(updated));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "patchTicketRequest")
    @ResponsePayload
    public PatchTicketResponse patchTicket(@RequestPayload PatchTicketRequest request) {
        return translateFaults(() -> {
            requirePositiveId(request.getId());
            TicketPatch patch = mapper.toModel(request.getTicket());
            validate(patch, "ticket");
            boolean empty = patch.getName() == null
                    && patch.getCoordinates() == null
                    && patch.getPrice() == null
                    && patch.getComment() == null
                    && patch.getType() == null
                    && !patch.isEventProvided();
            require(!empty, "ticket", "Необходимо передать хотя бы одно поле.");

            Ticket updated = store.patch(request.getId(), patch)
                    .orElseThrow(() -> new TicketNotFoundException(request.getId()))
                    .ticket();
            PatchTicketResponse response = factory.createPatchTicketResponse();
            response.setTicket(mapper.toSoap(updated));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "deleteTicketRequest")
    @ResponsePayload
    public DeleteTicketResponse deleteTicket(@RequestPayload DeleteTicketRequest request) {
        return translateFaults(() -> {
            requirePositiveId(request.getId());
            DeleteTicketResponse response = factory.createDeleteTicketResponse();
            response.setDeleted(store.delete(request.getId()));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getTicketWithMaxTypeRequest")
    @ResponsePayload
    public GetTicketWithMaxTypeResponse getTicketWithMaxType(
            @RequestPayload GetTicketWithMaxTypeRequest request
    ) {
        return translateFaults(() -> {
            Ticket result = store.findAll().stream()
                    .max(Comparator.comparingInt(item -> item.getType().ordinal()))
                    .orElseThrow(EmptyTicketCollectionException::new);
            GetTicketWithMaxTypeResponse response = factory.createGetTicketWithMaxTypeResponse();
            response.setTicket(mapper.toSoap(result));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getTicketsByCommentSubstringRequest")
    @ResponsePayload
    public GetTicketsByCommentSubstringResponse getTicketsByCommentSubstring(
            @RequestPayload GetTicketsByCommentSubstringRequest request
    ) {
        return translateFaults(() -> {
            require(request.getSubstring() != null && !request.getSubstring().isEmpty(),
                    "substring", "Подстрока не может быть пустой.");
            List<Ticket> result = store.findAll().stream()
                    .filter(ticket -> ticket.getComment().contains(request.getSubstring()))
                    .toList();
            GetTicketsByCommentSubstringResponse response =
                    factory.createGetTicketsByCommentSubstringResponse();
            response.setTickets(toSoapList(result));
            return response;
        });
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getTicketsWithTypeGreaterThanRequest")
    @ResponsePayload
    public GetTicketsWithTypeGreaterThanResponse getTicketsWithTypeGreaterThan(
            @RequestPayload GetTicketsWithTypeGreaterThanRequest request
    ) {
        return translateFaults(() -> {
            TicketType type = TicketType.valueOf(request.getType().value());
            List<Ticket> result = store.findAll().stream()
                    .filter(ticket -> ticket.getType().ordinal() > type.ordinal())
                    .toList();
            GetTicketsWithTypeGreaterThanResponse response =
                    factory.createGetTicketsWithTypeGreaterThanResponse();
            response.setTickets(toSoapList(result));
            return response;
        });
    }

    private TicketList toSoapList(List<Ticket> tickets) {
        TicketList result = factory.createTicketList();
        tickets.stream().map(mapper::toSoap).forEach(result.getTicket()::add);
        return result;
    }

    private void requirePositiveId(long id) {
        require(id > 0, "id", "Идентификатор должен быть больше 0.");
    }

    private void require(boolean condition, String field, String message) {
        if (!condition) {
            throw new TicketSoapFaultException(422, "VALIDATION_ERROR", message, field);
        }
    }

    private <T> void validate(T value, String prefix) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            ConstraintViolation<T> violation = violations.iterator().next();
            String field = violation.getPropertyPath().toString();
            throw new TicketSoapFaultException(
                    422,
                    "VALIDATION_ERROR",
                    violation.getMessage(),
                    field.isEmpty() ? prefix : prefix + "." + field
            );
        }
    }

    private <T> T translateFaults(Supplier<T> action) {
        try {
            return action.get();
        } catch (TicketSoapFaultException exception) {
            throw exception;
        } catch (TicketNotFoundException | EmptyTicketCollectionException exception) {
            throw new TicketSoapFaultException(404, "NOT_FOUND", exception.getMessage(), null);
        } catch (InvalidQueryException exception) {
            throw new TicketSoapFaultException(
                    422, "VALIDATION_ERROR", exception.getMessage(), exception.getField()
            );
        }
    }
}
