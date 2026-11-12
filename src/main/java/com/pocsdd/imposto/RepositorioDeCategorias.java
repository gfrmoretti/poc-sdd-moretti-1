package com.pocsdd.imposto;

import java.util.Optional;

/** Categoria de cada produto (no máximo uma). Não valida regras de negócio; isso é papel do serviço. */
public interface RepositorioDeCategorias {

    /** Categoria do produto, ou vazio se não tiver nenhuma. */
    Optional<String> categoriaDe(String produto);

    /** Define a categoria do produto, substituindo a anterior se houver. */
    void atribuir(String produto, String categoria);
}
