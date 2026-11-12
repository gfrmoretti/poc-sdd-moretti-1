package com.pocsdd.imposto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RegraDeAliquotaTest {

    private static RegraDeAliquota regra(String produto, String de, String ate) {
        LocalDate fim = ate == null ? null : LocalDate.parse(ate);
        return new RegraDeAliquota(produto, BigDecimal.TEN, LocalDate.parse(de), fim);
    }

    @Test
    void fronteirasSaoInclusivas() {
        RegraDeAliquota regra = regra("P1", "2026-01-01", "2026-06-30");

        assertThat(regra.vigenteEm(LocalDate.parse("2026-01-01"))).isTrue();
        assertThat(regra.vigenteEm(LocalDate.parse("2026-03-15"))).isTrue();
        assertThat(regra.vigenteEm(LocalDate.parse("2026-06-30"))).isTrue();
        assertThat(regra.vigenteEm(LocalDate.parse("2025-12-31"))).isFalse();
        assertThat(regra.vigenteEm(LocalDate.parse("2026-07-01"))).isFalse();
    }

    @Test
    void regraEmAbertoValeIndefinidamente() {
        RegraDeAliquota regra = regra("P1", "2026-07-01", null);

        assertThat(regra.emAberto()).isTrue();
        assertThat(regra.vigenteEm(LocalDate.parse("2026-07-01"))).isTrue();
        assertThat(regra.vigenteEm(LocalDate.parse("2030-01-01"))).isTrue();
        assertThat(regra.vigenteEm(LocalDate.parse("2026-06-30"))).isFalse();
    }

    @Test
    void periodosContiguosNaoSeSobrepoem() {
        RegraDeAliquota primeira = regra("P1", "2026-01-01", "2026-06-30");
        RegraDeAliquota seguinte = regra("P1", "2026-07-01", null);

        assertThat(primeira.sobrepoe(seguinte)).isFalse();
        assertThat(seguinte.sobrepoe(primeira)).isFalse();
    }

    @Test
    void sobreposicaoNoDiaDeFronteira() {
        RegraDeAliquota primeira = regra("P1", "2026-01-01", "2026-06-30");
        RegraDeAliquota seguinte = regra("P1", "2026-06-30", null);

        assertThat(primeira.sobrepoe(seguinte)).isTrue();
        assertThat(seguinte.sobrepoe(primeira)).isTrue();
    }

    @Test
    void regraEmAbertoSobrepoeQualquerPeriodoPosterior() {
        RegraDeAliquota aberta = regra("P1", "2026-01-01", null);
        RegraDeAliquota futura = regra("P1", "2027-01-01", "2027-12-31");

        assertThat(aberta.sobrepoe(futura)).isTrue();
        assertThat(futura.sobrepoe(aberta)).isTrue();
    }

    @Test
    void produtosDiferentesNuncaSeSobrepoem() {
        assertThat(regra("P1", "2026-01-01", "2026-12-31").sobrepoe(regra("P2", "2026-01-01", "2026-12-31")))
                .isFalse();
    }

    @Test
    void encerradaEmDefineDataDeFimSemAlterarOriginal() {
        RegraDeAliquota aberta = regra("P1", "2026-01-01", null);

        RegraDeAliquota encerrada = aberta.encerradaEm(LocalDate.parse("2026-06-30"));

        assertThat(encerrada.emAberto()).isFalse();
        assertThat(encerrada.vigenteAte()).isEqualTo(LocalDate.parse("2026-06-30"));
        assertThat(encerrada.vigenteDe()).isEqualTo(aberta.vigenteDe());
        assertThat(aberta.emAberto()).isTrue();
    }

    @Test
    void produtoECategoriaDeMesmoCodigoNaoSeSobrepoem() {
        RegraDeAliquota doProduto = new RegraDeAliquota(Alvo.produto("A"), BigDecimal.TEN,
                LocalDate.parse("2028-01-01"), null, null);
        RegraDeAliquota daCategoria = new RegraDeAliquota(Alvo.categoria("A"), BigDecimal.TEN,
                LocalDate.parse("2028-01-01"), null, null);

        assertThat(doProduto.sobrepoe(daCategoria)).isFalse();
        assertThat(daCategoria.sobrepoe(doProduto)).isFalse();
    }

    @Test
    void construtorDeProdutoCriaRegraDeProdutoSemTeto() {
        RegraDeAliquota regra = regra("P1", "2026-01-01", null);

        assertThat(regra.alvo()).isEqualTo(Alvo.produto("P1"));
        assertThat(regra.teto()).isNull();
    }

    @Test
    void encerradaEmMantemAlvoETeto() {
        RegraDeAliquota aberta = new RegraDeAliquota(Alvo.categoria("A"), new BigDecimal("14"),
                LocalDate.parse("2028-01-01"), null, new BigDecimal("1000.00"));

        RegraDeAliquota encerrada = aberta.encerradaEm(LocalDate.parse("2028-12-31"));

        assertThat(encerrada.alvo()).isEqualTo(Alvo.categoria("A"));
        assertThat(encerrada.teto()).isEqualByComparingTo("1000.00");
        assertThat(encerrada.vigenteAte()).isEqualTo(LocalDate.parse("2028-12-31"));
    }
}
