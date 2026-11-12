# Proposal

## Why

O sistema precisa calcular o imposto de um produto considerando que a alíquota muda ao longo do tempo. Hoje não existe nenhuma regra de negócio no projeto (só o `App` placeholder); esta é a primeira capacidade real e serve para exercitar o fluxo spec-driven ponta a ponta.

## What Changes

- Cadastro de regras de alíquota por produto, cada uma com período de vigência (início obrigatório, fim opcional = vigência em aberto).
- Validação do cadastro: alíquota não negativa, fim não anterior ao início e proibição de sobreposição de períodos para o mesmo produto.
- Operação para encerrar a vigência da regra em aberto de um produto numa data, permitindo cadastrar a alíquota seguinte.
- Cálculo do imposto dado produto, data e valor-base: retorna a alíquota vigente na data e o valor do imposto (2 casas decimais, arredondamento HALF_UP).
- Erro explícito quando não há regra vigente para o produto na data (nunca imposto zero implícito).
- Entrega como biblioteca de domínio com armazenamento de regras em memória; sem CLI, API REST ou persistência nesta mudança.

## Capabilities

### New Capabilities

- `cadastro-de-aliquotas`: manutenção das regras de alíquota por produto e período de vigência (cadastrar, encerrar vigência) e suas validações.
- `calculo-de-imposto`: cálculo do imposto de um produto numa data a partir da regra vigente, incluindo formato do resultado, arredondamento e erros.

### Modified Capabilities

(nenhuma)

## Impact

- Novo código de domínio em `src/main/java/com/pocsdd/` (pacote dedicado ao imposto), com testes unitários (`*Test`) e de integração/funcionais (`*IT`).
- Sem novas dependências de runtime; usa apenas a JDK. Testes continuam com JUnit + AssertJ.
- O placeholder `App` não é afetado.
