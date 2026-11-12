package com.pocsdd.imposto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Implementação em memória; não é thread-safe. */
public final class RepositorioDeRegrasEmMemoria implements RepositorioDeRegras {

    private final Map<String, List<RegraDeAliquota>> regrasPorProduto = new HashMap<>();

    @Override
    public List<RegraDeAliquota> buscarPorProduto(String produto) {
        return List.copyOf(regrasPorProduto.getOrDefault(produto, List.of()));
    }

    @Override
    public void adicionar(RegraDeAliquota regra) {
        regrasPorProduto.computeIfAbsent(regra.produto(), p -> new ArrayList<>()).add(regra);
    }

    @Override
    public void substituir(RegraDeAliquota atual, RegraDeAliquota nova) {
        List<RegraDeAliquota> regras = regrasPorProduto.getOrDefault(atual.produto(), List.of());
        int indice = regras.indexOf(atual);
        if (indice < 0) {
            throw new IllegalArgumentException("Regra não encontrada no repositório: " + atual);
        }
        regras.set(indice, nova);
    }
}
