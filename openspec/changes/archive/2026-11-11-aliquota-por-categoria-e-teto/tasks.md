# Tasks

## 1. Modelo: alvo, teto e resultado

- [x] 1.1 Criar `Alvo` (`Tipo` PRODUTO/CATEGORIA, fábricas `produto`/`categoria`) e adicionar `CATEGORIA_INVALIDA` e `TETO_INVALIDO` a `Motivo`; verificar que `./mvnw compile` passa sem avisos
- [x] 1.2 Evoluir `RegraDeAliquota` para `(alvo, aliquota, vigenteDe, vigenteAte, teto)` mantendo o construtor `(produto, aliquota, vigenteDe, vigenteAte)`, com `sobrepoe` por alvo e `encerradaEm` preservando o teto (design.md, decisão 2); verificar com novos casos em `RegraDeAliquotaTest` (produto e categoria de mesmo código não se sobrepõem; teto mantido ao encerrar) e com os testes atuais passando sem alteração
- [x] 1.3 Evoluir `ResultadoImposto` para `(aliquota, valorImposto, origem, tetoAplicado)`; verificar que `./mvnw test` passa com os testes atuais inalterados

## 2. Repositórios

- [x] 2.1 Trocar a busca do `RepositorioDeRegras` para `buscarPorAlvo(Alvo)`, mantendo `buscarPorProduto` como `default`, e indexar a implementação em memória por alvo; verificar com novo caso em `RepositorioDeRegrasEmMemoriaTest` (regras de produto e de categoria de mesmo código ficam separadas) e com os testes atuais inalterados
- [x] 2.2 Criar `RepositorioDeCategorias` e `RepositorioDeCategoriasEmMemoria`; verificar com `RepositorioDeCategoriasEmMemoriaTest` (produto sem categoria → vazio; atribuir; trocar substitui)

## 3. Categorias de produto (spec `categorias-de-produto`)

- [x] 3.1 Adicionar o construtor `ServicoDeImposto(RepositorioDeRegras, RepositorioDeCategorias)` (o atual delega com repositório de categorias vazio) e `atribuirCategoria(produto, categoria)`; verificar com `CategoriasDeProdutoTest` cobrindo todos os cenários de "Atribuir categoria a um produto" e "Validar atribuição de categoria"

## 4. Cadastro de regras de categoria e teto (spec `cadastro-de-aliquotas`)

- [x] 4.1 Generalizar cadastro e encerramento por `Alvo`, adicionar `cadastrarRegraDeCategoria` e `encerrarVigenciaDeCategoria`; verificar com `CadastroDeAliquotasDeCategoriaTest` cobrindo os cenários de "Cadastrar regra de alíquota para categoria", "Proibir sobreposição de vigência entre regras da mesma categoria" e "Encerrar vigência em aberto de categoria", e com `CadastroDeAliquotasTest`/`EncerramentoDeVigenciaTest` inalterados
- [x] 4.2 Adicionar o teto opcional ao cadastro (sobrecarga de `cadastrarRegra` com teto e parâmetro em `cadastrarRegraDeCategoria`) com a validação de teto (design.md, decisão 6); verificar com testes em `CadastroDeAliquotasDeCategoriaTest` para todos os cenários de "Definir teto do imposto na regra"

## 5. Cálculo por categoria e teto (spec `calculo-de-imposto`)

- [x] 5.1 Implementar a busca da regra com precedência produto → categoria do produto e preencher a origem no resultado (design.md, decisão 7); verificar com `CalculoPorCategoriaETetoTest` cobrindo os cenários novos de "Calcular imposto pela regra vigente" e de "Rejeitar cálculo sem regra vigente", e com `CalculoDeImpostoTest` inalterado
- [x] 5.2 Aplicar o teto após o arredondamento e preencher `tetoAplicado`; verificar com testes em `CalculoPorCategoriaETetoTest` para todos os cenários de "Aplicar teto do imposto"

## 6. Integração

- [x] 6.1 Criar `ImpostoPorCategoriaIT` com o caso de negócio (categoria `A` a 10% até `2027-12-31`, encerrada e seguida de 14% com teto `1000.00` a partir de `2028-01-01`; produtos `P7` e `P8` em `A`; cálculos abaixo, acima do teto e antes de 2028; regra própria de `P8` prevalecendo); verificar que roda no Failsafe com `./mvnw verify`
- [x] 6.2 Atualizar a descrição do domínio no `CLAUDE.md` (regras por produto ou categoria, teto, `RepositorioDeCategorias`); verificar que `./mvnw verify` passa completo e que nenhum teste pré-existente foi modificado (`git diff --stat` em `src/test` mostra apenas acréscimos)

## 7. Correções do code review

- [x] 7.1 Validar o código do alvo em `encerrarVigencia` (produto inválido) e `encerrarVigenciaDeCategoria` (categoria inválida); verificar com os cenários "Produto em branco ao encerrar" em `EncerramentoDeVigenciaTest` e "Categoria em branco ao encerrar" em `CadastroDeAliquotasDeCategoriaTest` (só acréscimos)
- [x] 7.2 Gravar o teto com exatamente 2 casas decimais (design.md, decisão 6); verificar com testes que `regra.teto()` é exatamente `50.00` para teto informado como `50` e exatamente `1000.00` para `1000.000` (comparando também a escala)
- [x] 7.3 Fazer `Alvo` rejeitar `tipo` e `codigo` nulos e produzir "o produto X" / "a categoria X" no `toString`; verificar com `AlvoTest`
- [x] 7.4 Verificar a mensagem de `SEM_REGRA_EM_ABERTO` com gênero correto, com testes que conferem "o produto P9" e "a categoria Z" na mensagem
- [x] 7.5 Atualizar o `## Purpose` da spec principal `openspec/specs/cadastro-de-aliquotas/spec.md` para regras por produto ou por categoria, com teto opcional; verificar com `openspec validate --specs --strict`
- [x] 7.6 Verificar que `./mvnw verify` passa completo e que `git diff` em `src/test` contém apenas acréscimos

## Workflow follow-up

- Arquivar a mudança (`openspec-archive-change`) após revisão.
