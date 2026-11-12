# categorias-de-produto Specification

## Purpose

Associa cada produto a no máximo uma categoria, permitindo que regras de alíquota cadastradas para a categoria se apliquem a todos os seus produtos.

## Requirements

### Requirement: Atribuir categoria a um produto
O sistema SHALL permitir atribuir uma categoria a um produto informando o código do produto e o código da categoria. Cada produto pertence a no máximo uma categoria, sem período de vigência: atribuir uma nova categoria substitui a anterior e vale para todos os cálculos feitos a partir de então, em qualquer data de referência. Códigos de produto e de categoria são independentes entre si.

#### Scenario: Atribuir categoria
- **WHEN** a categoria `A` é atribuída ao produto `P7`
- **THEN** a atribuição é aceita e `P7` pertence à categoria `A`

#### Scenario: Trocar a categoria
- **GIVEN** o produto `P7` pertence à categoria `A`
- **WHEN** a categoria `B` é atribuída ao produto `P7`
- **THEN** a atribuição é aceita e `P7` passa a pertencer somente à categoria `B`

#### Scenario: Vários produtos na mesma categoria
- **WHEN** a categoria `A` é atribuída aos produtos `P7` e `P8`
- **THEN** ambos pertencem à categoria `A`

#### Scenario: Produto sem categoria
- **WHEN** nenhuma categoria foi atribuída ao produto `P1`
- **THEN** `P1` não pertence a nenhuma categoria e só regras do próprio produto se aplicam a ele

### Requirement: Validar atribuição de categoria
O sistema MUST rejeitar a atribuição, com um erro que identifique o motivo e sem alterar a categoria atual do produto, quando o código do produto ou o código da categoria estiver ausente ou em branco.

#### Scenario: Categoria em branco
- **GIVEN** o produto `P7` pertence à categoria `A`
- **WHEN** uma categoria em branco é atribuída ao produto `P7`
- **THEN** a atribuição é rejeitada indicando categoria inválida
- **AND** `P7` continua pertencendo à categoria `A`

#### Scenario: Produto em branco
- **WHEN** a categoria `A` é atribuída a um código de produto em branco
- **THEN** a atribuição é rejeitada indicando produto inválido
