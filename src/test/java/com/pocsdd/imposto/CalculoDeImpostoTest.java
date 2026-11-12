package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec {@code calculo-de-imposto}. */
class CalculoDeImpostoTest {

    private final ServicoDeImposto servico = new ServicoDeImposto(new RepositorioDeRegrasEmMemoria());

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    /** Cadastra a alíquota para o produto em aberto desde 2026-01-01, para os cenários de "alíquota vigente". */
    private void aliquotaVigente(String produto, String aliquota) {
        servico.cadastrarRegra(produto, valor(aliquota), data("2026-01-01"), null);
    }

    private BigDecimal imposto(String produto, String valorBase) {
        return servico.calcular(produto, data("2026-05-01"), valor(valorBase)).valorImposto();
    }

    // Requirement: Calcular imposto pela regra vigente

    @Test
    @DisplayName("Data dentro de período fechado")
    void dataDentroDePeriodoFechado() {
        servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), data("2026-06-30"));

        ResultadoImposto resultado = servico.calcular("P1", data("2026-03-15"), valor("100.00"));

        assertThat(resultado.aliquota()).isEqualByComparingTo("10");
        assertThat(resultado.valorImposto()).isEqualTo(valor("10.00"));
    }

    @Test
    @DisplayName("Data no limite do período")
    void dataNoLimiteDoPeriodo() {
        servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), data("2026-06-30"));
        servico.cadastrarRegra("P1", valor("12"), data("2026-07-01"), null);

        ResultadoImposto ultimoDia = servico.calcular("P1", data("2026-06-30"), valor("100.00"));
        ResultadoImposto primeiroDia = servico.calcular("P1", data("2026-07-01"), valor("100.00"));

        assertThat(ultimoDia.aliquota()).isEqualByComparingTo("10");
        assertThat(ultimoDia.valorImposto()).isEqualTo(valor("10.00"));
        assertThat(primeiroDia.aliquota()).isEqualByComparingTo("12");
        assertThat(primeiroDia.valorImposto()).isEqualTo(valor("12.00"));
    }

    @Test
    @DisplayName("Data distante numa regra em aberto")
    void dataDistanteNumaRegraEmAberto() {
        servico.cadastrarRegra("P1", valor("12"), data("2026-07-01"), null);

        ResultadoImposto resultado = servico.calcular("P1", data("2030-01-01"), valor("100.00"));

        assertThat(resultado.aliquota()).isEqualByComparingTo("12");
        assertThat(resultado.valorImposto()).isEqualTo(valor("12.00"));
    }

    @Test
    @DisplayName("Produto isento")
    void produtoIsento() {
        servico.cadastrarRegra("P3", valor("0"), data("2026-01-01"), null);

        ResultadoImposto resultado = servico.calcular("P3", data("2026-05-01"), valor("100.00"));

        assertThat(resultado.aliquota()).isEqualByComparingTo("0");
        assertThat(resultado.valorImposto()).isEqualTo(valor("0.00"));
    }

    // Requirement: Arredondar o valor do imposto

    @Test
    @DisplayName("Arredondamento para cima na metade")
    void arredondamentoParaCimaNaMetade() {
        aliquotaVigente("P1", "10");

        assertThat(imposto("P1", "0.05")).isEqualTo(valor("0.01"));
    }

    @Test
    @DisplayName("Arredondamento para cima acima da metade")
    void arredondamentoParaCimaAcimaDaMetade() {
        aliquotaVigente("P1", "12");

        assertThat(imposto("P1", "10.05")).isEqualTo(valor("1.21"));
    }

    @Test
    @DisplayName("Arredondamento para baixo")
    void arredondamentoParaBaixo() {
        aliquotaVigente("P1", "12");

        assertThat(imposto("P1", "10.04")).isEqualTo(valor("1.20"));
    }

    @Test
    @DisplayName("Alíquota decimal")
    void aliquotaDecimal() {
        aliquotaVigente("P2", "7.5");

        assertThat(imposto("P2", "33.33")).isEqualTo(valor("2.50"));
    }

    @Test
    @DisplayName("Valor-base zero")
    void valorBaseZero() {
        aliquotaVigente("P1", "12");

        assertThat(imposto("P1", "0")).isEqualTo(valor("0.00"));
    }

    // Requirement: Rejeitar cálculo sem regra vigente

    @Test
    @DisplayName("Data antes da primeira regra")
    void dataAntesDaPrimeiraRegra() {
        servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), null);

        ImpostoException erro = falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                () -> servico.calcular("P1", data("2025-12-31"), valor("100.00")));

        assertThat(erro).hasMessageContaining("P1").hasMessageContaining("2025-12-31");
    }

    @Test
    @DisplayName("Data num intervalo sem regra")
    void dataNumIntervaloSemRegra() {
        servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), data("2026-06-30"));
        servico.cadastrarRegra("P1", valor("12"), data("2026-09-01"), null);

        ImpostoException erro = falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                () -> servico.calcular("P1", data("2026-08-01"), valor("100.00")));

        assertThat(erro).hasMessageContaining("P1").hasMessageContaining("2026-08-01");
    }

    @Test
    @DisplayName("Produto sem regras")
    void produtoSemRegras() {
        ImpostoException erro = falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                () -> servico.calcular("P9", data("2026-01-01"), valor("100.00")));

        assertThat(erro).hasMessageContaining("P9").hasMessageContaining("2026-01-01");
    }

    // Requirement: Validar entrada do cálculo

    @Test
    @DisplayName("Valor-base negativo")
    void valorBaseNegativo() {
        aliquotaVigente("P1", "12");

        falhaCom(Motivo.VALOR_BASE_INVALIDO, () -> servico.calcular("P1", data("2026-05-01"), valor("-1.00")));
    }

    @Test
    @DisplayName("Valor-base ausente")
    void valorBaseAusente() {
        aliquotaVigente("P1", "12");

        falhaCom(Motivo.VALOR_BASE_INVALIDO, () -> servico.calcular("P1", data("2026-05-01"), null));
    }

    @Test
    @DisplayName("Data ausente")
    void dataAusente() {
        aliquotaVigente("P1", "12");

        falhaCom(Motivo.DATA_INVALIDA, () -> servico.calcular("P1", null, valor("100.00")));
    }

    @Test
    @DisplayName("Produto em branco")
    void produtoEmBranco() {
        falhaCom(Motivo.PRODUTO_INVALIDO, () -> servico.calcular(" ", data("2026-05-01"), valor("100.00")));
    }

    @Test
    @DisplayName("Produto ausente")
    void produtoAusente() {
        falhaCom(Motivo.PRODUTO_INVALIDO, () -> servico.calcular(null, data("2026-05-01"), valor("100.00")));
    }
}
