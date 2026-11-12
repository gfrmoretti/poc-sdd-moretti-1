package com.pocsdd.imposto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RepositorioDeRegrasEmMemoriaTest {

    private final RepositorioDeRegrasEmMemoria repositorio = new RepositorioDeRegrasEmMemoria();

    private static RegraDeAliquota regra(String produto, String de) {
        return new RegraDeAliquota(produto, BigDecimal.TEN, LocalDate.parse(de), null);
    }

    @Test
    void produtoSemRegrasRetornaListaVazia() {
        assertThat(repositorio.buscarPorProduto("P9")).isEmpty();
    }

    @Test
    void regrasSaoIsoladasPorProduto() {
        RegraDeAliquota p1 = regra("P1", "2026-01-01");
        RegraDeAliquota p2 = regra("P2", "2026-01-01");

        repositorio.adicionar(p1);
        repositorio.adicionar(p2);

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(p1);
        assertThat(repositorio.buscarPorProduto("P2")).containsExactly(p2);
    }

    @Test
    void substituirTrocaApenasARegraIndicada() {
        RegraDeAliquota primeira = new RegraDeAliquota("P1", BigDecimal.TEN, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-06-30"));
        RegraDeAliquota aberta = regra("P1", "2026-07-01");
        RegraDeAliquota outroProduto = regra("P2", "2026-07-01");
        repositorio.adicionar(primeira);
        repositorio.adicionar(aberta);
        repositorio.adicionar(outroProduto);

        RegraDeAliquota encerrada = aberta.encerradaEm(LocalDate.parse("2026-12-31"));
        repositorio.substituir(aberta, encerrada);

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(primeira, encerrada);
        assertThat(repositorio.buscarPorProduto("P2")).containsExactly(outroProduto);
    }

    @Test
    void substituirRegraInexistenteFalha() {
        RegraDeAliquota inexistente = regra("P1", "2026-01-01");

        assertThatThrownBy(() -> repositorio.substituir(inexistente, inexistente))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listaRetornadaNaoAlteraORepositorio() {
        repositorio.adicionar(regra("P1", "2026-01-01"));

        assertThatThrownBy(() -> repositorio.buscarPorProduto("P1").clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(repositorio.buscarPorProduto("P1")).hasSize(1);
    }
}
