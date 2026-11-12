package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Functional test: categoria A passa a 14% com teto de R$ 1000 a partir de 2028-01-01, pela API pública. */
class ImpostoPorCategoriaIT {

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void categoriaAComTetoAPartirDe2028() {
        ServicoDeImposto servico = new ServicoDeImposto(new RepositorioDeRegrasEmMemoria(),
                new RepositorioDeCategoriasEmMemoria());
        servico.atribuirCategoria("P7", "A");
        servico.atribuirCategoria("P8", "A");
        servico.cadastrarRegraDeCategoria("A", valor("10"), data("2026-01-01"), null, null);
        servico.cadastrarRegra("P8", valor("5"), data("2028-01-01"), data("2028-06-30"));

        servico.encerrarVigenciaDeCategoria("A", data("2027-12-31"));
        servico.cadastrarRegraDeCategoria("A", valor("14"), data("2028-01-01"), null, valor("1000.00"));

        ResultadoImposto antesDe2028 = servico.calcular("P7", data("2027-12-31"), valor("20000.00"));
        assertThat(antesDe2028.aliquota()).isEqualByComparingTo("10");
        assertThat(antesDe2028.valorImposto()).isEqualTo(valor("2000.00"));
        assertThat(antesDe2028.tetoAplicado()).isFalse();

        ResultadoImposto abaixoDoTeto = servico.calcular("P7", data("2028-03-10"), valor("5000.00"));
        assertThat(abaixoDoTeto.aliquota()).isEqualByComparingTo("14");
        assertThat(abaixoDoTeto.valorImposto()).isEqualTo(valor("700.00"));
        assertThat(abaixoDoTeto.origem()).isEqualTo(Alvo.Tipo.CATEGORIA);
        assertThat(abaixoDoTeto.tetoAplicado()).isFalse();

        ResultadoImposto acimaDoTeto = servico.calcular("P7", data("2028-03-10"), valor("10000.00"));
        assertThat(acimaDoTeto.valorImposto()).isEqualTo(valor("1000.00"));
        assertThat(acimaDoTeto.tetoAplicado()).isTrue();

        ResultadoImposto regraPropria = servico.calcular("P8", data("2028-03-10"), valor("100.00"));
        assertThat(regraPropria.aliquota()).isEqualByComparingTo("5");
        assertThat(regraPropria.origem()).isEqualTo(Alvo.Tipo.PRODUTO);

        ResultadoImposto depoisDaRegraPropria = servico.calcular("P8", data("2028-07-01"), valor("10000.00"));
        assertThat(depoisDaRegraPropria.valorImposto()).isEqualTo(valor("1000.00"));
        assertThat(depoisDaRegraPropria.origem()).isEqualTo(Alvo.Tipo.CATEGORIA);

        falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA, () -> servico.calcular("P7", data("2025-12-31"), valor("100.00")));
    }
}
