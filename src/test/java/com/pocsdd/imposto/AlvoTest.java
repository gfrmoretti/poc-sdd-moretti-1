package com.pocsdd.imposto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AlvoTest {

    @Test
    void tipoNuloERejeitado() {
        assertThatThrownBy(() -> new Alvo(null, "A")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void codigoNuloERejeitado() {
        assertThatThrownBy(() -> Alvo.produto(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Alvo.categoria(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void produtoECategoriaDeMesmoCodigoSaoDiferentes() {
        assertThat(Alvo.produto("A")).isNotEqualTo(Alvo.categoria("A"));
    }

    @Test
    void descricaoUsaArtigoDoTipo() {
        assertThat(Alvo.produto("P9")).hasToString("o produto P9");
        assertThat(Alvo.categoria("Z")).hasToString("a categoria Z");
    }
}
