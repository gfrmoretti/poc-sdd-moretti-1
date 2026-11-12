# calculo-de-imposto Specification

## Purpose

Calcula o imposto devido sobre um valor-base para um produto numa data, aplicando a alíquota da regra vigente naquela data.

## Requirements

### Requirement: Calcular imposto pela regra vigente
O sistema SHALL calcular o imposto a partir do código do produto, da data de referência e do valor-base, usando a regra do produto vigente na data (início e fim inclusivos, fim ausente = em aberto) ou, se não houver, a regra da categoria do produto vigente na data. A regra do produto prevalece sobre a da categoria. O resultado MUST conter a alíquota aplicada (em percentual, como cadastrada), o valor do imposto, a origem da regra (produto ou categoria) e se o teto foi aplicado.

#### Scenario: Data dentro de período fechado
- **GIVEN** o produto `P1` tem regra de alíquota `10` de `2026-01-01` até `2026-06-30`
- **WHEN** o imposto de `P1` é calculado em `2026-03-15` sobre `100.00`
- **THEN** o resultado tem alíquota `10` e valor do imposto `10.00`

#### Scenario: Data no limite do período
- **GIVEN** o produto `P1` tem regra de alíquota `10` de `2026-01-01` até `2026-06-30` e regra de alíquota `12` a partir de `2026-07-01`
- **WHEN** o imposto de `P1` é calculado sobre `100.00` em `2026-06-30` e em `2026-07-01`
- **THEN** em `2026-06-30` o resultado tem alíquota `10` e imposto `10.00`
- **AND** em `2026-07-01` o resultado tem alíquota `12` e imposto `12.00`

#### Scenario: Data distante numa regra em aberto
- **GIVEN** o produto `P1` tem regra de alíquota `12` a partir de `2026-07-01`, em aberto
- **WHEN** o imposto de `P1` é calculado em `2030-01-01` sobre `100.00`
- **THEN** o resultado tem alíquota `12` e valor do imposto `12.00`

#### Scenario: Produto isento
- **GIVEN** o produto `P3` tem regra de alíquota `0` a partir de `2026-01-01`
- **WHEN** o imposto de `P3` é calculado em `2026-05-01` sobre `100.00`
- **THEN** o resultado tem alíquota `0` e valor do imposto `0.00`

#### Scenario: Regra de produto informa a origem
- **GIVEN** o produto `P1`, sem categoria, tem regra de alíquota `10` a partir de `2026-01-01`, sem teto
- **WHEN** o imposto de `P1` é calculado em `2026-05-01` sobre `100.00`
- **THEN** o resultado tem origem produto e teto não aplicado

#### Scenario: Produto usa a regra da sua categoria
- **GIVEN** o produto `P7` pertence à categoria `A`, sem regra própria
- **AND** a categoria `A` tem regra de alíquota `14` a partir de `2028-01-01`
- **WHEN** o imposto de `P7` é calculado em `2028-03-10` sobre `5000.00`
- **THEN** o resultado tem alíquota `14`, valor do imposto `700.00` e origem categoria

#### Scenario: Regra do produto prevalece sobre a da categoria
- **GIVEN** o produto `P8` pertence à categoria `A`
- **AND** a categoria `A` tem regra de alíquota `14` a partir de `2028-01-01`
- **AND** o produto `P8` tem regra de alíquota `5` de `2028-01-01` até `2028-06-30`
- **WHEN** o imposto de `P8` é calculado sobre `100.00` em `2028-03-10` e em `2028-07-01`
- **THEN** em `2028-03-10` o resultado tem alíquota `5`, imposto `5.00` e origem produto
- **AND** em `2028-07-01` o resultado tem alíquota `14`, imposto `14.00` e origem categoria

#### Scenario: Troca de categoria muda a regra aplicada
- **GIVEN** a categoria `A` tem regra de alíquota `14` e a categoria `B` tem regra de alíquota `8`, ambas a partir de `2028-01-01`
- **AND** o produto `P7` pertence à categoria `A`
- **WHEN** a categoria `B` é atribuída a `P7` e o imposto de `P7` é calculado em `2028-03-10` sobre `100.00`
- **THEN** o resultado tem alíquota `8` e valor do imposto `8.00`

### Requirement: Arredondar o valor do imposto
Antes da aplicação do teto, o valor do imposto MUST ser `valor-base × alíquota / 100`, arredondado uma única vez, ao final, para 2 casas decimais com HALF_UP (metade arredonda para cima). O cálculo MUST usar aritmética decimal exata, sem erro de ponto flutuante.

#### Scenario: Arredondamento para cima na metade
- **GIVEN** o produto `P1` tem alíquota vigente `10`
- **WHEN** o imposto de `P1` é calculado sobre `0.05`
- **THEN** o valor do imposto é `0.01` (0,005 arredondado para cima)

#### Scenario: Arredondamento para cima acima da metade
- **GIVEN** o produto `P1` tem alíquota vigente `12`
- **WHEN** o imposto de `P1` é calculado sobre `10.05`
- **THEN** o valor do imposto é `1.21` (1,206 arredondado)

#### Scenario: Arredondamento para baixo
- **GIVEN** o produto `P1` tem alíquota vigente `12`
- **WHEN** o imposto de `P1` é calculado sobre `10.04`
- **THEN** o valor do imposto é `1.20` (1,2048 arredondado)

#### Scenario: Alíquota decimal
- **GIVEN** o produto `P2` tem alíquota vigente `7.5`
- **WHEN** o imposto de `P2` é calculado sobre `33.33`
- **THEN** o valor do imposto é `2.50` (2,49975 arredondado)

#### Scenario: Valor-base zero
- **GIVEN** o produto `P1` tem alíquota vigente `12`
- **WHEN** o imposto de `P1` é calculado sobre `0`
- **THEN** o valor do imposto é `0.00`

### Requirement: Rejeitar cálculo sem regra vigente
O sistema MUST rejeitar o cálculo com um erro que identifique o produto e a data quando não houver, naquela data, regra vigente nem para o produto nem para a categoria do produto. O sistema MUST NOT assumir alíquota zero nesse caso.

#### Scenario: Data antes da primeira regra
- **GIVEN** o produto `P1` tem regra a partir de `2026-01-01`
- **WHEN** o imposto de `P1` é calculado em `2025-12-31` sobre `100.00`
- **THEN** o cálculo é rejeitado indicando que não há alíquota vigente para `P1` em `2025-12-31`

#### Scenario: Data num intervalo sem regra
- **GIVEN** o produto `P1` tem regra de `2026-01-01` até `2026-06-30` e regra a partir de `2026-09-01`
- **WHEN** o imposto de `P1` é calculado em `2026-08-01` sobre `100.00`
- **THEN** o cálculo é rejeitado indicando que não há alíquota vigente para `P1` em `2026-08-01`

#### Scenario: Produto sem regras
- **WHEN** o imposto do produto `P9`, sem nenhuma regra, é calculado em `2026-01-01` sobre `100.00`
- **THEN** o cálculo é rejeitado indicando que não há alíquota vigente para `P9` em `2026-01-01`

#### Scenario: Data antes da regra da categoria
- **GIVEN** o produto `P7` pertence à categoria `A`, sem regra própria
- **AND** a categoria `A` tem regra a partir de `2028-01-01`
- **WHEN** o imposto de `P7` é calculado em `2027-12-31` sobre `100.00`
- **THEN** o cálculo é rejeitado indicando que não há alíquota vigente para `P7` em `2027-12-31`

#### Scenario: Categoria sem regras
- **GIVEN** o produto `P7` pertence à categoria `Z`, e nem `P7` nem `Z` têm regras
- **WHEN** o imposto de `P7` é calculado em `2028-01-01` sobre `100.00`
- **THEN** o cálculo é rejeitado indicando que não há alíquota vigente para `P7` em `2028-01-01`

### Requirement: Validar entrada do cálculo
O sistema MUST rejeitar o cálculo, com um erro que identifique o motivo, quando o código do produto estiver ausente ou em branco, a data estiver ausente ou o valor-base estiver ausente ou for negativo.

#### Scenario: Valor-base negativo
- **GIVEN** o produto `P1` tem alíquota vigente `12`
- **WHEN** o imposto de `P1` é calculado sobre `-1.00`
- **THEN** o cálculo é rejeitado indicando valor-base inválido

#### Scenario: Data ausente
- **WHEN** o imposto de `P1` é calculado sem data de referência
- **THEN** o cálculo é rejeitado indicando data inválida

#### Scenario: Produto em branco
- **WHEN** o imposto é calculado com código de produto em branco
- **THEN** o cálculo é rejeitado indicando produto inválido

### Requirement: Aplicar teto do imposto
Quando a regra aplicada tiver teto, o valor do imposto MUST ser o menor entre o valor arredondado e o teto, sempre com 2 casas decimais. O resultado MUST indicar teto aplicado somente quando o valor arredondado exceder o teto. O teto vale apenas para a regra que o define.

#### Scenario: Imposto abaixo do teto
- **GIVEN** o produto `P7` pertence à categoria `A`, que tem regra de alíquota `14` a partir de `2028-01-01` com teto `1000.00`
- **WHEN** o imposto de `P7` é calculado em `2028-03-10` sobre `5000.00`
- **THEN** o valor do imposto é `700.00` e o teto não foi aplicado

#### Scenario: Imposto acima do teto
- **GIVEN** o produto `P7` pertence à categoria `A`, que tem regra de alíquota `14` a partir de `2028-01-01` com teto `1000.00`
- **WHEN** o imposto de `P7` é calculado em `2028-03-10` sobre `10000.00`
- **THEN** o resultado tem alíquota `14`, valor do imposto `1000.00` e teto aplicado

#### Scenario: Imposto exatamente igual ao teto
- **GIVEN** o produto `P7` pertence à categoria `A`, que tem regra de alíquota `14` a partir de `2028-01-01` com teto `1000.00`
- **WHEN** o imposto de `P7` é calculado em `2028-03-10` sobre `7142.86`
- **THEN** o valor do imposto é `1000.00` (1000,0004 arredondado) e o teto não foi aplicado

#### Scenario: Teto sem casas decimais
- **GIVEN** o produto `P1` tem regra de alíquota `10` a partir de `2026-01-01` com teto `50`
- **WHEN** o imposto de `P1` é calculado em `2026-05-01` sobre `1000.00`
- **THEN** o valor do imposto é `50.00` e o teto foi aplicado

#### Scenario: Teto não vale fora da sua regra
- **GIVEN** a categoria `A` tem regra de alíquota `10` de `2026-01-01` até `2027-12-31`, sem teto, e regra de alíquota `14` a partir de `2028-01-01` com teto `1000.00`
- **AND** o produto `P7` pertence à categoria `A`
- **WHEN** o imposto de `P7` é calculado em `2027-12-31` sobre `20000.00`
- **THEN** o valor do imposto é `2000.00` e o teto não foi aplicado

#### Scenario: Teto da categoria não vale quando a regra do produto prevalece
- **GIVEN** o produto `P8` pertence à categoria `A`, que tem regra de alíquota `14` a partir de `2028-01-01` com teto `1000.00`
- **AND** o produto `P8` tem regra própria de alíquota `14` a partir de `2028-01-01`, sem teto
- **WHEN** o imposto de `P8` é calculado em `2028-03-10` sobre `10000.00`
- **THEN** o valor do imposto é `1400.00`, com origem produto e teto não aplicado
