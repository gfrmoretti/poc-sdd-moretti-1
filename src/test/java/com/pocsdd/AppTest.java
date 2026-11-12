package com.pocsdd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void greetsByName() {
        assertThat(App.greet("SDD")).isEqualTo("Hello, SDD!");
    }
}
