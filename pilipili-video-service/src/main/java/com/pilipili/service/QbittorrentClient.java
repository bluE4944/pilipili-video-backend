package com.pilipili.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilipili.exception.BusinessException;
import com.pilipili.utils.Status;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class QbittorrentClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${download.qb.base-url:http://127.0.0.1:18080}")
    private String baseUrl;

    @Value("${download.qb.username:}")
    private String username;

    @Value("${download.qb.password:}")
    private String password;

    private volatile String cookie;

    public QbittorrentClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getAppVersion() {
        return executeText(HttpMethod.GET, "/api/v2/app/version", null, MediaType.APPLICATION_JSON, false);
    }

    public void addMagnet(String magnetUrl, String savePath, String category, String tag, boolean paused) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("urls", magnetUrl);
        body.add("savepath", savePath);
        body.add("category", category);
        body.add("tags", tag);
        body.add("stopped", Boolean.toString(paused));
        executeText(HttpMethod.POST, "/api/v2/torrents/add", body, MediaType.MULTIPART_FORM_DATA, false);
    }

    public void addTorrent(byte[] content, String fileName, String savePath, String category, String tag, boolean paused) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("torrents", new NamedByteArrayResource(content, fileName));
        body.add("savepath", savePath);
        body.add("category", category);
        body.add("tags", tag);
        body.add("stopped", Boolean.toString(paused));
        executeText(HttpMethod.POST, "/api/v2/torrents/add", body, MediaType.MULTIPART_FORM_DATA, false);
    }

    public List<QbTorrentInfo> getTorrentsByTag(String tag) {
        String text = executeText(HttpMethod.GET, "/api/v2/torrents/info?tag=" + encode(tag), null, MediaType.APPLICATION_JSON, true);
        return parseTorrentList(text);
    }

    public QbTorrentInfo getTorrentByHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return null;
        }
        String text = executeText(HttpMethod.GET, "/api/v2/torrents/info?hashes=" + encode(hash.trim()), null, MediaType.APPLICATION_JSON, true);
        List<QbTorrentInfo> list = parseTorrentList(text);
        return list.isEmpty() ? null : list.get(0);
    }

    public void pause(String hash) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("hashes", hash);
        executeText(HttpMethod.POST, "/api/v2/torrents/pause", body, MediaType.APPLICATION_FORM_URLENCODED, false);
    }

    public void resume(String hash) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("hashes", hash);
        executeText(HttpMethod.POST, "/api/v2/torrents/resume", body, MediaType.APPLICATION_FORM_URLENCODED, false);
    }

    public void delete(String hash, boolean deleteFiles) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("hashes", hash);
        body.add("deleteFiles", Boolean.toString(deleteFiles));
        executeText(HttpMethod.POST, "/api/v2/torrents/delete", body, MediaType.APPLICATION_FORM_URLENCODED, false);
    }

    private List<QbTorrentInfo> parseTorrentList(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(text);
            if (!root.isArray()) {
                return Collections.emptyList();
            }
            List<QbTorrentInfo> result = new ArrayList<>();
            for (JsonNode node : root) {
                QbTorrentInfo info = new QbTorrentInfo();
                info.setHash(textValue(node, "hash"));
                info.setName(textValue(node, "name"));
                info.setState(textValue(node, "state"));
                info.setSavePath(textValue(node, "save_path"));
                info.setProgress(roundProgress(node.path("progress").asDouble(0D) * 100D));
                info.setDownloadedBytes(longValue(node, "downloaded", "completed"));
                info.setTotalBytes(longValue(node, "total_size", "size"));
                info.setDownloadSpeed(longValue(node, "dlspeed"));
                info.setEtaSeconds(longValue(node, "eta"));
                info.setErrorMessage(textValue(node, "errmsg"));
                result.add(info);
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(Status.BUSINESS_ERROR, "解析 qBittorrent 响应失败: " + e.getMessage());
        }
    }

    private String executeText(HttpMethod method, String path, Object body, MediaType contentType, boolean allowNotFound) {
        ensureConfigured();
        try {
            return doExecute(method, path, body, contentType, allowNotFound, true);
        } catch (HttpClientErrorException.Forbidden e) {
            cookie = null;
            return doExecute(method, path, body, contentType, allowNotFound, true);
        } catch (RestClientException e) {
            throw new BusinessException(Status.BUSINESS_ERROR, "连接 qBittorrent 失败: " + e.getMessage());
        }
    }

    private String doExecute(HttpMethod method, String path, Object body, MediaType contentType, boolean allowNotFound, boolean loginIfNeeded) {
        if (loginIfNeeded && cookie == null) {
            login();
        }
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(buildUri(path), method, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            if (allowNotFound) {
                return "";
            }
            throw e;
        }
    }

    private void login() {
        ensureConfigured();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("username", username.trim());
        body.add("password", password.trim());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(buildUri("/api/v2/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (RestClientException e) {
            throw new BusinessException(Status.BUSINESS_ERROR, "登录 qBittorrent 失败: " + e.getMessage());
        }
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            throw new BusinessException(Status.BUSINESS_ERROR, "qBittorrent 未返回认证 Cookie");
        }
        String rawCookie = cookies.get(0);
        int separator = rawCookie.indexOf(';');
        cookie = separator >= 0 ? rawCookie.substring(0, separator) : rawCookie;
        String bodyText = response.getBody() == null ? "" : response.getBody().trim().toLowerCase(Locale.ROOT);
        if (!bodyText.contains("ok")) {
            throw new BusinessException(Status.BUSINESS_ERROR, "qBittorrent 登录失败");
        }
    }

    private void ensureConfigured() {
        if (isBlank(baseUrl) || isBlank(username) || isBlank(password)) {
            throw new BusinessException(Status.BUSINESS_ERROR, "qBittorrent 配置不完整");
        }
    }

    private URI buildUri(String path) {
        String normalizedBaseUrl = baseUrl.trim();
        String normalizedPath = path == null ? "" : path.trim();
        if (normalizedBaseUrl.endsWith("/") && normalizedPath.startsWith("/")) {
            return URI.create(normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1) + normalizedPath);
        }
        if (!normalizedBaseUrl.endsWith("/") && !normalizedPath.startsWith("/")) {
            return URI.create(normalizedBaseUrl + "/" + normalizedPath);
        }
        return URI.create(normalizedBaseUrl + normalizedPath);
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private long longValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asLong(0L);
            }
        }
        return 0L;
    }

    private double roundProgress(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Data
    public static class QbTorrentInfo {
        private String hash;
        private String name;
        private String state;
        private String savePath;
        private Double progress;
        private Long downloadedBytes;
        private Long totalBytes;
        private Long downloadSpeed;
        private Long etaSeconds;
        private String errorMessage;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {

        private final String fileName;

        private NamedByteArrayResource(byte[] byteArray, String fileName) {
            super(byteArray);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
