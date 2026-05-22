package com.docbase.infrastructure.client.python;

import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.infrastructure.client.python.dto.PythonChatRequest;
import com.docbase.infrastructure.client.python.dto.PythonChatResponse;
import com.docbase.infrastructure.client.python.dto.PythonDocumentResponse;
import com.docbase.infrastructure.client.python.dto.PythonDocumentUploadResponse;
import com.docbase.infrastructure.client.python.dto.PythonKbResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAiClient {

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;

    private RestClient restClient() {
        return restClient(properties.getReadTimeoutMs());
    }

    private RestClient restClient(int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public PythonChatResponse sendMessage(PythonChatRequest request) {
        try {
            RestClient client = restClient();
            PythonChatResponse response = client.post()
                    .uri("/api/chat/send")
                    .headers(headers -> {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                            headers.set("X-API-Key", properties.getApiKey());
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(PythonChatResponse.class);

            if (response == null) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python AI service returned an empty response");
            }
            if (response.getCode() != null && response.getCode() != 200) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python AI service error: " + response.getMessage());
            }
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Python AI send API", e);
            throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                    "AI service is temporarily unavailable, please try again later");
        }
    }

    public void streamMessage(PythonChatRequest request, Consumer<String> lineConsumer) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Calling Python stream API: url={}, body={}", buildUrl("/api/chat/stream"), requestBody);

            URL url = new URL(buildUrl("/api/chat/stream"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getConnectTimeoutMs());
            connection.setReadTimeout(properties.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setRequestProperty("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);

            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                connection.setRequestProperty("X-API-Key", properties.getApiKey());
            }

            byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bodyBytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bodyBytes);
                outputStream.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode >= 400) {
                InputStream errorStream = connection.getErrorStream() != null
                        ? connection.getErrorStream()
                        : connection.getInputStream();
                String errorBody = readBody(errorStream);
                log.error("Python stream API failed: status={}, body={}", statusCode, errorBody);
                throw new ApiException(
                        ErrorCode.Internal.INTERNAL_ERROR,
                        "Python AI stream error: HTTP " + statusCode
                                + (errorBody == null || errorBody.isBlank() ? "" : ", body=" + errorBody));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder eventBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        if (eventBuilder.length() > 0) {
                            lineConsumer.accept(eventBuilder.toString());
                            eventBuilder.setLength(0);
                        }
                        continue;
                    }

                    if (eventBuilder.length() > 0) {
                        eventBuilder.append('\n');
                    }
                    eventBuilder.append(line);
                }

                if (eventBuilder.length() > 0) {
                    lineConsumer.accept(eventBuilder.toString());
                }
            } finally {
                connection.disconnect();
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Python AI stream API", e);
            throw new ApiException(
                    ErrorCode.Internal.INTERNAL_ERROR,
                    "AI service is temporarily unavailable, please try again later");
        }
    }

    public PythonKbResponse checkKnowledgeBase(Integer kbId) {
        try {
            RestClient client = restClient();
            PythonKbResponse response = client.get()
                    .uri("/api/kb/{kbId}", kbId)
                    .headers(headers -> {
                        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                            headers.set("X-API-Key", properties.getApiKey());
                        }
                    })
                    .retrieve()
                    .body(PythonKbResponse.class);

            if (response == null || response.getData() == null) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python knowledge base not found: kbId=" + kbId);
            }
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to check Python knowledge base: kbId={}", kbId, e);
            throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                    "Python AI service is temporarily unavailable, cannot verify knowledge base");
        }
    }

    public PythonDocumentUploadResponse uploadDocument(Integer kbId, byte[] fileBytes,
                                                       String filename) {
        return uploadDocument(kbId, fileBytes, filename, null);
    }

    public PythonDocumentUploadResponse uploadDocument(Integer kbId, byte[] fileBytes,
                                                       String filename, String sourceRef) {
        try {
            String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");
            URL url = new URL(buildUrl("/api/doc/upload"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getConnectTimeoutMs());
            connection.setReadTimeout(properties.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                connection.setRequestProperty("X-API-Key", properties.getApiKey());
            }

            ByteArrayOutputStream body = new ByteArrayOutputStream();

            writeFormField(body, boundary, "kb_id", String.valueOf(kbId));

            if (sourceRef != null && !sourceRef.isBlank()) {
                writeFormField(body, boundary, "source_ref", sourceRef);
            }

            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"files\"; filename=\""
                    + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(fileBytes);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            byte[] bodyBytes = body.toByteArray();
            connection.setFixedLengthStreamingMode(bodyBytes.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode >= 400) {
                InputStream errorStream = connection.getErrorStream() != null
                        ? connection.getErrorStream() : connection.getInputStream();
                String errorBody = readBody(errorStream);
                log.error("Python document upload failed: status={}, body={}", statusCode, errorBody);
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python document upload failed: HTTP " + statusCode + ", " + errorBody);
            }

            String responseBody = readBody(connection.getInputStream());
            connection.disconnect();

            PythonDocumentUploadResponse response =
                    objectMapper.readValue(responseBody, PythonDocumentUploadResponse.class);

            if (response == null) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python document upload returned empty response");
            }
            if (response.getCode() != null && response.getCode() != 200) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python document upload failed: " + response.getMessage());
            }
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload document to Python: kbId={}, filename={}", kbId, filename, e);
            throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                    "Python document upload failed: " + e.getMessage());
        }
    }

    private void writeFormField(ByteArrayOutputStream body, String boundary,
                                String name, String value) throws IOException {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(value.getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public PythonDocumentResponse getDocumentStatus(Integer docId) {
        try {
            RestClient client = restClient(5000);
            PythonDocumentResponse response = client.get()
                    .uri("/api/doc/{docId}", docId)
                    .headers(headers -> {
                        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                            headers.set("X-API-Key", properties.getApiKey());
                        }
                    })
                    .retrieve()
                    .body(PythonDocumentResponse.class);

            if (response == null) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                        "Python document status query returned empty response");
            }
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get Python document status: docId={}", docId, e);
            throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR,
                    "Python document status query failed: " + e.getMessage());
        }
    }

    public void deleteDocument(Integer docId) {
        if (docId == null) {
            return;
        }
        try {
            RestClient client = restClient();
            client.delete()
                    .uri("/api/doc/{docId}", docId)
                    .headers(headers -> {
                        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                            headers.set("X-API-Key", properties.getApiKey());
                        }
                    })
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to delete Python document: docId={}", docId, e);
        }
    }

    public void streamAgentMessage(PythonChatRequest request, Consumer<String> lineConsumer) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Calling Python Agent stream API: url={}, body={}", buildUrl("/api/chat/agent/stream"), requestBody);

            URL url = new URL(buildUrl("/api/chat/agent/stream"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getConnectTimeoutMs());
            connection.setReadTimeout(properties.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setRequestProperty("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);

            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                connection.setRequestProperty("X-API-Key", properties.getApiKey());
            }

            byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bodyBytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bodyBytes);
                outputStream.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode >= 400) {
                InputStream errorStream = connection.getErrorStream() != null
                        ? connection.getErrorStream()
                        : connection.getInputStream();
                String errorBody = readBody(errorStream);
                log.error("Python Agent stream API failed: status={}, body={}", statusCode, errorBody);
                throw new ApiException(
                        ErrorCode.Internal.INTERNAL_ERROR,
                        "Python AI Agent stream error: HTTP " + statusCode
                                + (errorBody == null || errorBody.isBlank() ? "" : ", body=" + errorBody));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder eventBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        if (eventBuilder.length() > 0) {
                            lineConsumer.accept(eventBuilder.toString());
                            eventBuilder.setLength(0);
                        }
                        continue;
                    }

                    if (eventBuilder.length() > 0) {
                        eventBuilder.append('\n');
                    }
                    eventBuilder.append(line);
                }

                if (eventBuilder.length() > 0) {
                    lineConsumer.accept(eventBuilder.toString());
                }
            } finally {
                connection.disconnect();
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Python AI Agent stream API", e);
            throw new ApiException(
                    ErrorCode.Internal.INTERNAL_ERROR,
                    "AI service is temporarily unavailable, please try again later");
        }
    }

    public void deleteConversation(Integer convId) {
        if (convId == null) {
            return;
        }

        try {
            RestClient client = restClient();
            client.delete()
                    .uri("/api/chat/conversations/{convId}", convId)
                    .headers(headers -> {
                        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                            headers.set("X-API-Key", properties.getApiKey());
                        }
                    })
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to delete Python conversation: convId={}", convId, e);
        }
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private String readBody(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }
}
