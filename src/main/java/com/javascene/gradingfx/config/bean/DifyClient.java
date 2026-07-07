package com.javascene.gradingfx.config.bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dify 工作流客户端
 */
public class DifyClient {

    private static final Logger log = Logger.getLogger(DifyClient.class.getName());

    private final String baseUrl;
    private final HttpClient httpClient;

    /**
     * 构造器，传入 Dify 服务的 Base URL
     *
     * @param baseUrl
     */
    public DifyClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 同步阻塞方式调用工作流（返回完整响应字符串）
     */
    public String runWorkflowBlocking(String apiKey, Map<String, Object> extraInputs) {
        try {
            HttpRequest request = buildRequest(apiKey, extraInputs, "blocking");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleError(response);
            return response.body();
        } catch (IOException | InterruptedException e) {
            log.log(Level.SEVERE, "同步请求失败", e);
            throw new RuntimeException("Dify 请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式调用（非阻塞，但返回 CompletableFuture 方便在 JavaFX Task 中使用）
     * 每个数据块会通过 consumer 回调传递
     */
    public CompletableFuture<Void> streamWorkflow(String apiKey, Map<String, Object> extraInputs,
                                                  Consumer<String> onNext,
                                                  Consumer<Throwable> onError,
                                                  Runnable onComplete) {
        HttpRequest request = null;
        try {
            request = buildRequest(apiKey, extraInputs, "streaming");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    handleError(response);
                    try (InputStream is = response.body();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 如果 Dify 返回的是 SSE 格式 (data: {...})，我们去掉 "data: " 前缀
                            // 若直接返回 JSON 行，则原样传递
                            if (line.startsWith("data:")) {
                                line = line.substring(5).trim();
                                if (line.isEmpty()) continue;
                            }
                            // 回调给调用方
                            if (onNext != null) {
                                onNext.accept(line);
                            }
                        }
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } catch (IOException e) {
                        if (onError != null) {
                            onError.accept(e);
                        } else {
                            log.log(Level.SEVERE, "流式读取失败", e);
                        }
                    }
                })
                .exceptionally(ex -> {
                    if (onError != null) {
                        onError.accept(ex);
                    } else {
                        log.log(Level.SEVERE, "流式请求失败", ex);
                    }
                    return null;
                });
    }

    /**
     * 流式调用（同步阻塞方式，等待所有数据收集完，返回完整拼接字符串）
     */
    public String runWorkflow(String apiKey, Map<String, Object> extraInputs) {
        StringBuilder sb = new StringBuilder();
        CompletableFuture<Void> future = streamWorkflow(apiKey, extraInputs,
                chunk -> {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(chunk);
                },
                ex -> { throw new RuntimeException("流式处理异常", ex); },
                () -> { /* 完成 */ }
        );
        future.join(); // 阻塞等待完成
        return sb.toString();
    }

    private HttpRequest buildRequest(String apiKey, Map<String, Object> extraInputs, String responseMode) throws JsonProcessingException {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("inputs", extraInputs != null ? extraInputs : new HashMap<>());
        bodyMap.put("user", "fx-user-" + System.currentTimeMillis());
        bodyMap.put("response_mode", responseMode);

        String jsonBody = new ObjectMapper().writeValueAsString(bodyMap);

        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/workflows/run"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
    }

    private void handleError(HttpResponse<?> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String body = response.body() != null ? response.body().toString() : "";
        log.log(Level.SEVERE, "Dify 响应错误: status=" + status + ", body=" + body);
        if (status >= 400 && status < 500) {
            throw new RuntimeException("客户端错误 (" + status + "): " + body);
        } else if (status >= 500) {
            throw new RuntimeException("服务端错误 (" + status + "): " + body);
        } else {
            throw new RuntimeException("未知错误 (" + status + "): " + body);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }


}