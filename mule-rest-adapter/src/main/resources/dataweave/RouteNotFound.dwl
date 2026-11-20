%dw 2.0

fun mapRouteNotFound(attributes) = {
timestamp: (now() >> "UTC") as String {format: "dd.MM.yyyy HH:mm:ss"},
status: 404,
error: "Not Found",
message: "Запрошенный REST-ресурс не найден.",
path: attributes.requestPath
}
