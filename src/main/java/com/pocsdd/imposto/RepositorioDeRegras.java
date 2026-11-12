package com.pocsdd.imposto;

import java.util.List;

/** Armazenamento das regras de alíquota. Não valida regras de negócio; isso é papel do {@link ServicoDeImposto}. */
public interface RepositorioDeRegras {

    /** Regras do alvo, ou lista vazia se não houver nenhuma. */
    List<RegraDeAliquota> buscarPorAlvo(Alvo alvo);

    /** Regras do produto, ou lista vazia se não houver nenhuma. */
    default List<RegraDeAliquota> buscarPorProduto(String produto) {
        return buscarPorAlvo(Alvo.produto(produto));
    }

    void adicionar(RegraDeAliquota regra);

    /** Troca {@code atual} por {@code nova}; {@code atual} deve existir no repositório. */
    void substituir(RegraDeAliquota atual, RegraDeAliquota nova);
}
