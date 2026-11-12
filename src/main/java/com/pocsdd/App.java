package com.pocsdd;

public final class App {

    private App() {
    }

    public static String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "world";
        System.out.println(greet(name));
    }
}
