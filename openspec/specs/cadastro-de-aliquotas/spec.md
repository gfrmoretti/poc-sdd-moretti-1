# cadastro-de-aliquotas Specification

## Purpose

Mantém as regras de alíquota de imposto por produto ou por categoria de produto, cada uma válida num período de vigência e com teto opcional para o valor do imposto, garantindo que em qualquer data exista no máximo uma regra aplicável por produto e no máximo uma por categoria.

## Requirements

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
O sistema SHALL permitir encerrar a regra em aberto de um produto informando o código do produto e a data de fim (inclusiva). A data de fim MUST ser igual ou posterior ao início da regra; se o produto não tiver regra em aberto ou a data for inválida, a operação MUST ser rejeitada sem alterar as regras. Código de produto ausente ou em branco MUST ser rejeitado indicando produto inválido.

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

#### Scenario: Produto em branco ao encerrar
- **WHEN** a vigência é encerrada em `2026-12-31` para um código de produto em branco
- **THEN** a operação é rejeitada indicando produto inválido

### Requirement: Cadastrar regra de alíquota para categoria
O sistema SHALL permitir cadastrar uma regra de alíquota para uma categoria, com os mesmos dados, vigência e validações da regra de produto, informando o código da categoria no lugar do código do produto. Código de categoria ausente ou em branco MUST ser rejeitado indicando categoria inválida.

#### Scenario: Regra de categoria em aberto
- **WHEN** é cadastrada a regra da categoria `A` com alíquota `14`, a partir de `2028-01-01`, sem data de fim
- **THEN** o cadastro é aceito
- **AND** a regra se aplica à categoria `A` em `2028-01-01` e em qualquer data posterior

#### Scenario: Categoria em branco
- **WHEN** é cadastrada uma regra com alíquota `14`, a partir de `2028-01-01`, para um código de categoria em branco
- **THEN** o cadastro é rejeitado indicando categoria inválida

#### Scenario: Dados inválidos na regra de categoria
- **WHEN** é cadastrada a regra da categoria `A` com alíquota `-1`, a partir de `2028-01-01`
- **THEN** o cadastro é rejeitado indicando alíquota inválida

### Requirement: Proibir sobreposição de vigência entre regras da mesma categoria
O sistema MUST rejeitar o cadastro de uma regra de categoria cujo período compartilhe ao menos um dia com outra regra da mesma categoria, mantendo as regras existentes inalteradas. Regras de categorias diferentes nunca conflitam, e uma regra de categoria nunca conflita com uma regra de produto.

#### Scenario: Sobreposição entre regras da mesma categoria
- **GIVEN** a categoria `A` tem regra a partir de `2028-01-01`, em aberto
- **WHEN** é cadastrada para `A` uma regra de `2029-01-01` até `2029-12-31`
- **THEN** o cadastro é rejeitado indicando sobreposição de vigência

#### Scenario: Regra de categoria e regra de produto no mesmo período
- **GIVEN** o produto `P7` tem regra a partir de `2028-01-01`, em aberto
- **WHEN** é cadastrada para a categoria `A` uma regra a partir de `2028-01-01`, em aberto
- **THEN** o cadastro é aceito

#### Scenario: Categorias diferentes no mesmo período
- **GIVEN** a categoria `A` tem regra a partir de `2028-01-01`, em aberto
- **WHEN** é cadastrada para a categoria `B` uma regra a partir de `2028-01-01`, em aberto
- **THEN** o cadastro é aceito

### Requirement: Encerrar vigência em aberto de categoria
O sistema SHALL permitir encerrar a regra em aberto de uma categoria informando o código da categoria e a data de fim (inclusiva), com as mesmas regras do encerramento de regra de produto: a data de fim MUST ser igual ou posterior ao início da regra, e a operação MUST ser rejeitada sem alterar as regras se a categoria não tiver regra em aberto ou a data for inválida. Código de categoria ausente ou em branco MUST ser rejeitado indicando categoria inválida.

#### Scenario: Encerrar e cadastrar a alíquota seguinte da categoria
- **GIVEN** a categoria `A` tem regra de alíquota `10` a partir de `2026-01-01`, em aberto
- **WHEN** a vigência de `A` é encerrada em `2027-12-31`
- **AND** é cadastrada para `A` a regra de alíquota `14` a partir de `2028-01-01`
- **THEN** ambas as operações são aceitas

#### Scenario: Categoria sem regra em aberto
- **WHEN** a vigência da categoria `Z`, sem nenhuma regra, é encerrada em `2028-12-31`
- **THEN** a operação é rejeitada indicando que não há regra em aberto

#### Scenario: Data de fim anterior ao início da regra da categoria
- **GIVEN** a categoria `A` tem regra a partir de `2028-01-01`, em aberto
- **WHEN** a vigência de `A` é encerrada em `2027-12-31`
- **THEN** a operação é rejeitada indicando período inválido
- **AND** a regra continua em aberto

#### Scenario: Categoria em branco ao encerrar
- **WHEN** a vigência é encerrada em `2028-12-31` para um código de categoria em branco
- **THEN** a operação é rejeitada indicando categoria inválida

### Requirement: Definir teto do imposto na regra
O sistema SHALL permitir informar, ao cadastrar uma regra de produto ou de categoria, um teto opcional em reais para o valor do imposto calculado por essa regra. O teto MUST ser maior que zero e ter no máximo 2 casas decimais; caso contrário o cadastro MUST ser rejeitado indicando teto inválido. O teto MUST ser registrado com exatamente 2 casas decimais. Uma regra sem teto não limita o imposto. Ao encerrar a vigência, o teto da regra é mantido.

#### Scenario: Regra de categoria com teto
- **WHEN** é cadastrada a regra da categoria `A` com alíquota `14`, a partir de `2028-01-01`, com teto `1000.00`
- **THEN** o cadastro é aceito com teto de R$ 1000,00

#### Scenario: Regra de produto com teto
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `10`, a partir de `2026-01-01`, com teto `50`
- **THEN** o cadastro é aceito com teto de R$ 50,00

#### Scenario: Teto zero
- **WHEN** é cadastrada a regra da categoria `A` com alíquota `14`, a partir de `2028-01-01`, com teto `0`
- **THEN** o cadastro é rejeitado indicando teto inválido

#### Scenario: Teto negativo
- **WHEN** é cadastrada a regra do produto `P1` com alíquota `10`, a partir de `2026-01-01`, com teto `-1`
- **THEN** o cadastro é rejeitado indicando teto inválido

#### Scenario: Teto com mais de 2 casas decimais
- **WHEN** é cadastrada a regra da categoria `A` com alíquota `14`, a partir de `2028-01-01`, com teto `1000.001`
- **THEN** o cadastro é rejeitado indicando teto inválido

#### Scenario: Encerrar vigência mantém o teto
- **GIVEN** a categoria `A` tem regra de alíquota `14` a partir de `2028-01-01`, em aberto, com teto `1000.00`
- **WHEN** a vigência de `A` é encerrada em `2028-12-31`
- **THEN** a regra encerrada continua com teto de R$ 1000,00
