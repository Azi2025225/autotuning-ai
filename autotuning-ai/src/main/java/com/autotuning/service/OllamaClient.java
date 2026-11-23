package com.autotuning.service;

import com.autotuning.config.OllamaProperties;
import java.util.List;

public interface OllamaClient {

    OllamaProperties props();

    String chat(String model, String prompt, List<byte[]> images, boolean json);
}
