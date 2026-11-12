package com.pocsdd.imposto;

import java.math.BigDecimal;

/**
 * Alíquota aplicada (em percentual, como cadastrada), valor do imposto com 2 casas decimais, se a regra aplicada era
 * do produto ou da sua categoria, e se o valor foi limitado pelo teto da regra.
 */
public record ResultadoImposto(BigDecimal aliquota, BigDecimal valorImposto, Alvo.Tipo origem, boolean tetoAplicado) {
}
