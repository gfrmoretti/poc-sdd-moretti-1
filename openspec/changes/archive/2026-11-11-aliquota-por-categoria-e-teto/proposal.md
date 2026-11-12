# Proposal

## Why

A partir de 2028-01-01 os produtos da categoria A passam a ter alíquota de 14%, com imposto limitado a R$ 1000,00 por cálculo. Hoje as alíquotas só podem ser cadastradas produto a produto e o imposto não tem limite, então essa regra não pode ser representada: seria preciso repetir a regra em cada produto da categoria e não haveria como aplicar o teto.

## What Changes

- Produtos passam a poder pertencer a uma categoria (no máximo uma, sem vigência; a atribuição pode ser trocada).
- Regras de alíquota passam a poder ser cadastradas para uma categoria, valendo para todos os produtos dela, com as mesmas regras de vigência, validação, sobreposição (entre regras da mesma categoria) e encerramento de vigência das regras de produto.
- Toda regra (de produto ou de categoria) passa a aceitar um teto opcional, em reais, para o valor do imposto.
- O cálculo passa a procurar primeiro a regra do produto vigente na data e, se não houver, a regra da categoria do produto vigente na data. Regra de produto e regra de categoria podem coexistir no mesmo período; a do produto prevalece.
- O valor do imposto, depois de arredondado, é limitado ao teto da regra aplicada, quando houver.
- O resultado do cálculo passa a informar também a origem da regra aplicada (produto ou categoria) e se o teto foi aplicado.
- Compatível com o comportamento atual: regras de produto sem teto e produtos sem categoria continuam calculando exatamente como hoje.

O cadastro de 14% com teto de 1000 para a categoria A em 2028 é feito pelas operações acima (é dado, não código).

## Capabilities

### New Capabilities

- `categorias-de-produto`: associação de cada produto a uma categoria, usada para localizar regras de alíquota de categoria.

### Modified Capabilities

- `cadastro-de-aliquotas`: novos requisitos para regras de categoria (cadastro, sobreposição, encerramento de vigência) e para o teto opcional da regra; validação do código de produto ao encerrar vigência (código em branco passa a ser rejeitado como produto inválido).
- `calculo-de-imposto`: seleção da regra passa a considerar a categoria do produto com precedência do produto; aplicação do teto após o arredondamento; resultado ganha origem da regra e indicação de teto aplicado; erro de "sem regra vigente" passa a considerar também a categoria.

## Impact

- Código em `com.pocsdd.imposto`: modelo da regra (alvo produto/categoria e teto), resultado do cálculo, repositório de regras, novo armazenamento da categoria dos produtos e novas operações no `ServicoDeImposto`.
- As operações e os testes atuais continuam válidos sem alteração (assinaturas existentes mantidas).
- Sem novas dependências.
