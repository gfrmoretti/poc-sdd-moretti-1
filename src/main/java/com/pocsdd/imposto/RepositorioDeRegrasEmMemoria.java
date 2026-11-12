package com.pocsdd.imposto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Implementação em memória; não é thread-safe. */
public final class RepositorioDeRegrasEmMemoria implements RepositorioDeRegras {

    private final Map<Alvo, List<RegraDeAliquota>> regrasPorAlvo = new HashMap<>();

    @Override
    public List<RegraDeAliquota> buscarPorAlvo(Alvo alvo) {
        return List.copyOf(regrasPorAlvo.getOrDefault(alvo, List.of()));
    }

    @Override
    public void adicionar(RegraDeAliquota regra) {
        regrasPorAlvo.computeIfAbsent(regra.alvo(), a -> new ArrayList<>()).add(regra);
    }

    @Override
    public void substituir(RegraDeAliquota atual, RegraDeAliquota nova) {
        List<RegraDeAliquota> regras = regrasPorAlvo.getOrDefault(atual.alvo(), List.of());
        int indice = regras.indexOf(atual);
        if (indice < 0) {
            throw new IllegalArgumentException("Regra não encontrada no repositório: " + atual);
        }
        regras.set(indice, nova);
    }
}
