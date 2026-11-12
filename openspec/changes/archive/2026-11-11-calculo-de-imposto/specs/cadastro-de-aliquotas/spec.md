# Spec Delta

## Purpose

Mantém as regras de alíquota de imposto por produto, cada uma válida num período de vigência, garantindo que em qualquer data exista no máximo uma regra aplicável por produto.

## ADDED Requirements

### Requirement: Cadastrar regra de alíquota
O sistema SHALL permitir cadastrar uma regra de alíquota informando o código do produto, a alíquota em percentual (ex.: `12` = 12%, decimais permitidos), a data de início de vigência e, opcionalmente, a data de fim. Início e fim são inclusivos; sem data de fim, a regra vale por tempo indeterminado (em aberto).

#### Scenario: Regra com período fechado
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `10`, de `2026-01-01` até `2026-06-30`
- **THEN** o cadastro é aceito
- **AND** a regra se aplica a `P1` em `2026-01-01`, em `2026-06-30` e em qualquer data entre elas

#### Scenario: Regra em aberto
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `12`, de `2026-07-01`, sem data de fim
- **THEN** o cadastro é aceito
- **AND** a regra se aplica a `P1` em `2026-07-01` e em qualquer data posterior

#### Scenario: Alíquota com casas decimais
- **WHEN** é cadastrada a regra do produto `P2` com alíquota `7.5`, de `2026-01-01`, sem data de fim
- **THEN** o cadastro é aceito com alíquota de 7,5%

#### Scenario: Alíquota zero
- **WHEN** é cadastrada a regra do produto `P3` com alíquota `0`, de `2026-01-01`, sem data de fim
- **THEN** o cadastro é aceito (produto isento no período)

### Requirement: Validar dados da regra
O sistema MUST rejeitar o cadastro de uma regra com dados inválidos, com um erro que identifique o motivo, sem alterar as regras existentes. São inválidos: código de produto ausente ou em branco, alíquota ausente ou negativa, data de início ausente e data de fim anterior à data de início.

#### Scenario: Alíquota negativa
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `-1`, de `2026-01-01`
- **THEN** o cadastro é rejeitado indicando alíquota inválida

#### Scenario: Fim anterior ao início
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `10`, de `2026-06-30` até `2026-01-01`
- **THEN** o cadastro é rejeitado indicando período inválido

#### Scenario: Início igual ao fim
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `10`, de `2026-03-15` até `2026-03-15`
- **THEN** o cadastro é aceito e a regra vale somente em `2026-03-15`

#### Scenario: Produto em branco
- **WHEN** é cadastrada uma regra com código de produto em branco
- **THEN** o cadastro é rejeitado indicando produto inválido

#### Scenario: Data de início ausente
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `10` sem data de início
- **THEN** o cadastro é rejeitado indicando período inválido

### Requirement: Proibir sobreposição de vigência
O sistema MUST rejeitar o cadastro de uma regra cujo período compartilhe ao menos um dia com o período de outra regra do mesmo produto, mantendo as regras existentes inalteradas. Regras de produtos diferentes nunca conflitam.

#### Scenario: Sobreposição parcial
- **GIVEN** o produto `P1` tem regra de `2026-01-01` até `2026-06-30`
- **WHEN** é cadastrada para `P1` uma regra de `2026-06-01` até `2026-12-31`
- **THEN** o cadastro é rejeitado indicando sobreposição de vigência

#### Scenario: Sobreposição no dia de fronteira
- **GIVEN** o produto `P1` tem regra de `2026-01-01` até `2026-06-30`
- **WHEN** é cadastrada para `P1` uma regra a partir de `2026-06-30`
- **THEN** o cadastro é rejeitado indicando sobreposição de vigência

#### Scenario: Períodos contíguos
- **GIVEN** o produto `P1` tem regra de `2026-01-01` até `2026-06-30`
- **WHEN** é cadastrada para `P1` uma regra a partir de `2026-07-01`
- **THEN** o cadastro é aceito

#### Scenario: Conflito com regra em aberto
- **GIVEN** o produto `P1` tem regra a partir de `2026-01-01`, em aberto
- **WHEN** é cadastrada para `P1` uma regra de `2027-01-01` até `2027-12-31`
- **THEN** o cadastro é rejeitado indicando sobreposição de vigência

#### Scenario: Produtos diferentes no mesmo período
- **GIVEN** o produto `P1` tem regra de `2026-01-01` até `2026-12-31`
- **WHEN** é cadastrada para `P2` uma regra de `2026-01-01` até `2026-12-31`
- **THEN** o cadastro é aceito

### Requirement: Encerrar vigência em aberto
O sistema SHALL permitir encerrar a regra em aberto de um produto informando o código do produto e a data de fim (inclusiva). A data de fim MUST ser igual ou posterior ao início da regra; se o produto não tiver regra em aberto ou a data for inválida, a operação MUST ser rejeitada sem alterar as regras.

#### Scenario: Encerrar e cadastrar a alíquota seguinte
- **GIVEN** o produto `P1` tem regra de alíquota `10` a partir de `2026-01-01`, em aberto
- **WHEN** a vigência de `P1` é encerrada em `2026-06-30`
- **AND** é cadastrada para `P1` a regra de alíquota `12` a partir de `2026-07-01`
- **THEN** ambas as operações são aceitas
- **AND** em `2026-06-30` vale a alíquota `10` e em `2026-07-01` vale a alíquota `12`

#### Scenario: Produto sem regra em aberto
- **GIVEN** o produto `P1` só tem regra de `2026-01-01` até `2026-06-30`
- **WHEN** a vigência de `P1` é encerrada em `2026-12-31`
- **THEN** a operação é rejeitada indicando que não há regra em aberto

#### Scenario: Produto sem regras
- **WHEN** a vigência do produto `P9`, sem nenhuma regra, é encerrada em `2026-12-31`
- **THEN** a operação é rejeitada indicando que não há regra em aberto

#### Scenario: Data de fim anterior ao início da regra
- **GIVEN** o produto `P1` tem regra a partir de `2026-07-01`, em aberto
- **WHEN** a vigência de `P1` é encerrada em `2026-06-30`
- **THEN** a operação é rejeitada indicando período inválido
- **AND** a regra continua em aberto
