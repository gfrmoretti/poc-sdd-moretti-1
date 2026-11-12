package com.pocsdd.imposto;

import com.pocsdd.imposto.ImpostoException.Motivo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Porta de entrada do domínio: categorias de produto, cadastro de regras de alíquota e cálculo do imposto. */
public final class ServicoDeImposto {

    private final RepositorioDeRegras repositorio;
    private final RepositorioDeCategorias categorias;

    /** Serviço sem categorias atribuídas, com as categorias guardadas em memória. */
    public ServicoDeImposto(RepositorioDeRegras repositorio) {
        this(repositorio, new RepositorioDeCategoriasEmMemoria());
    }

    public ServicoDeImposto(RepositorioDeRegras repositorio, RepositorioDeCategorias categorias) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.categorias = Objects.requireNonNull(categorias, "categorias");
    }

    /** Define a categoria do produto, substituindo a anterior; vale para cálculos em qualquer data. */
    public void atribuirCategoria(String produto, String categoria) {
        validarProduto(produto);
        validarCategoria(categoria);
        categorias.atribuir(produto, categoria);
    }

    /**
     * Cadastra a alíquota (em percentual) do produto vigente de {@code vigenteDe} até {@code vigenteAte}, ambos
     * inclusivos; {@code vigenteAte} nulo deixa a vigência em aberto.
     */
    public RegraDeAliquota cadastrarRegra(String produto, BigDecimal aliquota, LocalDate vigenteDe,
            LocalDate vigenteAte) {
        return cadastrarRegra(produto, aliquota, vigenteDe, vigenteAte, null);
    }

    /** Como {@link #cadastrarRegra(String, BigDecimal, LocalDate, LocalDate)}, com teto opcional em reais. */
    public RegraDeAliquota cadastrarRegra(String produto, BigDecimal aliquota, LocalDate vigenteDe,
            LocalDate vigenteAte, BigDecimal teto) {
        validarProduto(produto);
        return cadastrar(Alvo.produto(produto), aliquota, vigenteDe, vigenteAte, teto);
    }

    /** Cadastra a alíquota da categoria, que vale para os produtos dela sem regra própria vigente na data. */
    public RegraDeAliquota cadastrarRegraDeCategoria(String categoria, BigDecimal aliquota, LocalDate vigenteDe,
            LocalDate vigenteAte, BigDecimal teto) {
        validarCategoria(categoria);
        return cadastrar(Alvo.categoria(categoria), aliquota, vigenteDe, vigenteAte, teto);
    }

    /** Encerra em {@code dataFim} (inclusiva) a regra em aberto do produto. */
    public RegraDeAliquota encerrarVigencia(String produto, LocalDate dataFim) {
        validarProduto(produto);
        return encerrar(Alvo.produto(produto), dataFim);
    }

    /** Encerra em {@code dataFim} (inclusiva) a regra em aberto da categoria. */
    public RegraDeAliquota encerrarVigenciaDeCategoria(String categoria, LocalDate dataFim) {
        validarCategoria(categoria);
        return encerrar(Alvo.categoria(categoria), dataFim);
    }

    /**
     * Imposto sobre {@code valorBase} pela regra do produto vigente em {@code data} ou, se não houver, pela regra
     * vigente da categoria do produto; limitado ao teto da regra aplicada.
     */
    public ResultadoImposto calcular(String produto, LocalDate data, BigDecimal valorBase) {
        validarProduto(produto);
        if (data == null) {
            throw new ImpostoException(Motivo.DATA_INVALIDA, "Data de referência é obrigatória");
        }
        if (valorBase == null || valorBase.signum() < 0) {
            throw new ImpostoException(Motivo.VALOR_BASE_INVALIDO,
                    "Valor-base deve ser informado e não pode ser negativo: " + valorBase);
        }
        RegraDeAliquota vigente = regraVigente(Alvo.produto(produto), data)
                .or(() -> categorias.categoriaDe(produto)
                        .flatMap(categoria -> regraVigente(Alvo.categoria(categoria), data)))
                .orElseThrow(() -> new ImpostoException(Motivo.ALIQUOTA_NAO_ENCONTRADA,
                        "Não há alíquota vigente para o produto " + produto + " em " + data));

        // Multiplicação e movePointLeft são exatos: o único arredondamento é o final.
        BigDecimal arredondado = valorBase.multiply(vigente.aliquota())
                .movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP);
        boolean tetoAplicado = vigente.teto() != null && arredondado.compareTo(vigente.teto()) > 0;
        BigDecimal valorImposto = tetoAplicado ? vigente.teto() : arredondado;
        return new ResultadoImposto(vigente.aliquota(), valorImposto, vigente.alvo().tipo(), tetoAplicado);
    }

    private RegraDeAliquota cadastrar(Alvo alvo, BigDecimal aliquota, LocalDate vigenteDe, LocalDate vigenteAte,
            BigDecimal teto) {
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
        if (teto != null && (teto.signum() <= 0 || teto.stripTrailingZeros().scale() > 2)) {
            throw new ImpostoException(Motivo.TETO_INVALIDO,
                    "Teto deve ser maior que zero e ter no máximo 2 casas decimais: " + teto);
        }

        // A validação acima garante no máximo 2 casas significativas: a escala 2 é exata.
        BigDecimal tetoEmReais = teto == null ? null : teto.setScale(2, RoundingMode.UNNECESSARY);
        RegraDeAliquota nova = new RegraDeAliquota(alvo, aliquota, vigenteDe, vigenteAte, tetoEmReais);
        for (RegraDeAliquota existente : repositorio.buscarPorAlvo(alvo)) {
            if (existente.sobrepoe(nova)) {
                throw new ImpostoException(Motivo.SOBREPOSICAO_DE_VIGENCIA,
                        "Vigência de " + nova + " se sobrepõe a " + existente);
            }
        }
        repositorio.adicionar(nova);
        return nova;
    }

    private RegraDeAliquota encerrar(Alvo alvo, LocalDate dataFim) {
        if (dataFim == null) {
            throw new ImpostoException(Motivo.PERIODO_INVALIDO, "Data de fim de vigência é obrigatória");
        }
        RegraDeAliquota aberta = repositorio.buscarPorAlvo(alvo).stream()
                .filter(RegraDeAliquota::emAberto)
                .findFirst()
                .orElseThrow(() -> new ImpostoException(Motivo.SEM_REGRA_EM_ABERTO,
                        "Não há regra de alíquota em aberto para " + alvo));
        if (dataFim.isBefore(aberta.vigenteDe())) {
            throw new ImpostoException(Motivo.PERIODO_INVALIDO,
                    "Fim de vigência " + dataFim + " é anterior ao início " + aberta.vigenteDe());
        }

        RegraDeAliquota encerrada = aberta.encerradaEm(dataFim);
        repositorio.substituir(aberta, encerrada);
        return encerrada;
    }

    private Optional<RegraDeAliquota> regraVigente(Alvo alvo, LocalDate data) {
        return repositorio.buscarPorAlvo(alvo).stream()
                .filter(regra -> regra.vigenteEm(data))
                .findFirst();
    }

    private static void validarProduto(String produto) {
        if (produto == null || produto.isBlank()) {
            throw new ImpostoException(Motivo.PRODUTO_INVALIDO, "Código do produto é obrigatório");
        }
    }

    private static void validarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new ImpostoException(Motivo.CATEGORIA_INVALIDA, "Código da categoria é obrigatório");
        }
    }
}
