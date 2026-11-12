package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec {@code calculo-de-imposto}: regra por categoria, origem da regra e teto. */
class CalculoPorCategoriaETetoTest {

    private final ServicoDeImposto servico = new ServicoDeImposto(new RepositorioDeRegrasEmMemoria());

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    /** Categoria A a 14% desde 2028-01-01 com teto de 1000.00, e P7 na categoria A. */
    private void categoriaAComTeto() {
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, valor("1000.00"));
        servico.atribuirCategoria("P7", "A");
    }

    // Requirement: Calcular imposto pela regra vigente (cenários novos)

    @Test
    @DisplayName("Regra de produto informa a origem")
    void regraDeProdutoInformaAOrigem() {
        servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), null);

        ResultadoImposto resultado = servico.calcular("P1", data("2026-05-01"), valor("100.00"));

        assertThat(resultado.origem()).isEqualTo(Alvo.Tipo.PRODUTO);
        assertThat(resultado.tetoAplicado()).isFalse();
    }

    @Test
    @DisplayName("Produto usa a regra da sua categoria")
    void produtoUsaARegraDaSuaCategoria() {
        servico.atribuirCategoria("P7", "A");
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);

        ResultadoImposto resultado = servico.calcular("P7", data("2028-03-10"), valor("5000.00"));

        assertThat(resultado.aliquota()).isEqualByComparingTo("14");
        assertThat(resultado.valorImposto()).isEqualTo(valor("700.00"));
        assertThat(resultado.origem()).isEqualTo(Alvo.Tipo.CATEGORIA);
    }

    @Test
    @DisplayName("Regra do produto prevalece sobre a da categoria")
    void regraDoProdutoPrevaleceSobreADaCategoria() {
        servico.atribuirCategoria("P8", "A");
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);
        servico.cadastrarRegra("P8", valor("5"), data("2028-01-01"), data("2028-06-30"));

        ResultadoImposto naRegraDoProduto = servico.calcular("P8", data("2028-03-10"), valor("100.00"));
        ResultadoImposto depoisDaRegraDoProduto = servico.calcular("P8", data("2028-07-01"), valor("100.00"));

        assertThat(naRegraDoProduto.aliquota()).isEqualByComparingTo("5");
        assertThat(naRegraDoProduto.valorImposto()).isEqualTo(valor("5.00"));
        assertThat(naRegraDoProduto.origem()).isEqualTo(Alvo.Tipo.PRODUTO);
        assertThat(depoisDaRegraDoProduto.aliquota()).isEqualByComparingTo("14");
        assertThat(depoisDaRegraDoProduto.valorImposto()).isEqualTo(valor("14.00"));
        assertThat(depoisDaRegraDoProduto.origem()).isEqualTo(Alvo.Tipo.CATEGORIA);
    }

    @Test
    @DisplayName("Troca de categoria muda a regra aplicada")
    void trocaDeCategoriaMudaARegraAplicada() {
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);
        servico.cadastrarRegraDeCategoria("B", valor("8"), data("2028-01-01"), null, null);
        servico.atribuirCategoria("P7", "A");

        servico.atribuirCategoria("P7", "B");
        ResultadoImposto resultado = servico.calcular("P7", data("2028-03-10"), valor("100.00"));

        assertThat(resultado.aliquota()).isEqualByComparingTo("8");
        assertThat(resultado.valorImposto()).isEqualTo(valor("8.00"));
    }

    // Requirement: Rejeitar cálculo sem regra vigente (cenários novos)

    @Test
    @DisplayName("Data antes da regra da categoria")
    void dataAntesDaRegraDaCategoria() {
        servico.atribuirCategoria("P7", "A");
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);

        ImpostoException erro = falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                () -> servico.calcular("P7", data("2027-12-31"), valor("100.00")));

        assertThat(erro).hasMessageContaining("P7").hasMessageContaining("2027-12-31");
    }

    @Test
    @DisplayName("Categoria sem regras")
    void categoriaSemRegras() {
        servico.atribuirCategoria("P7", "Z");

        ImpostoException erro = falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                () -> servico.calcular("P7", data("2028-01-01"), valor("100.00")));

        assertThat(erro).hasMessageContaining("P7").hasMessageContaining("2028-01-01");
    }

    // Requirement: Aplicar teto do imposto

    @Test
    @DisplayName("Imposto abaixo do teto")
    void impostoAbaixoDoTeto() {
        categoriaAComTeto();

        ResultadoImposto resultado = servico.calcular("P7", data("2028-03-10"), valor("5000.00"));

        assertThat(resultado.valorImposto()).isEqualTo(valor("700.00"));
        assertThat(resultado.tetoAplicado()).isFalse();
    }

    @Test
    @DisplayName("Imposto acima do teto")
    void impostoAcimaDoTeto() {
        categoriaAComTeto();

        ResultadoImposto resultado = servico.calcular("P7", data("2028-03-10"), valor("10000.00"));

        assertThat(resultado.aliquota()).isEqualByComparingTo("14");
        assertThat(resultado.valorImposto()).isEqualTo(valor("1000.00"));
        assertThat(resultado.tetoAplicado()).isTrue();
    }

    @Test
    @DisplayName("Imposto exatamente igual ao teto")
    void impostoExatamenteIgualAoTeto() {
        categoriaAComTeto();

        ResultadoImposto resultado = servico.calcular("P7", data("2028-03-10"), valor("7142.86"));

        assertThat(resultado.valorImposto()).isEqualTo(valor("1000.00"));
        assertThat(resultado.tetoAplicado()).isFalse();
    }

    @Test
    @DisplayName("Teto sem casas decimais")
    void tetoSemCasasDecimais() {
        servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), null, valor("50"));

        ResultadoImposto resultado = servico.calcular("P1", data("2026-05-01"), valor("1000.00"));

        assertThat(resultado.valorImposto()).isEqualTo(valor("50.00"));
        assertThat(resultado.tetoAplicado()).isTrue();
    }

    @Test
    @DisplayName("Teto não vale fora da sua regra")
    void tetoNaoValeForaDaSuaRegra() {
        servico.cadastrarRegraDeCategoria("A", valor("10"), data("2026-01-01"), data("2027-12-31"), null);
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, valor("1000.00"));
        servico.atribuirCategoria("P7", "A");

        ResultadoImposto resultado = servico.calcular("P7", data("2027-12-31"), valor("20000.00"));

        assertThat(resultado.valorImposto()).isEqualTo(valor("2000.00"));
        assertThat(resultado.tetoAplicado()).isFalse();
    }

    @Test
    @DisplayName("Teto da categoria não vale quando a regra do produto prevalece")
    void tetoDaCategoriaNaoValeQuandoARegraDoProdutoPrevalece() {
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, valor("1000.00"));
        servico.atribuirCategoria("P8", "A");
        servico.cadastrarRegra("P8", valor("14"), data("2028-01-01"), null);

        ResultadoImposto resultado = servico.calcular("P8", data("2028-03-10"), valor("10000.00"));

        assertThat(resultado.valorImposto()).isEqualTo(valor("1400.00"));
        assertThat(resultado.origem()).isEqualTo(Alvo.Tipo.PRODUTO);
        assertThat(resultado.tetoAplicado()).isFalse();
    }
}
