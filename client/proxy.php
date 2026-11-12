<?php
declare(strict_types=1);

const API_HOST = '127.0.0.1';
const API_PORT = 18530;

$services = [
    'ticket' => '/api/v1',
    'booking' => '/booking',
];

function sendJsonError(int $status, string $error, string $message): never
{
    http_response_code($status);
    header('Content-Type: application/json; charset=UTF-8');
    echo json_encode([
        'status' => $status,
        'error' => $error,
        'message' => $message,
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

function decodeChunkedBody(string $body): string|false
{
    $decoded = '';
    $offset = 0;
    $bodyLength = strlen($body);

    while ($offset < $bodyLength) {
        $lineEnd = strpos($body, "\r\n", $offset);
        if ($lineEnd === false) {
            return false;
        }

        $sizeLine = trim(substr($body, $offset, $lineEnd - $offset));
        $extensionPos = strpos($sizeLine, ';');
        if ($extensionPos !== false) {
            $sizeLine = substr($sizeLine, 0, $extensionPos);
        }
        if ($sizeLine === '' || !ctype_xdigit($sizeLine)) {
            return false;
        }

        $chunkSize = hexdec($sizeLine);
        $offset = $lineEnd + 2;
        if ($chunkSize === 0) {
            return $decoded;
        }
        if ($offset + $chunkSize + 2 > $bodyLength) {
            return false;
        }

        $decoded .= substr($body, $offset, $chunkSize);
        $offset += $chunkSize;
        if (substr($body, $offset, 2) !== "\r\n") {
            return false;
        }
        $offset += 2;
    }

    return false;
}

$service = $_GET['service'] ?? '';
$queryString = (string) ($_SERVER['QUERY_STRING'] ?? '');
$queryWithPrefix = '&' . $queryString;
$pathMarkerPosition = strpos($queryWithPrefix, '&path=');
$rawPath = $pathMarkerPosition === false
    ? ''
    : substr($queryWithPrefix, $pathMarkerPosition + strlen('&path='));
$decodedPath = rawurldecode($rawPath);

if (
    !isset($services[$service])
    || $rawPath === ''
    || $decodedPath === ''
    || $decodedPath[0] !== '/'
    || str_contains($decodedPath, "\r")
    || str_contains($decodedPath, "\n")
) {
    sendJsonError(400, 'Bad Request', 'Некорректный адрес сервиса.');
}

$requestPath = $services[$service] . $rawPath;
$transportMethod = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
$methodOverride = strtoupper((string) ($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'] ?? ''));
$method = $transportMethod === 'POST' && in_array($methodOverride, ['DELETE', 'PUT', 'PATCH'], true)
    ? $methodOverride
    : $transportMethod;
if (!preg_match('/^[A-Z]+$/', $method)) {
    sendJsonError(400, 'Bad Request', 'Некорректный HTTP-метод.');
}

$requestBody = file_get_contents('php://input');
if ($requestBody === false) {
    $requestBody = '';
}

$requestHeaders = [
    'Host: ' . API_HOST . ':' . API_PORT,
    'Connection: close',
];

foreach (['HTTP_ACCEPT' => 'Accept', 'CONTENT_TYPE' => 'Content-Type', 'HTTP_IF_NONE_MATCH' => 'If-None-Match'] as $serverKey => $headerName) {
    if (!empty($_SERVER[$serverKey])) {
        $value = str_replace(["\r", "\n"], '', (string) $_SERVER[$serverKey]);
        $requestHeaders[] = $headerName . ': ' . $value;
    }
}

if ($requestBody !== '' && !in_array($method, ['GET', 'HEAD'], true)) {
    $requestHeaders[] = 'Content-Length: ' . strlen($requestBody);
}

$context = stream_context_create([
    'http' => [
        'method' => $method,
        'header' => implode("\r\n", $requestHeaders),
        'content' => $requestBody,
        'ignore_errors' => true,
        'follow_location' => 0,
        'timeout' => 20,
    ],
]);

$target = 'http://' . API_HOST . ':' . API_PORT . $requestPath;
$responseBody = @file_get_contents($target, false, $context);
if ($responseBody === false) {
    $lastError = error_get_last();
    $details = is_array($lastError) && isset($lastError['message'])
        ? (string) $lastError['message']
        : 'неизвестная ошибка';
    sendJsonError(502, 'Bad Gateway', 'Не удалось подключиться к API Gateway: ' . $details);
}

$responseHeaderLines = $http_response_header ?? [];
if ($responseHeaderLines === []) {
    sendJsonError(502, 'Bad Gateway', 'API Gateway не вернул HTTP-заголовки.');
}
$statusLine = array_shift($responseHeaderLines);
if (!is_string($statusLine) || !preg_match('/^HTTP\/\d(?:\.\d)?\s+(\d{3})\b/', $statusLine, $statusMatch)) {
    sendJsonError(502, 'Bad Gateway', 'API Gateway вернул некорректный статус.');
}

$status = (int) $statusMatch[1];
$isChunked = false;

$allowedHeaders = ['content-type', 'location', 'etag', 'allow', 'x-booking-instance'];
foreach ($responseHeaderLines as $headerLine) {
    if (!str_contains($headerLine, ':')) {
        continue;
    }
    [$name, $value] = explode(':', $headerLine, 2);
    $lowerName = strtolower(trim($name));
    if ($lowerName === 'transfer-encoding' && str_contains(strtolower($value), 'chunked')) {
        $isChunked = true;
    }
    if (in_array($lowerName, $allowedHeaders, true)) {
        header(trim($name) . ':' . $value, false);
    }
}

http_response_code($status);
echo $responseBody;
