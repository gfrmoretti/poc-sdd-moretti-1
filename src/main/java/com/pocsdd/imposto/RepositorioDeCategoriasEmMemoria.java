package com.pocsdd.imposto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Implementação em memória; não é thread-safe. */
public final class RepositorioDeCategoriasEmMemoria implements RepositorioDeCategorias {

    private final Map<String, String> categoriaPorProduto = new HashMap<>();

    @Override
    public Optional<String> categoriaDe(String produto) {
        return Optional.ofNullable(categoriaPorProduto.get(produto));
    }

    @Override
    public void atribuir(String produto, String categoria) {
        categoriaPorProduto.put(produto, categoria);
    }
}
