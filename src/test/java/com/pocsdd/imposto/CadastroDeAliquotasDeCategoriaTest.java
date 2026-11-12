package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec {@code cadastro-de-aliquotas}: regras de categoria e teto. */
class CadastroDeAliquotasDeCategoriaTest {

    private final RepositorioDeRegrasEmMemoria repositorio = new RepositorioDeRegrasEmMemoria();
    private final ServicoDeImposto servico = new ServicoDeImposto(repositorio);

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    private List<RegraDeAliquota> regrasDaCategoria(String categoria) {
        return repositorio.buscarPorAlvo(Alvo.categoria(categoria));
    }

    // Requirement: Cadastrar regra de alíquota para categoria

    @Test
    @DisplayName("Regra de categoria em aberto")
    void regraDeCategoriaEmAberto() {
        RegraDeAliquota regra = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);

        assertThat(regrasDaCategoria("A")).containsExactly(regra);
        assertThat(regra.alvo()).isEqualTo(Alvo.categoria("A"));
        assertThat(regra.vigenteEm(data("2028-01-01"))).isTrue();
        assertThat(regra.vigenteEm(data("2099-12-31"))).isTrue();
    }

    @Test
    @DisplayName("Categoria em branco")
    void categoriaEmBranco() {
        falhaCom(Motivo.CATEGORIA_INVALIDA,
                () -> servico.cadastrarRegraDeCategoria(" ", valor("14"), data("2028-01-01"), null, null));

        assertThat(regrasDaCategoria(" ")).isEmpty();
    }

    @Test
    @DisplayName("Categoria ausente")
    void categoriaAusente() {
        falhaCom(Motivo.CATEGORIA_INVALIDA,
                () -> servico.cadastrarRegraDeCategoria(null, valor("14"), data("2028-01-01"), null, null));
    }

    @Test
    @DisplayName("Dados inválidos na regra de categoria")
    void dadosInvalidosNaRegraDeCategoria() {
        falhaCom(Motivo.ALIQUOTA_INVALIDA,
                () -> servico.cadastrarRegraDeCategoria("A", valor("-1"), data("2028-01-01"), null, null));
        falhaCom(Motivo.PERIODO_INVALIDO,
                () -> servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-06-30"), data("2028-01-01"),
                        null));

        assertThat(regrasDaCategoria("A")).isEmpty();
    }

    // Requirement: Proibir sobreposição de vigência entre regras da mesma categoria

    @Test
    @DisplayName("Sobreposição entre regras da mesma categoria")
    void sobreposicaoEntreRegrasDaMesmaCategoria() {
        RegraDeAliquota existente = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                null);

        falhaCom(Motivo.SOBREPOSICAO_DE_VIGENCIA,
                () -> servico.cadastrarRegraDeCategoria("A", valor("15"), data("2029-01-01"), data("2029-12-31"),
                        null));

        assertThat(regrasDaCategoria("A")).containsExactly(existente);
    }

    @Test
    @DisplayName("Regra de categoria e regra de produto no mesmo período")
    void regraDeCategoriaERegraDeProdutoNoMesmoPeriodo() {
        RegraDeAliquota doProduto = servico.cadastrarRegra("P7", valor("5"), data("2028-01-01"), null);

        RegraDeAliquota daCategoria = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                null);

        assertThat(repositorio.buscarPorProduto("P7")).containsExactly(doProduto);
        assertThat(regrasDaCategoria("A")).containsExactly(daCategoria);
    }

    @Test
    @DisplayName("Categorias diferentes no mesmo período")
    void categoriasDiferentesNoMesmoPeriodo() {
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);

        RegraDeAliquota daB = servico.cadastrarRegraDeCategoria("B", valor("8"), data("2028-01-01"), null, null);

        assertThat(regrasDaCategoria("B")).containsExactly(daB);
    }

    // Requirement: Encerrar vigência em aberto de categoria

    @Test
    @DisplayName("Encerrar e cadastrar a alíquota seguinte da categoria")
    void encerrarECadastrarAAliquotaSeguinteDaCategoria() {
        servico.cadastrarRegraDeCategoria("A", valor("10"), data("2026-01-01"), null, null);

        RegraDeAliquota encerrada = servico.encerrarVigenciaDeCategoria("A", data("2027-12-31"));
        RegraDeAliquota seguinte = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                null);

        assertThat(encerrada.vigenteAte()).isEqualTo(data("2027-12-31"));
        assertThat(regrasDaCategoria("A")).containsExactly(encerrada, seguinte);
    }

    @Test
    @DisplayName("Categoria sem regra em aberto")
    void categoriaSemRegraEmAberto() {
        falhaCom(Motivo.SEM_REGRA_EM_ABERTO, () -> servico.encerrarVigenciaDeCategoria("Z", data("2028-12-31")));
    }

    @Test
    @DisplayName("Categoria sem regras: mensagem identifica a categoria")
    void categoriaSemRegrasMensagemIdentificaACategoria() {
        ImpostoException erro = falhaCom(Motivo.SEM_REGRA_EM_ABERTO,
                () -> servico.encerrarVigenciaDeCategoria("Z", data("2028-12-31")));

        assertThat(erro).hasMessageContaining("a categoria Z");
    }

    @Test
    @DisplayName("Categoria em branco ao encerrar")
    void categoriaEmBrancoAoEncerrar() {
        falhaCom(Motivo.CATEGORIA_INVALIDA, () -> servico.encerrarVigenciaDeCategoria(" ", data("2028-12-31")));
    }

    @Test
    @DisplayName("Categoria ausente ao encerrar")
    void categoriaAusenteAoEncerrar() {
        falhaCom(Motivo.CATEGORIA_INVALIDA, () -> servico.encerrarVigenciaDeCategoria(null, data("2028-12-31")));
    }

    @Test
    @DisplayName("Encerrar categoria não afeta regra em aberto de produto com o mesmo código")
    void encerrarCategoriaNaoAfetaProdutoDeMesmoCodigo() {
        RegraDeAliquota doProduto = servico.cadastrarRegra("A", valor("5"), data("2028-01-01"), null);

        falhaCom(Motivo.SEM_REGRA_EM_ABERTO, () -> servico.encerrarVigenciaDeCategoria("A", data("2028-12-31")));

        assertThat(repositorio.buscarPorProduto("A")).containsExactly(doProduto);
    }

    @Test
    @DisplayName("Data de fim anterior ao início da regra da categoria")
    void dataDeFimAnteriorAoInicioDaRegraDaCategoria() {
        RegraDeAliquota aberta = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, null);

        falhaCom(Motivo.PERIODO_INVALIDO, () -> servico.encerrarVigenciaDeCategoria("A", data("2027-12-31")));

        assertThat(regrasDaCategoria("A")).containsExactly(aberta);
        assertThat(regrasDaCategoria("A").getFirst().emAberto()).isTrue();
    }

    // Requirement: Definir teto do imposto na regra

    @Test
    @DisplayName("Regra de categoria com teto")
    void regraDeCategoriaComTeto() {
        RegraDeAliquota regra = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                valor("1000.00"));

        assertThat(regra.teto()).isEqualByComparingTo("1000.00");
        assertThat(regrasDaCategoria("A")).containsExactly(regra);
    }

    @Test
    @DisplayName("Regra de produto com teto")
    void regraDeProdutoComTeto() {
        RegraDeAliquota regra = servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), null, valor("50"));

        assertThat(regra.teto()).isEqualByComparingTo("50.00");
        assertThat(repositorio.buscarPorProduto("P1")).containsExactly(regra);
    }

    @Test
    @DisplayName("Teto zero")
    void tetoZero() {
        falhaCom(Motivo.TETO_INVALIDO,
                () -> servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, valor("0")));

        assertThat(regrasDaCategoria("A")).isEmpty();
    }

    @Test
    @DisplayName("Teto negativo")
    void tetoNegativo() {
        falhaCom(Motivo.TETO_INVALIDO,
                () -> servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), null, valor("-1")));

        assertThat(repositorio.buscarPorProduto("P1")).isEmpty();
    }

    @Test
    @DisplayName("Teto com mais de 2 casas decimais")
    void tetoComMaisDe2CasasDecimais() {
        falhaCom(Motivo.TETO_INVALIDO,
                () -> servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                        valor("1000.001")));

        assertThat(regrasDaCategoria("A")).isEmpty();
    }

    @Test
    @DisplayName("Teto com zeros à direita além de 2 casas é aceito")
    void tetoComZerosADireitaAlemDe2CasasEAceito() {
        RegraDeAliquota regra = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                valor("1000.000"));

        assertThat(regra.teto()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Teto é registrado com exatamente 2 casas decimais")
    void tetoERegistradoComExatamente2CasasDecimais() {
        RegraDeAliquota semCasas = servico.cadastrarRegra("P1", valor("10"), data("2026-01-01"), null, valor("50"));
        RegraDeAliquota comZerosExtras = servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null,
                valor("1000.000"));

        assertThat(semCasas.teto()).isEqualTo(valor("50.00"));
        assertThat(comZerosExtras.teto()).isEqualTo(valor("1000.00"));
        assertThat(repositorio.buscarPorProduto("P1").getFirst().teto()).isEqualTo(valor("50.00"));
    }

    @Test
    @DisplayName("Encerrar vigência mantém o teto")
    void encerrarVigenciaMantemOTeto() {
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, valor("1000.00"));

        RegraDeAliquota encerrada = servico.encerrarVigenciaDeCategoria("A", data("2028-12-31"));

        assertThat(encerrada.teto()).isEqualByComparingTo("1000.00");
        assertThat(regrasDaCategoria("A")).containsExactly(encerrada);
    }
}
