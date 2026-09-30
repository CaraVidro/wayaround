# Britadores montáveis V1

Base: `feature/pipework-industrial-v1`, Minecraft 1.21.1 / NeoForge 21.1.

Os documentos em `docs/design/` são a base de design. O código existente é a
fonte para identificar o estado implementado. Não foi criado outro Assembly:
usamos AssemblyMachine/AssemblyPartProfile, MechanicalTransmission e
SmoothObjectAnimation existentes.

## Três mecanismos

| Estrutura | Ferramenta | Entrada com funil estreito/largo | Lote estreito/largo | Apoios reais |
| --- | --- | ---: | ---: | --- |
| Pequena | Mandíbula reciprocante | 1 / 1 | 1 / 1 | Fundação sólida sob o núcleo |
| Média | Dois rolos contrarrotativos | 64 / 128 | 8 / 16 | Dois apoios laterais, cada um com fundação |
| Grande | Tambores contrarrotativos dentados | 256 / 512 | 32 / 64 | Quatro apoios de ferro nos cantos de uma base 3x3 |

A capacidade é armazenamento de entradas. O lote é o máximo processado por ciclo;
RPM, potência disponível, dureza e peças determinam a duração. O grande não é
uma ampliação de textura: tem dentes, travessas, base larga e fundação composta.
O volume sólido dos apoios pertence a blocos reais colocados separadamente.

## Montagem no mundo

1. Fabrique e coloque apenas a estrutura. Ela começa sem peças funcionais.
2. Instale um eixo, um conjunto de mancais, a ferramenta da escala correta e um
   funil, clicando com cada item. Cada instalação consome uma peça real.
3. Médio voltado ao norte: apoios a leste e oeste do núcleo. Grande: apoios nos
   quatro cantos diagonais. Coloque blocos sólidos sob núcleo e apoios.
4. Ligue a transmissão pela frente/traseira. O pequeno pode receber a manivela
   existente; máquinas maiores exigem mais potência/torque.
5. Jogue os itens dentro da entrada ou clique com material aceito. Entradas
   cheias ou incompletas não consomem o excedente.
6. Mão vazia recolhe produtos. Shift + mão vazia retira a peça da região clicada
   (alto: funil, centro: ferramenta, baixo: eixo/mancal), com a transmissão parada.
   Se a região estiver vazia, retira a última peça instalada.
7. O Guia Assembly existente inspeciona o grafo de peças e apoios.

## Peças e contrapartidas

| Componente | Vantagem | Custo |
| --- | --- | --- |
| Eixo leve | Resposta rápida, menor consumo | Menor resistência à carga e desgaste maior |
| Eixo reforçado | Resistência, menor desgaste | Mais potência, aceleração mais lenta, menor velocidade de trabalho |
| Mancal de ferro | Resiste a impacto/desgaste | Mais atrito |
| Bucha de cobre | Menos atrito | Desgasta mais sob carga |
| Funil estreito | Alimentação controlada e menor carga | Armazenamento/lote menores |
| Funil largo | Entrada e lote maiores | Maior demanda e desgaste da alimentação |
| Ferramenta simples | Leve e rápida | Menor tolerância a materiais duros |
| Ferramenta reforçada | Aceita carga mais dura, menor desgaste | Mais potência e menor velocidade |
| Apoio de madeira | Barato | Menor capacidade de carga |
| Apoio de ferro | Suporta mais carga | Mais material para fabricar; obrigatório no grande |

Cada peça possui modelo próprio no inventário, receita e representação instalada.
O perfil de fabricação recebido de AssemblyItemData é mantido. Desgaste e fadiga
são aplicados ao ItemStack instalado, e não só a uma barra abstrata da máquina.
Calor, atrito, torque insuficiente, apoios ausentes e ferramenta gasta podem
interromper o trabalho. Remover ou substituir peças recupera as mesmas peças,
incluindo seu desgaste. Quebrar a estrutura derruba peças, entradas e produtos.

## Materiais

Ferro, cobre e ouro brutos/minérios viram material britado. Minérios profundos
exigem mais resistência. Um bloco de metal bruto representa nove unidades,
conservadas no resultado. Pedra/cascalho têm etapas de britagem próprias.

Os três materiais britados podem ser fundidos em barras vanilla. A rota vanilla
original permanece disponível. Lavagem, separação de rejeitos e ferro industrial
de média/alta qualidade ainda não estão implementados nesta etapa.

## Cargas conectadas: contrato futuro, não mecânica pronta

`offerConnectedBatch` é uma entrada atômica e limitada para o grande. Só aceita
materiais compatíveis e capacidade disponível. Rejeição mantém o payload e a
máquina intactos; sucesso transfere as cópias e consome os stacks entregues.

Ela não minera remotamente, não procura estruturas, não remove blocos do mundo,
nem reconhece aglomerados físicos ainda. A futura infraestrutura de transporte
precisará entregar a estrutura, convertê-la com conservação de conteúdo e então
usar esse contrato. Hoje o grande aceita itens/blocos dropados e lotes entregues
pela API, sem simular o futuro transporte de blocos colados.

## Aplicação a outros maquinários

O moinho existente foi convertido para instalação física de eixo, mancais,
funil e mós, usando a mesma coleção de peças. Mós simples e com cintas de ferro
possuem contrapartidas de desgaste/potência/velocidade. A receita agora produz
somente a estrutura; o renderizador mostra cada peça apenas após sua instalação.

Moinhos antigos são migrados ao carregar: conservam grãos e farinha e recebem
as peças equivalentes à montagem que já possuíam. O marcador do novo inventário
impede que um moinho desmontado regenere peças ao reabrir o mundo.

Roda d'água e serraria já têm partes reais em suas arquiteturas existentes. Os
outros maquinários ainda usam a ponte LegacyMachineAssembly: não foram
convertidos silenciosamente neste patch. O catálogo compartilhado oferece a
base para migrá-los um a um, preservando seus saves e sistemas próprios.

## Limites e validação

- Até 16 pilhas de entrada, 16 de saída e 512 itens na saída.
- Pilhas são divididas pelo tamanho máximo real; saída cheia pausa o processo.
- A entrada de itens é verificada a cada cinco ticks numa caixa local.
- Não há uma entidade para cada peça móvel e não há escaneamento remoto.
- Perfis e inventários sobrevivem ao salvamento e à desmontagem.
- Regressões puras verificam conservação de lotes, escala e contrapartidas.
- GameTests verificam montagem vazia, entrada pequena, fundação do médio,
  payload atômico do grande, perfis persistentes, britagem com manivela e
  migração de moinhos antigos.

V1 tem catálogos de peças e encaixes conhecidos. Montagem livre arbitrária,
colisões entre conjuntos móveis, transporte de aglomerados e falhas estruturais
com detritos continuam etapas futuras; esta versão não finge simulá-las.
