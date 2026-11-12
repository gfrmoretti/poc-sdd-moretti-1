package com.pocsdd.imposto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RepositorioDeCategoriasEmMemoriaTest {

    private final RepositorioDeCategoriasEmMemoria repositorio = new RepositorioDeCategoriasEmMemoria();

    @Test
    void produtoSemCategoriaRetornaVazio() {
        assertThat(repositorio.categoriaDe("P1")).isEmpty();
    }

    @Test
    void atribuirDefineACategoria() {
        repositorio.atribuir("P7", "A");

        assertThat(repositorio.categoriaDe("P7")).contains("A");
    }

    @Test
    void atribuirNovamenteSubstituiACategoria() {
        repositorio.atribuir("P7", "A");

        repositorio.atribuir("P7", "B");

        assertThat(repositorio.categoriaDe("P7")).contains("B");
    }

    @Test
    void categoriasSaoIsoladasPorProduto() {
        repositorio.atribuir("P7", "A");
        repositorio.atribuir("P8", "B");

        assertThat(repositorio.categoriaDe("P7")).contains("A");
        assertThat(repositorio.categoriaDe("P8")).contains("B");
    }
}
