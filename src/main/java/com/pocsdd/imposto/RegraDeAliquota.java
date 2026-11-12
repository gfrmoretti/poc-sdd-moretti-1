package com.pocsdd.imposto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alíquota (em percentual) de um produto vigente no intervalo fechado [vigenteDe, vigenteAte].
 * {@code vigenteAte == null} significa vigência em aberto.
 */
public record RegraDeAliquota(String produto, BigDecimal aliquota, LocalDate vigenteDe, LocalDate vigenteAte) {

    public boolean emAberto() {
        return vigenteAte == null;
    }

    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(vigenteDe) && !data.isAfter(fimEfetivo());
    }

    public boolean sobrepoe(RegraDeAliquota outra) {
        return produto.equals(outra.produto)
                && !vigenteDe.isAfter(outra.fimEfetivo())
                && !outra.vigenteDe.isAfter(fimEfetivo());
    }

    public RegraDeAliquota encerradaEm(LocalDate dataFim) {
        return new RegraDeAliquota(produto, aliquota, vigenteDe, dataFim);
    }

    private LocalDate fimEfetivo() {
        return emAberto() ? LocalDate.MAX : vigenteAte;
    }
}
