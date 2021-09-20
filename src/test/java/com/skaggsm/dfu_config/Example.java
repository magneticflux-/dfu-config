package com.skaggsm.dfu_config;

import com.mojang.serialization.JsonOps;

import java.util.Arrays;
import java.util.List;

interface TestConfig {
    String myString();

    List<Integer> myList();

    default String myStringUpperCase() {
        return myString().toUpperCase();
    }
}

public class Example {
    public static void main(String[] args) {
        var codec = ConfigBuilder.buildCodec(TestConfig.class);
        System.out.printf("Codec: %s%n", codec);
        var in1 = new TestConfig() {
            @Override
            public String myString() {
                return "test string";
            }

            @Override
            public List<Integer> myList() {
                return Arrays.asList(1, 2, 3);
            }
        };
        System.out.printf("In1: %s%n", in1);
        var result1 = codec.encodeStart(JsonOps.INSTANCE, in1);
        System.out.printf("Result1: %s%n", result1);
        var out1 = result1.getOrThrow(false, (s) -> {
            throw new IllegalStateException(s);
        });
        System.out.printf("Out1: %s%n", out1);
        var result2 = codec.decode(JsonOps.INSTANCE, out1);
        System.out.printf("Result2: %s%n", result2);
        var in2 = result2.getOrThrow(false, (s) -> {
            throw new IllegalStateException(s);
        }).getFirst();
        System.out.printf("In2: %s%n", in2);
    }
}
