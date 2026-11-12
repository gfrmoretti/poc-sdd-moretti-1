package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec {@code cadastro-de-aliquotas}, requisito "Encerrar vigência em aberto". */
class EncerramentoDeVigenciaTest {

    private final RepositorioDeRegrasEmMemoria repositorio = new RepositorioDeRegrasEmMemoria();
    private final ServicoDeImposto servico = new ServicoDeImposto(repositorio);

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    @Test
    @DisplayName("Encerrar e cadastrar a alíquota seguinte")
    void encerrarECadastrarAAliquotaSeguinte() {
        servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"), null);

        RegraDeAliquota encerrada = servico.encerrarVigencia("P1", data("2026-06-30"));
        RegraDeAliquota seguinte = servico.cadastrarRegra("P1", new BigDecimal("12"), data("2026-07-01"), null);

        assertThat(encerrada.vigenteAte()).isEqualTo(data("2026-06-30"));
        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(encerrada, seguinte);
        assertThat(encerrada.vigenteEm(data("2026-06-30"))).isTrue();
        assertThat(encerrada.aliquota()).isEqualByComparingTo("10");
        assertThat(seguinte.vigenteEm(data("2026-07-01"))).isTrue();
        assertThat(seguinte.aliquota()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("Encerrar no próprio dia de início")
    void encerrarNoProprioDiaDeInicio() {
        servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-07-01"), null);

        RegraDeAliquota encerrada = servico.encerrarVigencia("P1", data("2026-07-01"));

        assertThat(encerrada.vigenteAte()).isEqualTo(data("2026-07-01"));
    }

    @Test
    @DisplayName("Produto sem regra em aberto")
    void produtoSemRegraEmAberto() {
        RegraDeAliquota fechada = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"),
                data("2026-06-30"));

        falhaCom(Motivo.SEM_REGRA_EM_ABERTO, () -> servico.encerrarVigencia("P1", data("2026-12-31")));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(fechada);
    }

    @Test
    @DisplayName("Produto sem regras")
    void produtoSemRegras() {
        falhaCom(Motivo.SEM_REGRA_EM_ABERTO, () -> servico.encerrarVigencia("P9", data("2026-12-31")));

        assertThat(repositorio.buscarPorProduto("P9")).isEmpty();
    }

    @Test
    @DisplayName("Data de fim anterior ao início da regra")
    void dataDeFimAnteriorAoInicioDaRegra() {
        RegraDeAliquota aberta = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-07-01"), null);

        falhaCom(Motivo.PERIODO_INVALIDO, () -> servico.encerrarVigencia("P1", data("2026-06-30")));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(aberta);
        assertThat(repositorio.buscarPorProduto("P1").getFirst().emAberto()).isTrue();
    }

    @Test
    @DisplayName("Data de fim ausente")
    void dataDeFimAusente() {
        RegraDeAliquota aberta = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-07-01"), null);

        falhaCom(Motivo.PERIODO_INVALIDO, () -> servico.encerrarVigencia("P1", null));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(aberta);
    }
}
