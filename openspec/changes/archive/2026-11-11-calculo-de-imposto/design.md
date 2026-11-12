# Design

## Context

Projeto Java 25 / Maven sem framework e sem código de domínio (apenas o `App` placeholder em `com.pocsdd`). O build (`./mvnw verify`) aplica Checkstyle e `-Xlint:all -Werror`, roda testes unitários (`*Test`, Surefire) e de integração/funcionais (`*IT`, Failsafe) com JUnit 6 + AssertJ. Motivação e escopo: ver `proposal.md`; comportamento exigido: ver `specs/cadastro-de-aliquotas` e `specs/calculo-de-imposto`.

## Goals / Non-Goals

**Goals:**
- Domínio pequeno e sem dependências, com uma única porta de entrada para cadastro, encerramento e cálculo.
- Armazenamento de regras atrás de uma interface, para trocar a implementação em memória por persistência real depois sem mexer nas regras de negócio.
- Erros distinguíveis por motivo, para que testes e chamadores verifiquem o motivo sem depender do texto da mensagem.

**Non-Goals:**
- Concorrência/thread-safety (uso single-thread nesta mudança).
- Remover ou editar regras além de encerrar a vigência em aberto.
- Moeda, múltiplos tipos de imposto, CLI, REST ou persistência.

## Decisions

**1. Pacote e idioma.** Código em `com.pocsdd.imposto`, com nomes de domínio em português, alinhados à spec (`ServicoDeImposto`, `RegraDeAliquota`, `calcular`). *Alternativa:* nomes em inglês como o `App` — rejeitada para manter a linguagem do negócio igual entre spec, código e testes.

**2. Tipos.**
- `RegraDeAliquota` — `record (String produto, BigDecimal aliquota, LocalDate vigenteDe, LocalDate vigenteAte)`, com `vigenteAte == null` = em aberto. Métodos de conveniência: `emAberto()`, `vigenteEm(LocalDate)`, `sobrepoe(RegraDeAliquota)`, `encerradaEm(LocalDate)` (retorna nova instância). Imutável.
- `ResultadoImposto` — `record (BigDecimal aliquota, BigDecimal valorImposto)`.
- `ServicoDeImposto` — fachada: `cadastrarRegra(produto, aliquota, vigenteDe, vigenteAte)`, `encerrarVigencia(produto, dataFim)`, `calcular(produto, data, valorBase)`. Recebe o repositório no construtor. Todas as validações da spec vivem aqui (o record não valida, para que o serviço controle o motivo do erro).
- `RepositorioDeRegras` (interface) — `List<RegraDeAliquota> buscarPorProduto(String)`, `void adicionar(RegraDeAliquota)`, `void substituir(RegraDeAliquota atual, RegraDeAliquota nova)`.
- `RepositorioDeRegrasEmMemoria` — `Map<String, List<RegraDeAliquota>>`.

*Alternativa:* um único objeto que valida e guarda — rejeitada porque acopla a regra de negócio ao armazenamento.

**3. Erros.** Uma exceção não checada `ImpostoException` com `Motivo` (enum): `PRODUTO_INVALIDO`, `ALIQUOTA_INVALIDA`, `PERIODO_INVALIDO`, `SOBREPOSICAO_DE_VIGENCIA`, `SEM_REGRA_EM_ABERTO`, `ALIQUOTA_NAO_ENCONTRADA`, `VALOR_BASE_INVALIDO`, `DATA_INVALIDA`. Para `ALIQUOTA_NAO_ENCONTRADA` a mensagem inclui produto e data. *Alternativa:* uma classe por erro — mais código sem ganho para os chamadores atuais; o enum deixa os testes assertarem o motivo diretamente. Toda validação ocorre antes de qualquer escrita no repositório, garantindo "rejeitado sem alterar as regras".

**4. Vigência e sobreposição.** Intervalos fechados `[vigenteDe, vigenteAte]`, com `vigenteAte == null` tratado como `LocalDate.MAX`. Duas regras do mesmo produto se sobrepõem se `a.de <= b.ate && b.de <= a.ate`. Como sobreposição é proibida, existe no máximo uma regra vigente por data e no máximo uma em aberto por produto — por isso `encerrarVigencia` só precisa do produto.

**5. Aritmética.** Alíquota em percentual (`12` = 12%) como `BigDecimal`. `valorImposto = valorBase.multiply(aliquota).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)` — multiplicação e deslocamento de vírgula são exatos, então há um único arredondamento ao final. O resultado devolve a alíquota exatamente como cadastrada; testes comparam `BigDecimal` por valor (`isEqualByComparingTo`), não por escala. `valorImposto` sempre tem escala 2.

**6. Testes.** Unitários (`*Test`) por requisito da spec, exercitando `ServicoDeImposto` com o repositório em memória; um cenário da spec ≈ um teste, com o nome do cenário no nome do método. Um teste funcional (`ImpostoIT`) percorre o fluxo completo da spec — cadastrar, encerrar, cadastrar a seguinte, calcular nas fronteiras e no buraco — pela API pública.

## Risks / Trade-offs

- [Busca linear das regras do produto] → irrelevante para os volumes de uma biblioteca em memória; a interface do repositório permite indexar depois.
- [Sem thread-safety] → documentado como não-objetivo; uma implementação persistente futura tratará concorrência.
- [`LocalDate.MAX` como sentinela de "em aberto"] → usado só internamente na comparação; o modelo público continua com `vigenteAte` nulo.
- [Alíquota devolvida com a escala cadastrada (`12` vs `12.00`)] → comparação por valor nos testes; se um consumidor futuro exigir escala fixa, normalizar na saída.
