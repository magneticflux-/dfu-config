package com.skaggsm.dfu_config;

import java.util.List;

interface TestConfig {
    String myString();

    List<Integer> myList();
}

public class Example {
    public static void main(String[] args) {
        System.out.println(ConfigBuilder.buildCodec(TestConfig.class));
    }
}
