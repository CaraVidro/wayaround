# Geografia e expedições — WayAround 1.6

Minecraft / NeoForge 1.21.1, protocolo 33. Implementação própria; Terralith e Lithosphere são referências de escala/ritmo visual, sem copiar seus dados ou exigir esses mods.

## Mundo novo e migração

O menu existente de criação do mundo ganha `Continents & Massifs`, `Round World`, `Unseen Expeditions` e `Lost After Death`. Os três primeiros vêm ligados em mundos novos; respawn distante vem desligado. As escolhas são salvas por mundo e enviadas aos clientes. As opções de geração não podem ser alteradas pelos comandos de gameplay.

Saves anteriores mantêm geografia e tamanho antigos: campos de geração ausentes são carregados como desligados. As opções são lidas **antes** de criar os níveis/RandomState, inclusive no servidor dedicado, para evitar gerar spawn com configurações diferentes das regiões seguintes. Um save antigo sem SavedData é reconhecido pela presença de arquivos de região. Leitura inválida preserva geração antiga e registra erro. Não reescreve chunks existentes. Para usar o relevo novo, crie um mundo novo.

## Escala e relevo

Continentalness é amostrado em escala horizontal 6× (anterior: 4×, vinculado à ecologia); temperatura/umidade 3×, erosão 4×, ridges 3×. O tamanho efetivo de cada bioma depende dos campos e da seed, não há um diâmetro único garantido. Todos os controles são independentes da ativação de vegetação.

Um novo campo de relevo usa os ruídos climáticos **seeded** de Minecraft: prateleiras submarinas, praias contínuas, planícies/planaltos, vales baixos e maciços largos com superfície até aproximadamente Y=286. Há oceanos grandes e pequenos, terra contínua e ilhas conforme continentalness; não transforma todo o mapa em mar. A profundidade extra do sistema deep ocean continua funcionando. A superfície do mar permanece Y=63, com água até Y=62. Biomas vanilla de altitude são escolhidos conforme altura e temperatura; biomas customizados do WayAround são preservados. A integração se limita ao noise overworld de pedra/água/nível do mar padrão; Nether, End, Nexus e colônias não recebem o novo relevo.

A density preliminar acompanha a final, para aquifers e superfície não discordarem. Noise caves vanilla são preservadas abaixo de Y=30, com transição até Y=54; o interior dos maciços altos fica mais sólido, além dos carvers normais. Roteamento de minério e decoração seguem os sistemas existentes. O relevo usa 64 colunas em cache por worker, sem cache global dependente da seed. Wrappers de escala/periodicidade atuam em folhas de ruído, preservando marcadores de cache/interpolação.

Antártica ganha um continente fechado e irregular, centrado aproximadamente em X=0/Z=22.528, com semieixos de 14.500/6.200 blocos, cercado pelo oceano austral. Não cresce indefinidamente para Z positivo. Clima periódico cria faixas frias nos dois hemisférios. Vulcões e great rifts permanecem integrados; sua influência some suavemente perto da costura para evitar um corte artificial. Minas abertas continuam consultando a superfície natural local, retirando o teto, evitando água e usando apenas regiões já carregadas; não têm altitude fixa nem são recriadas.

## Volta pelas bordas

Com `Round World`, o domínio explorável tem **65.536 × 65.536 blocos**, X/Z em `[-32768, 32768)`. Cruzar +X retorna a -X; cruzar -X retorna a +X. O mesmo ocorre em Z, inclusive em diagonal, mantendo o excedente fracionário, orientação, velocidade, altura, passageiros e identidade/inventário do veículo. A altura Y não dá a volta: norte/sul refere-se ao mapa. A topologia dos dois eixos é um toro, não uma simulação de esfera com latitude/longitude reais.

O destino são os chunks reais do outro lado, com construções/modificações salvas. Não cria um segundo continente para cada passagem. O ruído é periódico; a costura recebe mistura suave de 1.024 blocos. Perto da borda, jogadores e veículos ocupados preparam uma vizinhança de destino 3×3. No cruzamento, aguarda disponibilidade dos chunks sem bloquear o servidor em `join`. Até quatro pedidos de chunk por tick, 16 destinos ativos, prazo/tickets de 200 ticks e liberação ao concluir, sair ou fechar o servidor. Um veículo é movido com sua árvore de passageiros, sem descartar e recriar entidades.

**Limite visual:** a travessia muda as coordenadas e o conjunto de chunks mostrado pelo cliente. Pode haver breve troca visual/carregamento. Não implementa um renderer toroidal que mostre entidades e construções do outro lado antes do cruzamento. Minecraft pode manter uma faixa técnica de chunks de visualização além do domínio; ela não constitui nova área explorável. Estruturas/decoradores vanilla não são reescritos para atravessar a costura nem têm alterações espelhadas nessa faixa. É um mundo finito para viagem, sem impor um formato de armazenamento novo aos arquivos vanilla.

## Morte e renascimento

`Unseen Expeditions` filtra exclusivamente destinatários das mensagens vanilla de morte: mesma dimensão, até 48 blocos, olhando em direção à vítima (cone conservador de 35°) e sem parede na linha de visão. Proximidade sozinha não basta. Preserva `showDeathMessages`, filtros de equipe, tela de morte do próprio jogador, drops e demais consequências. Spectate remoto/Entity Spectate não serve como testemunha. A verificação usa posição/olhar do servidor, não uma captura da câmera; FOV, terceira pessoa ou mods visuais podem criar diferenças. Não envia aviso global de expedição desaparecida nem altera mensagens de login/logout, chat manual ou informações de plugins externos.

`Lost After Death` prepara terra aleatória no overworld, pelo menos 2.048 blocos distante da morte e da cama (distância curta pela costura quando finito). Procura solo completo, duas posições de ar, sem água/lava, magma ou cacto e dentro da borda vanilla. A busca e os chunks são assíncronos/bounded; até 32 tentativas, 32 candidatos climáticos por rodada e 81 pontos locais por destino. A tela de morte aguarda terra segura após a solicitação explícita do jogador. Não respawna na cama para teleportar depois. Mantém a cama registrada para dormir e para voltar ao comportamento normal ao desligar a opção.

Não afeta retorno do End, hardcore, mortes falsas dos sonhos, ou mecanismos de confinamento/respawn próprios de Nexus e colônias. Não concede proteção climática: terra firme pode continuar perigosa. Se o mundo não oferece terra segura dentro dos limites de busca, usa respawn normal e avisa privadamente; não deixa a tela de morte presa para sempre.

Comandos de administrador, permissão 2:

```text
/wayworld status
/wayworld respawn random
/wayworld respawn bed
/wayworld deaths witnessed
/wayworld deaths global
```

## Validação

- `python tests/geography/run_math.py`: periodicidade dos dois eixos, excesso fracionário, distância curta na costura, costa finita/polar, limites/continuidade/variedade de alturas.
- `bash gradlew runGeographyGameTestServer`: migração/persistência e defaults, folhas de ruído periódicas, testemunha olhando vs parede/costas, identidade/passagem de veículo com passageiro.
- `bash gradlew runGeographyValidationServer`: execução opt-in normal dedicada; campos climáticos seeded e chunks reais de oceano, planície e maciço gerados assincronamente, nível do mar e solo seguro. Marcador obrigatório `GEOGRAPHY RUNTIME PASSED`.

Arenas de GameTest vanilla distantes usam geração antiga para isolamento; testes do novo relevo usam servidor normal dedicado. Teste manual de navegação multiplayer, shaders, render distance extremo e máquinas/estruturas construídas diretamente na costura permanece necessário. Não há declaração de compatibilidade total com outros mods que substituem worldgen.

Referências: https://www.stardustlabs.net/terralith e https://modrinth.com/datapack/lithosphere .
