package com.pocsdd.imposto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.pocsdd.imposto.ImpostoException.Motivo;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

final class ImpostoAssertions {

    private ImpostoAssertions() {
    }

    /** Verifica que a operação lança {@link ImpostoException} com o motivo esperado e devolve o erro. */
    static ImpostoException falhaCom(Motivo motivo, ThrowingCallable operacao) {
        ImpostoException erro = catchThrowableOfType(ImpostoException.class, operacao);
        assertThat(erro).as("esperava ImpostoException com motivo %s", motivo).isNotNull();
        assertThat(erro.motivo()).isEqualTo(motivo);
        return erro;
    }
}
