package com.pocsdd.imposto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alíquota (em percentual) de um produto ou categoria vigente no intervalo fechado [vigenteDe, vigenteAte].
 * {@code vigenteAte == null} significa vigência em aberto; {@code teto == null} significa imposto sem limite.
 */
public record RegraDeAliquota(Alvo alvo, BigDecimal aliquota, LocalDate vigenteDe, LocalDate vigenteAte,
        BigDecimal teto) {

    /** Regra de produto sem teto. */
    public RegraDeAliquota(String produto, BigDecimal aliquota, LocalDate vigenteDe, LocalDate vigenteAte) {
        this(Alvo.produto(produto), aliquota, vigenteDe, vigenteAte, null);
    }

    public boolean emAberto() {
        return vigenteAte == null;
    }

    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(vigenteDe) && !data.isAfter(fimEfetivo());
    }

    public boolean sobrepoe(RegraDeAliquota outra) {
        return alvo.equals(outra.alvo)
                && !vigenteDe.isAfter(outra.fimEfetivo())
                && !outra.vigenteDe.isAfter(fimEfetivo());
    }

    public RegraDeAliquota encerradaEm(LocalDate dataFim) {
        return new RegraDeAliquota(alvo, aliquota, vigenteDe, dataFim, teto);
    }

    private LocalDate fimEfetivo() {
        return emAberto() ? LocalDate.MAX : vigenteAte;
    }
}
