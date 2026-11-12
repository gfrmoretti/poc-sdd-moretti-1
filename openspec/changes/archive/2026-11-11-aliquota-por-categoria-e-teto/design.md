# Design

## Context

O domínio atual (`com.pocsdd.imposto`, ver specs `cadastro-de-aliquotas` e `calculo-de-imposto`) tem regras só por produto: `RegraDeAliquota(produto, aliquota, vigenteDe, vigenteAte)`, `RepositorioDeRegras.buscarPorProduto`, `ServicoDeImposto` com `cadastrarRegra`, `encerrarVigencia` e `calcular`, e `ResultadoImposto(aliquota, valorImposto)`. Há 55 testes cobrindo esse comportamento, e a regra do projeto proíbe alterar testes salvo mudança na spec que eles codificam — e os requisitos existentes só ganham comportamento, não mudam o atual. Motivação: ver `proposal.md`.

## Goals / Non-Goals

**Goals:**
- Regras de produto e de categoria compartilham o mesmo modelo, validações, sobreposição e encerramento, diferindo só no "alvo".
- Manter as assinaturas usadas pelos testes existentes (construtor de regra de produto, `buscarPorProduto`, `cadastrarRegra`, `encerrarVigencia`, `calcular`), para que continuem passando sem edição. `RegraDeAliquota.produto()` e o construtor de 2 argumentos de `ResultadoImposto` são removidos de propósito: não têm consumidores fora do domínio e `produto()` não faz sentido para regras de categoria.
- Teto como atributo da regra, aplicado num único ponto do cálculo.

**Non-Goals:**
- Vigência da associação produto → categoria (a troca vale para qualquer data de referência).
- Remover a categoria de um produto, hierarquia de categorias, mais de uma categoria por produto.
- Teto acumulado por período (o teto é por cálculo).

## Decisions

**1. Alvo da regra.** Novo `record Alvo(Tipo tipo, String codigo)`, com `enum Tipo { PRODUTO, CATEGORIA }` e fábricas `Alvo.produto(codigo)` / `Alvo.categoria(codigo)`. Produto `"A"` e categoria `"A"` são alvos diferentes, o que dá de graça "regra de categoria nunca conflita com regra de produto". O record rejeita `tipo` e `codigo` nulos (código em branco é validado pelo serviço, com o motivo de negócio), e seu `toString` produz "o produto X" / "a categoria X" para as mensagens de erro. *Alternativa:* dois tipos de regra ou um campo `categoria` nulo na regra — duplicaria validação/sobreposição ou criaria estados inválidos (produto e categoria preenchidos).

**2. Regra com teto.** `RegraDeAliquota` passa a `(Alvo alvo, BigDecimal aliquota, LocalDate vigenteDe, LocalDate vigenteAte, BigDecimal teto)`, com `teto == null` = sem teto. Um construtor secundário `(String produto, aliquota, vigenteDe, vigenteAte)` cria regra de produto sem teto, preservando o uso atual nos testes. `sobrepoe` compara `alvo` em vez de `produto`; `encerradaEm` preserva o teto.

**3. Repositório de regras.** `buscarPorProduto(String)` vira `buscarPorAlvo(Alvo)`; `buscarPorProduto` permanece como método `default` da interface delegando para `buscarPorAlvo(Alvo.produto(...))`. A implementação em memória indexa por `Alvo`.

**4. Categoria do produto.** Nova porta `RepositorioDeCategorias` (`Optional<String> categoriaDe(String produto)`, `void atribuir(String produto, String categoria)`) com `RepositorioDeCategoriasEmMemoria`. Separada de `RepositorioDeRegras` porque é outro dado com outro ciclo de vida (cadastro de produto vs. tabela tributária). `ServicoDeImposto` ganha o construtor `(RepositorioDeRegras, RepositorioDeCategorias)`; o construtor atual `(RepositorioDeRegras)` usa um `RepositorioDeCategoriasEmMemoria` vazio.

**5. Operações do serviço.**
- `cadastrarRegra(produto, aliquota, de, ate)` (existente) e nova sobrecarga com `teto`.
- `cadastrarRegraDeCategoria(categoria, aliquota, de, ate, teto)`.
- `encerrarVigencia(produto, dataFim)` (existente) e `encerrarVigenciaDeCategoria(categoria, dataFim)`.
- `atribuirCategoria(produto, categoria)`.
- `calcular(produto, data, valorBase)` — mesma assinatura.

Cadastro e encerramento passam a ser métodos privados parametrizados por `Alvo`; os públicos validam o código (produto ou categoria, inclusive no encerramento) e montam o alvo. Ordem de validação no cadastro: código do alvo, alíquota, período, teto, sobreposição (nenhuma escrita antes de todas passarem).

**6. Erros.** `Motivo` ganha `CATEGORIA_INVALIDA` e `TETO_INVALIDO`. Teto válido: `teto.signum() > 0 && teto.stripTrailingZeros().scale() <= 2` (assim `1000`, `1000.00` e `1000.000` são aceitos e `1000.001` não). O teto aceito é gravado com `setScale(2)` (exato, dada a validação), então regras equivalentes são `equals` e o teto sempre aparece com 2 casas. A mensagem de `ALIQUOTA_NAO_ENCONTRADA` continua citando produto e data.

**7. Cálculo.**
```
regra = vigente(Alvo.produto(p), data)
        .or(() -> categoriaDe(p).flatMap(c -> vigente(Alvo.categoria(c), data)))
        .orElseThrow(ALIQUOTA_NAO_ENCONTRADA)
arredondado = base × aliquota / 100, HALF_UP, escala 2        (como hoje)
tetoAplicado = teto != null && arredondado > teto
valor = tetoAplicado ? teto.setScale(2) : arredondado
```
A precedência é por data: um produto com regra própria só em parte do período usa a da categoria fora dela. `ResultadoImposto` passa a `(aliquota, valorImposto, Alvo.Tipo origem, boolean tetoAplicado)`.

**8. Testes.** Os testes existentes ficam intactos (são a prova de compatibilidade). Novos testes por requisito: `CategoriasDeProdutoTest`, `CadastroDeAliquotasDeCategoriaTest` (regras de categoria + teto no cadastro), `CalculoPorCategoriaETetoTest` (cenários novos e alterados de `calculo-de-imposto`), novos casos em `RegraDeAliquotaTest` e `RepositorioDeRegrasEmMemoriaTest` para alvo/teto. Um teste funcional `ImpostoPorCategoriaIT` reproduz o caso de negócio: categoria A a 10% até 2027-12-31, 14% com teto 1000 a partir de 2028-01-01.

## Risks / Trade-offs

- [Associação produto → categoria sem vigência: trocar a categoria muda também o resultado de cálculos com datas passadas] → decisão explícita da spec; se precisar de histórico, uma mudança futura dá vigência à associação.
- [Construtores/métodos de compatibilidade aumentam a superfície da API] → custo pequeno, evita reescrever testes que codificam requisitos que não mudaram; restrito ao que os testes usam (ver Goals).
- [Teto por cálculo, não acumulado] → é o comportamento especificado; acumulado exigiria histórico de cálculos.
- [Purpose da spec principal `cadastro-de-aliquotas` fala só em "por produto"] → ajustar o texto do Purpose na spec principal ao sincronizar/arquivar (não pode ser feito por delta).
