# Tasks

## 1. Modelo de domínio e erros

- [x] 1.1 Criar `ImpostoException` com o enum `Motivo` (motivos listados em design.md, decisão 3) em `com.pocsdd.imposto`; verificar que `./mvnw compile` passa sem avisos
- [x] 1.2 Criar o record `RegraDeAliquota` com `emAberto()`, `vigenteEm(LocalDate)`, `sobrepoe(RegraDeAliquota)` e `encerradaEm(LocalDate)` (intervalo fechado, fim nulo = em aberto); verificar com `RegraDeAliquotaTest` cobrindo fronteiras inclusivas, regra em aberto, períodos contíguos (não sobrepõem) e sobreposição no dia de fronteira
- [x] 1.3 Criar o record `ResultadoImposto (aliquota, valorImposto)`; verificar que `./mvnw compile` passa

## 2. Repositório em memória

- [x] 2.1 Criar a interface `RepositorioDeRegras` (`buscarPorProduto`, `adicionar`, `substituir`) e `RepositorioDeRegrasEmMemoria`; verificar com `RepositorioDeRegrasEmMemoriaTest` que regras são isoladas por produto, que produto sem regras retorna lista vazia e que `substituir` troca apenas a regra indicada

## 3. Cadastro de regras (spec `cadastro-de-aliquotas`)

- [x] 3.1 Implementar `ServicoDeImposto.cadastrarRegra` com as validações de dados (produto, alíquota, período); verificar com `CadastroDeAliquotasTest` cobrindo os cenários dos requisitos "Cadastrar regra de alíquota" e "Validar dados da regra", assertando o `Motivo` e que nada foi gravado quando rejeitado
- [x] 3.2 Implementar a proibição de sobreposição no cadastro; verificar com testes em `CadastroDeAliquotasTest` para todos os cenários do requisito "Proibir sobreposição de vigência"

## 4. Encerramento de vigência (spec `cadastro-de-aliquotas`)

- [x] 4.1 Implementar `ServicoDeImposto.encerrarVigencia(produto, dataFim)`; verificar com `EncerramentoDeVigenciaTest` cobrindo os cenários do requisito "Encerrar vigência em aberto", incluindo que a regra continua em aberto após rejeição

## 5. Cálculo do imposto (spec `calculo-de-imposto`)

- [x] 5.1 Implementar `ServicoDeImposto.calcular(produto, data, valorBase)` com seleção da regra vigente e arredondamento HALF_UP em 2 casas (design.md, decisão 5); verificar com `CalculoDeImpostoTest` cobrindo os cenários de "Calcular imposto pela regra vigente" e "Arredondar o valor do imposto"
- [x] 5.2 Implementar os erros do cálculo (sem regra vigente, entrada inválida); verificar com testes em `CalculoDeImpostoTest` para os cenários de "Rejeitar cálculo sem regra vigente" e "Validar entrada do cálculo", incluindo que a mensagem de `ALIQUOTA_NAO_ENCONTRADA` contém produto e data

## 6. Integração

- [x] 6.1 Criar `ImpostoIT` percorrendo o fluxo completo pela API pública (cadastrar `P1` 10% em aberto, encerrar em `2026-06-30`, cadastrar 12% a partir de `2026-07-01`, calcular em `2026-06-30`, `2026-07-01` e antes da primeira regra); verificar que roda no Failsafe com `./mvnw verify`
- [x] 6.2 Atualizar a descrição do projeto no `CLAUDE.md` (deixa de ser só o placeholder `App`; citar o pacote `com.pocsdd.imposto`); verificar que `./mvnw verify` passa completo (lint + unitários + integração)

## Workflow follow-up

- Sincronizar as specs delta em `openspec/specs/` (`openspec-sync-specs`) e arquivar a mudança (`openspec-archive-change`) após revisão.
