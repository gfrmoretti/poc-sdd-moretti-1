package com.pocsdd.imposto;

import static com.pocsdd.imposto.ImpostoAssertions.falhaCom;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Functional test: troca de alíquota de um produto ao longo do tempo, pela API pública. */
class ImpostoIT {

    @Test
    void trocaDeAliquotaAoLongoDoTempo() {
        ServicoDeImposto servico = new ServicoDeImposto(new RepositorioDeRegrasEmMemoria());
        BigDecimal base = new BigDecimal("100.00");

        servico.cadastrarRegra("P1", new BigDecimal("10"), LocalDate.parse("2026-01-01"), null);
        servico.encerrarVigencia("P1", LocalDate.parse("2026-06-30"));
        servico.cadastrarRegra("P1", new BigDecimal("12"), LocalDate.parse("2026-07-01"), null);

        ResultadoImposto ultimoDiaDe10 = servico.calcular("P1", LocalDate.parse("2026-06-30"), base);
        assertThat(ultimoDiaDe10.aliquota()).isEqualByComparingTo("10");
        assertThat(ultimoDiaDe10.valorImposto()).isEqualTo(new BigDecimal("10.00"));

        ResultadoImposto primeiroDiaDe12 = servico.calcular("P1", LocalDate.parse("2026-07-01"), base);
        assertThat(primeiroDiaDe12.aliquota()).isEqualByComparingTo("12");
        assertThat(primeiroDiaDe12.valorImposto()).isEqualTo(new BigDecimal("12.00"));

        falhaCom(Motivo.ALIQUOTA_NAO_ENCONTRADA, () -> servico.calcular("P1", LocalDate.parse("2025-12-31"), base));
    }
}
