package com.pocsdd.imposto;

import java.math.BigDecimal;

/** Alíquota aplicada (em percentual, como cadastrada) e valor do imposto com 2 casas decimais. */
public record ResultadoImposto(BigDecimal aliquota, BigDecimal valorImposto) {
}
