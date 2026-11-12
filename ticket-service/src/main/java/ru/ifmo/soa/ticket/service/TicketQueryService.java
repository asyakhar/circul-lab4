package ru.ifmo.soa.ticket.service;

import org.springframework.stereotype.Service;
import ru.ifmo.soa.ticket.model.EventType;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class TicketQueryService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public void filter(
            List<Ticket> tickets,
            List<String> filterParameters
    ) {
        if (filterParameters == null) {
            return;
        }

        for (String filter : filterParameters) {
            String[] parts = filter.split(":", 3);

            if (parts.length != 3) {
                throw invalidFilter(filter);
            }

            String field = parts[0];
            String operator = parts[1];
            String value = parts[2];

            checkField(field, "filter");
            checkOperator(operator);
            validateFilter(field, operator, value);
            tickets.removeIf(ticket ->
                    !matches(ticket, field, operator, value)
            );
        }
    }

    public void sort(
            List<Ticket> tickets,
            List<String> sortParameters
    ) {
        if (sortParameters == null || sortParameters.isEmpty()) {
            return;
        }

        Comparator<Ticket> resultComparator = null;

        for (int i = 0; i < sortParameters.size(); i++) {
            String value = sortParameters.get(i);
            String field;
            String direction;

            if (value.contains(",")) {
                String[] parts = value.split(",", -1);

                if (parts.length != 2) {
                    throw invalidSort(value);
                }

                field = parts[0];
                direction = parts[1];
            } else {
                if (i + 1 >= sortParameters.size()) {
                    throw invalidSort(value);
                }

                field = value;
                direction = sortParameters.get(++i);
            }

            checkField(field, "sort");

            if (!direction.equalsIgnoreCase("asc")
                    && !direction.equalsIgnoreCase("desc")) {
                throw invalidSort(value);
            }

            Comparator<Ticket> comparator =
                    comparatorFor(field, direction);

            if (resultComparator == null) {
                resultComparator = comparator;
            } else {
                resultComparator = resultComparator.thenComparing(comparator);
            }
        }

        tickets.sort(resultComparator);
    }

    private void validateFilter(
            String field,
            String operator,
            String value
    ) {
        if (operator.equals("isnull")) {
            if (!value.equals("true") && !value.equals("false")) {
                throw invalidFilter(field + ":" + operator + ":" + value);
            }
            return;
        }

        if (operator.equals("contains")) {
            if (!field.equals("name")
                    && !field.equals("comment")
                    && !field.equals("event.name")) {
                throw new InvalidQueryException(
                        "filter",
                        "Оператор contains поддерживается только для строк."
                );
            }
            return;
        }

        parseValue(field, value);
    }

    private boolean matches(
            Ticket ticket,
            String field,
            String operator,
            String value
    ) {
        Comparable<?> actual = getValue(ticket, field);

        if (operator.equals("isnull")) {
            if (!value.equals("true") && !value.equals("false")) {
                throw invalidFilter(field + ":" + operator + ":" + value);
            }

            boolean expectedNull = Boolean.parseBoolean(value);
            return expectedNull == (actual == null);
        }

        if (actual == null) {
            return false;
        }

        if (operator.equals("contains")) {
            if (!(actual instanceof String text)) {
                throw new InvalidQueryException(
                        "filter",
                        "Оператор contains поддерживается только для строк."
                );
            }

            return text.contains(value);
        }

        Comparable<?> expected = parseValue(field, value);
        int comparison = compare(actual, expected);

        return switch (operator) {
            case "eq" -> comparison == 0;
            case "ne" -> comparison != 0;
            case "gt" -> comparison > 0;
            case "gte" -> comparison >= 0;
            case "lt" -> comparison < 0;
            case "lte" -> comparison <= 0;
            default -> false;
        };
    }

    private Comparator<Ticket> comparatorFor(
            String field,
            String direction
    ) {
        return (first, second) -> {
            Comparable<?> firstValue = getValue(first, field);
            Comparable<?> secondValue = getValue(second, field);

            if (firstValue == null && secondValue == null) {
                return 0;
            }
            if (firstValue == null) {
                return 1;
            }
            if (secondValue == null) {
                return -1;
            }

            int result = compare(firstValue, secondValue);

            if (direction.equalsIgnoreCase("desc")) {
                result = -result;
            }

            return result;
        };
    }

    private Comparable<?> getValue(Ticket ticket, String field) {
        return switch (field) {
            case "id" -> ticket.getId();
            case "name" -> ticket.getName();
            case "coordinates.x" -> ticket.getCoordinates().getX();
            case "coordinates.y" -> ticket.getCoordinates().getY();
            case "creationDate" -> LocalDateTime.parse(
                    ticket.getCreationDate(),
                    DATE_FORMAT
            );
            case "price" -> ticket.getPrice();
            case "comment" -> ticket.getComment();
            case "type" -> ticket.getType();

            case "event.id" -> ticket.getEvent() == null
                    ? null
                    : ticket.getEvent().getId();

            case "event.name" -> ticket.getEvent() == null
                    ? null
                    : ticket.getEvent().getName();

            case "event.ticketsCount" -> ticket.getEvent() == null
                    ? null
                    : ticket.getEvent().getTicketsCount();

            case "event.eventType" -> ticket.getEvent() == null
                    ? null
                    : ticket.getEvent().getEventType();

            default -> null;
        };
    }

    private Comparable<?> parseValue(String field, String value) {
        try {
            return switch (field) {
                case "id", "coordinates.y", "event.id" -> Long.valueOf(value);

                case "coordinates.x", "price" -> parseDouble(value);

                case "event.ticketsCount" -> Integer.valueOf(value);

                case "type" -> TicketType.valueOf(value);

                case "event.eventType" -> parseEventType(value);

                case "creationDate" -> LocalDateTime.parse(value, DATE_FORMAT);

                default -> value;
            };
        } catch (RuntimeException exception) {
            throw new InvalidQueryException(
                    "filter",
                    "Некорректное значение " + value
                            + " для поля " + field + "."
            );
        }
    }

    private Double parseDouble(String value) {
        Double number = Double.valueOf(value);

        if (!Double.isFinite(number)) {
            throw new NumberFormatException();
        }

        return number;
    }

    private EventType parseEventType(String value) {
        EventType type = EventType.valueOf(value);

        if (type == EventType.NULL) {
            throw new IllegalArgumentException();
        }

        return type;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compare(
            Comparable first,
            Comparable second
    ) {
        return first.compareTo(second);
    }

    private void checkField(String field, String parameter) {
        switch (field) {
            case "id",
                 "name",
                 "coordinates.x",
                 "coordinates.y",
                 "creationDate",
                 "price",
                 "comment",
                 "type",
                 "event.id",
                 "event.name",
                 "event.ticketsCount",
                 "event.eventType" -> {
                return;
            }

            default -> throw new InvalidQueryException(
                    parameter,
                    "Поле " + field + " не поддерживается."
            );
        }
    }

    private void checkOperator(String operator) {
        switch (operator) {
            case "eq",
                 "ne",
                 "gt",
                 "gte",
                 "lt",
                 "lte",
                 "contains",
                 "isnull" -> {
                return;
            }

            default -> throw new InvalidQueryException(
                    "filter",
                    "Оператор " + operator + " не поддерживается."
            );
        }
    }

    private InvalidQueryException invalidSort(String value) {
        return new InvalidQueryException(
                "sort",
                "Некорректный параметр сортировки: " + value + "."
        );
    }

    private InvalidQueryException invalidFilter(String value) {
        return new InvalidQueryException(
                "filter",
                "Некорректный параметр фильтрации: " + value + "."
        );
    }
}