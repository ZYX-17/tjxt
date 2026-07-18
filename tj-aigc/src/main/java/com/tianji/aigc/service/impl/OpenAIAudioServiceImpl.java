package com.tianji.aigc.service.impl;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.tianji.aigc.service.AudioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIAudioServiceImpl implements AudioService {

    private final DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel;
    private final DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel;

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
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".tmp";
        File tempFile = null;
        try {
            tempFile = File.createTempFile("stt_", suffix);
            multipartFile.transferTo(tempFile);
            log.info("开始语音识别, 文件：{}", originalFilename);

            Resource audioResource = new FileSystemResource(tempFile);
            AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioResource);
            AudioTranscriptionResponse response = dashScopeAudioTranscriptionModel.call(transcriptionRequest);
            String output = response.getResult().getOutput();
            log.info("语音识别完成, 结果：{}", output);
            return output;
        } catch (Exception e) {
            log.error("语音识别失败, 文件：{}", originalFilename, e);
            throw new RuntimeException("语音识别失败", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }



}