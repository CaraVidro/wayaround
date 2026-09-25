# Spectrum Audit V1

Base auditada: `feature/spectrum-polish`.

## Regra

Antes de criar qualquer sistema, o código existente foi rastreado. O objetivo
desta branch é integrar e criar uma camada comum, não duplicar managers.

## O que já estava implementado

- **Blue**: summon, carga, distância pela rodinha, hold/orbit/launch e
  encerramento server-authoritative.
- **Red**: preparado na frente do jogador, partículas de spawn, modo máximo
  visual/sonoro sem crescer, lançamento rápido, destruição do caminho, wake de
  partículas e detonação própria.
- **Purple**: já contorna o limite prático da explosão vanilla com
  `pulverizeEllipsoid`, shockwave e sparkles; a cratera não depende apenas da
  magnitude passada para `Level.explode`.
- **Desmartelar**: já usa vários planos cortantes móveis em offsets, alturas e
  ângulos diferentes. O modo fogo já propaga fogo para alvos/ambiente.
- **Fuga**: frase pessoal persistente, carga, comando final `fuga`, projétil,
  pulverização/cratera e pilar de fogo visível a longa distância.
- **Immortal Wheel**: já possui duas reativações antes da morte final e
  reconstrução progressiva com a roda em estado de spin.
- **Domínio da Justiça**: a abertura já toca `ANVIL_LAND` e `ANVIL_USE`,
  funcionando como a batida de martelo do tribunal.
- **Creative tab**: o nome **Spectrums** já existia e BLUE já estava escondido.

## Problemas encontrados

1. Void, Tukuna e Justice repetiam loops de inventário para descobrir posse.
2. O Void Spectrum ainda era um `Item` genérico.
3. Voice Intent, Infinity, Void Domain, Tukuna, Justice e o cliente do Blue
   tinham caminhos diferentes para responder à mesma pergunta: "qual Spectrum
   este jogador possui?"
4. O reconhecimento especulativo entrava em modo atento em `desmar`, mas um
   transcript final contendo somente `desmar` não executava o golpe.

## Framework V1

- `SpectrumType`: VOID, TUKUNA e JUSTICE.
- `SpectrumItem`: base tipada para itens de Spectrum.
- `SpectrumAccess`: acesso centralizado à posse/remoção de Spectrum.
- `SpectrumAbility`: contrato mínimo para novas habilidades.
- `VoidSpectrumItem`: controle físico contextual para Blue/Red.

Os managers especializados continuam donos do estado e comportamento. Não foi
criado um segundo Blue, um segundo sistema de domínio ou um segundo Voice Chat.

## Ajustes desta branch

- Void + Blue ativa + clique direito: encerra sem mensagem.
- Rodinha continua controlando distância pelo sistema atual do Blue.
- Red preparado + clique direito: lança pelo mesmo manager usado pela voz.
- `desmar`: fica atento durante reconhecimento parcial e, se a fala terminar
  ali, executa Desmartelar.
- Aba **Spectrums** agora contém Void, Tukuna, Justice, Immortal Wheel, dedos do
  Tukuna e a lâmina de execução. Priorite sai da aba de poderes.
- BLUE continua registrado para compatibilidade/debug, mas não aparece na aba.
