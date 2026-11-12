package com.pocsdd.imposto;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/** Porta de entrada do domínio: cadastro de regras de alíquota e cálculo do imposto. */
public final class ServicoDeImposto {

    private final RepositorioDeRegras repositorio;

    public ServicoDeImposto(RepositorioDeRegras repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    /**
     * Cadastra a alíquota (em percentual) do produto vigente de {@code vigenteDe} até {@code vigenteAte}, ambos
     * inclusivos; {@code vigenteAte} nulo deixa a vigência em aberto.
     */
    public RegraDeAliquota cadastrarRegra(String produto, BigDecimal aliquota, LocalDate vigenteDe,
            LocalDate vigenteAte) {
        validarProduto(produto);
        if (aliquota == null || aliquota.signum() < 0) {
            throw new ImpostoException(Motivo.ALIQUOTA_INVALIDA,
                    "Alíquota deve ser informada e não pode ser negativa: " + aliquota);
        }
        if (vigenteDe == null) {
            throw new ImpostoException(Motivo.PERIODO_INVALIDO, "Data de início de vigência é obrigatória");
        }
        if (vigenteAte != null && vigenteAte.isBefore(vigenteDe)) {
            throw new ImpostoException(Motivo.PERIODO_INVALIDO,
                    "Fim de vigência " + vigenteAte + " é anterior ao início " + vigenteDe);
        }

        RegraDeAliquota nova = new RegraDeAliquota(produto, aliquota, vigenteDe, vigenteAte);
        for (RegraDeAliquota existente : repositorio.buscarPorProduto(produto)) {
            if (existente.sobrepoe(nova)) {
                throw new ImpostoException(Motivo.SOBREPOSICAO_DE_VIGENCIA,
                        "Vigência de " + nova + " se sobrepõe a " + existente);
            }
        }
        repositorio.adicionar(nova);
        return nova;
    }

    /** Encerra em {@code dataFim} (inclusiva) a regra em aberto do produto. */
    public RegraDeAliquota encerrarVigencia(String produto, LocalDate dataFim) {
        if (dataFim == null) {
            throw new ImpostoException(Motivo.PERIODO_INVALIDO, "Data de fim de vigência é obrigatória");
        }
        RegraDeAliquota aberta = repositorio.buscarPorProduto(produto).stream()
                .filter(RegraDeAliquota::emAberto)
                .findFirst()
                .orElseThrow(() -> new ImpostoException(Motivo.SEM_REGRA_EM_ABERTO,
                        "Produto " + produto + " não tem regra de alíquota em aberto"));
        if (dataFim.isBefore(aberta.vigenteDe())) {
            throw new ImpostoException(Motivo.PERIODO_INVALIDO,
                    "Fim de vigência " + dataFim + " é anterior ao início " + aberta.vigenteDe());
        }

        RegraDeAliquota encerrada = aberta.encerradaEm(dataFim);
        repositorio.substituir(aberta, encerrada);
        return encerrada;
    }

    /** Imposto sobre {@code valorBase} pela alíquota do produto vigente em {@code data}. */
    public ResultadoImposto calcular(String produto, LocalDate data, BigDecimal valorBase) {
        validarProduto(produto);
        if (data == null) {
            throw new ImpostoException(Motivo.DATA_INVALIDA, "Data de referência é obrigatória");
        }
        if (valorBase == null || valorBase.signum() < 0) {
            throw new ImpostoException(Motivo.VALOR_BASE_INVALIDO,
                    "Valor-base deve ser informado e não pode ser negativo: " + valorBase);
        }
        RegraDeAliquota vigente = repositorio.buscarPorProduto(produto).stream()
                .filter(regra -> regra.vigenteEm(data))
                .findFirst()
                .orElseThrow(() -> new ImpostoException(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                        "Não há alíquota vigente para o produto " + produto + " em " + data));

        // Multiplicação e movePointLeft são exatos: o único arredondamento é o final.
        BigDecimal valorImposto = valorBase.multiply(vigente.aliquota())
                .movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP);
        return new ResultadoImposto(vigente.aliquota(), valorImposto);
    }

    private static void validarProduto(String produto) {
        if (produto == null || produto.isBlank()) {
            throw new ImpostoException(Motivo.PRODUTO_INVALIDO, "Código do produto é obrigatório");
        }
    }
}
