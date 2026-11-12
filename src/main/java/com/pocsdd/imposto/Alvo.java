package com.pocsdd.imposto;

import java.util.Objects;

/** A quem uma regra de alíquota se aplica: um produto ou uma categoria (códigos independentes entre si). */
public record Alvo(Tipo tipo, String codigo) {

    public Alvo {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(codigo, "codigo");
    }

    public enum Tipo {
        PRODUTO,
        CATEGORIA
    }

    public static Alvo produto(String codigo) {
        return new Alvo(Tipo.PRODUTO, codigo);
    }

    public static Alvo categoria(String codigo) {
        return new Alvo(Tipo.CATEGORIA, codigo);
    }

    @Override
    public String toString() {
        return (tipo == Tipo.PRODUTO ? "o produto " : "a categoria ") + codigo;
    }
}
