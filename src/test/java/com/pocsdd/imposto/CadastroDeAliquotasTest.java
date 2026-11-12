package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec {@code cadastro-de-aliquotas}: cadastro, validação e sobreposição de vigência. */
class CadastroDeAliquotasTest {

    private final RepositorioDeRegrasEmMemoria repositorio = new RepositorioDeRegrasEmMemoria();
    private final ServicoDeImposto servico = new ServicoDeImposto(repositorio);

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    // Requirement: Cadastrar regra de alíquota

    @Test
    @DisplayName("Regra com período fechado")
    void regraComPeriodoFechado() {
        RegraDeAliquota regra = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"),
                data("2026-06-30"));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(regra);
        assertThat(regra.vigenteEm(data("2026-01-01"))).isTrue();
        assertThat(regra.vigenteEm(data("2026-03-15"))).isTrue();
        assertThat(regra.vigenteEm(data("2026-06-30"))).isTrue();
    }

    @Test
    @DisplayName("Regra em aberto")
    void regraEmAberto() {
        RegraDeAliquota regra = servico.cadastrarRegra("P1", new BigDecimal("12"), data("2026-07-01"), null);

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(regra);
        assertThat(regra.emAberto()).isTrue();
        assertThat(regra.vigenteEm(data("2026-07-01"))).isTrue();
        assertThat(regra.vigenteEm(data("2099-12-31"))).isTrue();
    }

    @Test
    @DisplayName("Alíquota com casas decimais")
    void aliquotaComCasasDecimais() {
        RegraDeAliquota regra = servico.cadastrarRegra("P2", new BigDecimal("7.5"), data("2026-01-01"), null);

        assertThat(regra.aliquota()).isEqualByComparingTo("7.5");
        assertThat(repositorio.buscarPorProduto("P2")).containsExactly(regra);
    }

    @Test
    @DisplayName("Alíquota zero")
    void aliquotaZero() {
        RegraDeAliquota regra = servico.cadastrarRegra("P3", BigDecimal.ZERO, data("2026-01-01"), null);

        assertThat(regra.aliquota()).isEqualByComparingTo("0");
        assertThat(repositorio.buscarPorProduto("P3")).containsExactly(regra);
    }

    // Requirement: Validar dados da regra

    @Test
    @DisplayName("Alíquota negativa")
    void aliquotaNegativa() {
        falhaCom(Motivo.ALIQUOTA_INVALIDA,
                () -> servico.cadastrarRegra("P1", new BigDecimal("-1"), data("2026-01-01"), null));

        assertThat(repositorio.buscarPorProduto("P1")).isEmpty();
    }

    @Test
    @DisplayName("Alíquota ausente")
    void aliquotaAusente() {
        falhaCom(Motivo.ALIQUOTA_INVALIDA, () -> servico.cadastrarRegra("P1", null, data("2026-01-01"), null));

        assertThat(repositorio.buscarPorProduto("P1")).isEmpty();
    }

    @Test
    @DisplayName("Fim anterior ao início")
    void fimAnteriorAoInicio() {
        falhaCom(Motivo.PERIODO_INVALIDO,
                () -> servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-06-30"), data("2026-01-01")));

        assertThat(repositorio.buscarPorProduto("P1")).isEmpty();
    }

    @Test
    @DisplayName("Início igual ao fim")
    void inicioIgualAoFim() {
        RegraDeAliquota regra = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-03-15"),
                data("2026-03-15"));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(regra);
        assertThat(regra.vigenteEm(data("2026-03-15"))).isTrue();
        assertThat(regra.vigenteEm(data("2026-03-14"))).isFalse();
        assertThat(regra.vigenteEm(data("2026-03-16"))).isFalse();
    }

    @Test
    @DisplayName("Produto em branco")
    void produtoEmBranco() {
        falhaCom(Motivo.PRODUTO_INVALIDO,
                () -> servico.cadastrarRegra("  ", new BigDecimal("10"), data("2026-01-01"), null));

        assertThat(repositorio.buscarPorProduto("  ")).isEmpty();
    }

    @Test
    @DisplayName("Produto ausente")
    void produtoAusente() {
        falhaCom(Motivo.PRODUTO_INVALIDO,
                () -> servico.cadastrarRegra(null, new BigDecimal("10"), data("2026-01-01"), null));
    }

    @Test
    @DisplayName("Data de início ausente")
    void dataDeInicioAusente() {
        falhaCom(Motivo.PERIODO_INVALIDO, () -> servico.cadastrarRegra("P1", new BigDecimal("10"), null, null));

        assertThat(repositorio.buscarPorProduto("P1")).isEmpty();
    }

    // Requirement: Proibir sobreposição de vigência

    @Test
    @DisplayName("Sobreposição parcial")
    void sobreposicaoParcial() {
        RegraDeAliquota existente = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"),
                data("2026-06-30"));

        falhaCom(Motivo.SOBREPOSICAO_DE_VIGENCIA,
                () -> servico.cadastrarRegra("P1", new BigDecimal("12"), data("2026-06-01"), data("2026-12-31")));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(existente);
    }

    @Test
    @DisplayName("Sobreposição no dia de fronteira")
    void sobreposicaoNoDiaDeFronteira() {
        RegraDeAliquota existente = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"),
                data("2026-06-30"));

        falhaCom(Motivo.SOBREPOSICAO_DE_VIGENCIA,
                () -> servico.cadastrarRegra("P1", new BigDecimal("12"), data("2026-06-30"), null));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(existente);
    }

    @Test
    @DisplayName("Períodos contíguos")
    void periodosContiguos() {
        RegraDeAliquota primeira = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"),
                data("2026-06-30"));

        RegraDeAliquota seguinte = servico.cadastrarRegra("P1", new BigDecimal("12"), data("2026-07-01"), null);

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(primeira, seguinte);
    }

    @Test
    @DisplayName("Conflito com regra em aberto")
    void conflitoComRegraEmAberto() {
        RegraDeAliquota existente = servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"), null);

        falhaCom(Motivo.SOBREPOSICAO_DE_VIGENCIA,
                () -> servico.cadastrarRegra("P1", new BigDecimal("12"), data("2027-01-01"), data("2027-12-31")));

        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(existente);
    }

    @Test
    @DisplayName("Produtos diferentes no mesmo período")
    void produtosDiferentesNoMesmoPeriodo() {
        servico.cadastrarRegra("P1", new BigDecimal("10"), data("2026-01-01"), data("2026-12-31"));

        RegraDeAliquota p2 = servico.cadastrarRegra("P2", new BigDecimal("10"), data("2026-01-01"),
                data("2026-12-31"));

        assertThat(repositorio.buscarPorProduto("P2")).containsExactly(p2);
    }
}
