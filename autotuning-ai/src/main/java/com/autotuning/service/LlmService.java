package com.autotuning.service;

public interface LlmService {

    boolean modeDemo();

    String generer(String prompt, boolean json);
}
