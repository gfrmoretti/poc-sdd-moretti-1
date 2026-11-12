package com.pocsdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Functional test: exercises the application through its entry point. */
class AppIT {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void mainPrintsDefaultGreeting() {
        App.main(new String[0]);

        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualToIgnoringNewLines("Hello, world!");
    }

    @Test
    void mainPrintsGreetingForArgument() {
        App.main(new String[] {"SDD"});

        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualToIgnoringNewLines("Hello, SDD!");
    }
}
