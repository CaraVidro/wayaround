# Sistema de sonhos — protótipo V2

NeoForge 1.21.1 / Java 21. Início exclusivamente administrativo; dormir normalmente apenas contribui para o perfil.

## Testar

1. Reinicie o mundo para carregar `wayaround:dream`.
2. Use uma cama e interaja com baús/barris e bancadas na base. O perfil é amostrado a cada segundo; o detector de base combina uso, permanência, retornos e construções.
3. Execute `/wayaround dream info <jogador>` para consultar a base, os hábitos, áreas semânticas, pontos de permanência e alvo do DreamPlayer.
4. Execute `/wayaround dream start <jogador>`. A tela escurece durante a cópia e revela uma reprodução da base ao lado da cama. Nenhuma mensagem de entrada é enviada ao alvo.
5. O DreamPlayer, com aparência de Steve, procura o container mais utilizado que tenha sido copiado. Golpes não retiram vida: interrompem a tarefa, causam impulso, provocam um olhar de 0,7–3 segundos e ele volta ao mesmo container.
6. Use `/wayaround dream die <jogador>` para testar a morte simulada. Os botões do menu falso não executam ações; após a transição, o estado real é restaurado.
7. `/wayaround dream stop <jogador>` força a restauração. `/wayaround dream testplayer <jogador>` recria o ator de uma sessão já preparada.

Todos os comandos exigem nível de permissão 2. Os comandos de início/ator não ignoram os limites da sessão. A falta de cama ou de espaço seguro aborta a preparação e restaura o jogador; detalhes aparecem no log do servidor.

## Perfil e semântica

Dados agregados por UUID em `wayaround_behavior.dat`: até 256 chunks, 48 locais e 64 células de permanência de 8 blocos por jogador. Não há histórico individual de movimentos, texto de placas no perfil, chat, credenciais ou dados de rede.

`BaseSemanticMap` identifica áreas de cama, armazenamento (pelo menos três containers próximos), oficina (pelo menos duas estações), atividade e preferência subterrânea. Usa dados de interação, sem procurar paredes ou varrer construções continuamente. Dados escassos produzem informações parciais.

A região com confiança abaixo de 0,25 não é usada como base: o início tenta a cama de renascimento e depois a posição atual. Ainda é necessário encontrar uma cama dentro da área copiada.

O NPC recebe uma chance de pulo limitada a 1%–70% por tentativa e um modificador de movimento entre 0,85 e 1,35. Ele não copia identidade/skin, não fala e não combate.

## Diretor

Instabilidade começa em 0,02, sobe 0,002 a cada dez segundos de exploração e 0,025 por golpe no ator, limitada a 0,30. Tentativas de abrir portas e remover tochas alternam com intervalos aleatórios de aproximadamente 10–35 segundos.

Alvos são coletados durante a cópia, com limite de 512 portas e 512 tochas. A cada tentativa são considerados até 32 candidatos, preferindo proximidade das áreas semânticas. Mudanças exigem distância de 6–32 blocos, direção fora da frente do jogador e obstrução de múltiplos raios visuais. Visibilidade direta veta a mudança mesmo quando o alvo está atrás. As portas verificam também sua metade superior. Tentativas sem candidato elegível são descartadas, sem forçar um evento.

Só portas e tochas estão implementadas. Outros valores do enum são reservados para expansão.

## Cópia, isolamento e recuperação

Cada sessão copia 5×5 chunks, em toda a altura da dimensão de origem, durante ticks normais. O trabalho é dividido em lotes e limitado por tempo; não ocorre em worldgen. Os chunks da cópia permanecem ativos até a limpeza. A dimensão fica limitada a **uma sessão simultânea**, inclusive durante a limpeza, por compartilhar horário e céu.

São copiados BlockStates e dados de baús, barris, fornalhas, placas e máquinas conhecidas do Way Around. Ações clicáveis de placas são removidas. Outros BlockEntities recebem apenas seu bloco/estado padrão; entidades normais não são clonadas. Biomas e céu individual não são reproduzidos nesta etapa.

Antes de modificar o jogador, grava-se um backup NBT síncrono e atômico em `<mundo>/wayaround-dream-backups/<uuid>.dat`. Inventário, armadura, mão secundária, Ender Chest, vida, alimentação, experiência, efeitos, habilidades, modo de jogo e dados persistentes do jogador são restaurados. O backup só é removido após confirmar o salvamento real do jogador. Desconexão e encerramento tentam restaurar imediatamente; login recupera uma sessão interrompida a partir do arquivo. Não apague esses arquivos manualmente durante um sonho.

A restauração usa um espaço seguro próximo da cama real; quando isso não é possível, usa a dimensão e posição originais. O efeito de susto move apenas a câmera, sem lançar o corpo contra o teto. A cópia e suas entidades são apagadas em lotes. Após uma interrupção abrupta, a área temporária é sobrescrita antes de ser reutilizada.

Portais e transferências de dimensão são bloqueados durante o sonho, assim como comandos comuns que poderiam exportar itens/alterações para o mundo real. Os comandos administrativos do próprio sistema continuam disponíveis. O limite da cópia e quedas extremas acionam a morte simulada. O sistema não intercepta interfaces vanilla permanentemente.

Configuração: arquivo de servidor `wayaround-server.toml`, com duração do menu falso (40–100 ticks), orçamento de cópia e limite fixo de uma sessão.

## Verificação

`gradlew.bat build --offline`

`gradlew.bat runGameTestServer --offline`

Os GameTests incluem perfil limitado e persistência, classificação de áreas, hábitos limitados, seleção do baú mais usado, bloqueio visual do diretor, cópia de inventário de baú, ausência de dano, ciclo do ator, morte simulada, restauração de inventários/XP/vida, preservação do baú original e recuperação a partir de backup em disco. O pacote `dream_tests` é ativado apenas no processo GameTest para incluir a dimensão no preset de testes; não substitui presets em mundos normais.
