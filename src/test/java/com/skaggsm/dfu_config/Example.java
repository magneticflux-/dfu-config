package com.skaggsm.dfu_config;

import com.mojang.serialization.JsonOps;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

interface TestConfig1 {
    String myString();

    List<String> myList();

    default String myStringUpperCase() {
        return myString().toUpperCase();
    }
}

record TestConfig2(int myInt, String myString, List<Byte> myBytes) {
}

class TestConfig3 {
    public final String myString;
    public final List<String> myList;
    public final transient String myStringUppercase;

    public TestConfig3(String myString, List<String> myList) {
        this.myString = myString;
        this.myList = Collections.unmodifiableList(myList);
        this.myStringUppercase = myString.toUpperCase();
    }
}

record MinecraftTestConfig1(Identifier genericIdent, Item item, Block block) {
}

public class Example {
    public static void main(String[] args) {
        testRoundtrip(
                TestConfig1.class,
                new TestConfig1() {
                    @Override
                    public String myString() {
                        return "test string";
                    }

                    @Override
                    public List<String> myList() {
                        return Arrays.asList("1", "2", "3");
                    }
                }
        );

        testRoundtrip(
                TestConfig2.class,
                new TestConfig2(1, "testing!", Arrays.asList((byte) 1, (byte) 2, (byte) 3))
        );

        testRoundtrip(
                TestConfig3.class,
                new TestConfig3("testing!", Arrays.asList("a", "b", "c"))
        );

        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        testRoundtrip(
                MinecraftTestConfig1.class,
                new MinecraftTestConfig1(Identifier.tryParse("my_mod:test"), Items.WOODEN_AXE, Blocks.COAL_ORE)
        );
    }

    private static <T> void testRoundtrip(Class<T> superclass, T in1) {
        System.out.printf("Roundtrip for %s:%n", superclass.getSimpleName());
        var codec = ConfigBuilder.buildCodec(superclass);
        System.out.printf("\tCodec: %s%n", codec);
        System.out.printf("\tIn1: %s%n", in1);
        var result1 = codec.encodeStart(JsonOps.INSTANCE, in1);
        System.out.printf("\tResult1: %s%n", result1);
        var out1 = result1.getOrThrow(false, (s) -> {
            throw new IllegalStateException(s);
        });
        System.out.printf("\tOut1: %s%n", out1);
        var result2 = codec.decode(JsonOps.INSTANCE, out1);
        System.out.printf("\tResult2: %s%n", result2);
        var in2 = result2.getOrThrow(false, (s) -> {
            throw new IllegalStateException(s);
        }).getFirst();
        System.out.printf("\tIn2: %s%n", in2);
    }
}
