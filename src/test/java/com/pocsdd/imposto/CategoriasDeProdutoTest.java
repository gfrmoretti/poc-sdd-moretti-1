package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec {@code categorias-de-produto}. */
class CategoriasDeProdutoTest {

    private final RepositorioDeCategoriasEmMemoria categorias = new RepositorioDeCategoriasEmMemoria();
    private final ServicoDeImposto servico = new ServicoDeImposto(new RepositorioDeRegrasEmMemoria(), categorias);

    // Requirement: Atribuir categoria a um produto

    @Test
    @DisplayName("Atribuir categoria")
    void atribuirCategoria() {
        servico.atribuirCategoria("P7", "A");

        assertThat(categorias.categoriaDe("P7")).contains("A");
    }

    @Test
    @DisplayName("Trocar a categoria")
    void trocarACategoria() {
        servico.atribuirCategoria("P7", "A");

        servico.atribuirCategoria("P7", "B");

        assertThat(categorias.categoriaDe("P7")).contains("B");
    }

    @Test
    @DisplayName("Vários produtos na mesma categoria")
    void variosProdutosNaMesmaCategoria() {
        servico.atribuirCategoria("P7", "A");
        servico.atribuirCategoria("P8", "A");

        assertThat(categorias.categoriaDe("P7")).contains("A");
        assertThat(categorias.categoriaDe("P8")).contains("A");
    }

    @Test
    @DisplayName("Produto sem categoria")
    void produtoSemCategoria() {
        assertThat(categorias.categoriaDe("P1")).isEmpty();
    }

    // Requirement: Validar atribuição de categoria

    @Test
    @DisplayName("Categoria em branco")
    void categoriaEmBranco() {
        servico.atribuirCategoria("P7", "A");

        falhaCom(Motivo.CATEGORIA_INVALIDA, () -> servico.atribuirCategoria("P7", " "));

        assertThat(categorias.categoriaDe("P7")).contains("A");
    }

    @Test
    @DisplayName("Categoria ausente")
    void categoriaAusente() {
        servico.atribuirCategoria("P7", "A");

        falhaCom(Motivo.CATEGORIA_INVALIDA, () -> servico.atribuirCategoria("P7", null));

        assertThat(categorias.categoriaDe("P7")).contains("A");
    }

    @Test
    @DisplayName("Produto em branco")
    void produtoEmBranco() {
        falhaCom(Motivo.PRODUTO_INVALIDO, () -> servico.atribuirCategoria(" ", "A"));

        assertThat(categorias.categoriaDe(" ")).isEmpty();
    }
}
