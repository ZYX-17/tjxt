package com.tianji.aigc.service.impl;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.aigc.service.AudioService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAIAudioServiceImpl implements AudioService {

    private final DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.compatible-base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    private static final String ASR_MODEL = "qwen3-asr-flash";

    public OpenAIAudioServiceImpl(DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel) {
        this.dashScopeSpeechSynthesisModel = dashScopeSpeechSynthesisModel;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        var emitter = new ResponseBodyEmitter();
        log.info("开始语音合成, 文本内容：{}", text);
        var speechPrompt = new SpeechSynthesisPrompt(text);
        var responseStream = dashScopeSpeechSynthesisModel.stream(speechPrompt);
        responseStream.subscribe(
                (SpeechSynthesisResponse speechResponse) -> {
                    try {
                        ByteBuffer audioBuffer = speechResponse.getResult().getOutput().getAudio();
                        byte[] audioBytes = new byte[audioBuffer.remaining()];
                        audioBuffer.get(audioBytes);
                        emitter.send(audioBytes);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );
        return emitter;
    }

    @Override
    public String stt(MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        try {
            log.info("开始语音识别, 文件：{}", originalFilename);

            // 1. 读取音频字节并 Base64 编码
            byte[] audioBytes = multipartFile.getBytes();
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            // 2. 拼接 data URI: data:audio/xxx;base64,...
            String mimeType = getMimeType(originalFilename);
            String dataUri = "data:" + mimeType + ";base64," + base64Audio;

            // 3. 构建 DashScope OpenAI 兼容模式请求体
            Map<String, Object> requestBody = Map.of(
                    "model", ASR_MODEL,
                    "messages", List.of(
                            Map.of("role", "user",
                                    "content", List.of(
                                            Map.of("type", "input_audio",
                                                    "input_audio", Map.of("data", dataUri))
                                    ))
                    ),
                    "stream", false
            );

            // 4. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 5. 发送请求
            String url = baseUrl + "/chat/completions";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);

            // 6. 解析响应
            String responseBody = responseEntity.getBody();
            JsonNode root = objectMapper.readTree(responseBody);
            String output = root.path("choices").get(0)
                    .path("message").path("content").asText();

            log.info("语音识别完成, 结果：{}", output);
            return output;
        } catch (Exception e) {
            log.error("语音识别失败, 文件：{}", originalFilename, e);
            throw new RuntimeException("语音识别失败", e);
        }
    }

    /**
     * 根据文件名后缀推断 MIME 类型
     */
    private String getMimeType(String filename) {
        if (filename == null) return "audio/mpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".flac")) return "audio/flac";
        if (lower.endsWith(".aac")) return "audio/aac";
        return "audio/mpeg";
    }
}