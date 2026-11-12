package com.pocsdd.imposto;

/** Erro de regra de negócio do imposto; o {@link Motivo} identifica a causa sem depender da mensagem. */
public final class ImpostoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        PRODUTO_INVALIDO,
        ALIQUOTA_INVALIDA,
        PERIODO_INVALIDO,
        SOBREPOSICAO_DE_VIGENCIA,
        SEM_REGRA_EM_ABERTO,
        ALIQUOTA_NAO_ENCONTRADA,
        VALOR_BASE_INVALIDO,
        DATA_INVALIDA
    }

    private final Motivo motivo;

    public ImpostoException(Motivo motivo, String mensagem) {
        super(mensagem);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
