# Spectrums: inputs, calor e Fuga (1.0.5)

## Menu

- Shift+T: abrir/fechar. Shift+Y: próximo Spectrum desbloqueado. Shift+U: próxima página.
- As três combinações são remapeáveis em Opções → Controles → Way Around - Spectrums.
- Enquanto o menu estiver aberto, 1–9 são habilidades, não seleção da hotbar.
- Abrir uma tela/fechar o menu cancela um gesto pendente.
- Tukuna: 1 toque = um corte; segurar 1 e soltar = 2–8 cortes; 1 → 2 dentro de 6 ticks, ou 2 enquanto segura 1, adiciona fogo.
- 2 sem combo prepara Fuga; depois de pronta, 2 lança. A palavra personalizada continua preparando; dizer/digitar `fuga` também prepara/lança.
- 3 = prévia de domínio: selo, sala preta, retorno automático. Sem ataque de domínio.
- Void tem duas páginas com Blue, Red, Purple, Infinity, domínio e controles das técnicas. Justiça tem o tribunal existente.

## Entradas compartilhadas

Voz e chat preservam os interpretadores existentes e chamam as mesmas funções que o menu.
Precisão (palavras, duração, números): preferir chat. RP: voz opcional.

Comandos adicionais: `preparar desmartelar`, `combinar fogo`, `soltar desmartelar`, `cancelar tecnica`, `dominio tukuna`.
Exatos do Void: `tecnica imaginaria azul`, `tecnica imaginaria vermelho`, `azul orbitar`, `azul parar`, `azul lancar`, `azul encerrar`, `azul potencia maxima`, `vermelho lancar`, `vermelho energia maxima`, `ativar infinito`, `infinito reforcar`, `infinito encerrar`, `preparar azul e vermelho`, `lancar roxo`, `dominio void`.

## Temperatura regional

Temperaturas são unidades de gameplay. Células esparsas de 8 blocos, separadas por dimensão, resfriam exponencialmente. Não carregam chunks para reagir.

| Limiar | Reação |
| --- | --- |
| 280 | Madeira/folhas podem incendiar |
| 650 | Entidades podem pegar fogo |
| 1100 | Ferro recebe revestimento visual amarelo luminoso |
| 1900 | Pedra e metais compatíveis podem virar lava |

As chances crescem exponencialmente e têm limites. Blocos indestrutíveis e inventários não entram na fusão térmica. `RegionalTemperature.register` permite adicionar reações; `pulse` aquece; `at` consulta. O calor é transitório, não é salvo após reiniciar o servidor.

Fuga aquece o centro ao máximo de 3200, com queda espacial; corte de fogo aplica pulsos de 2600. A cratera avança em cascas elipsoidais, com orçamento global de 2400 verificações e 360 remoções por tick, compartilhado por até 16 explosões. A explosão vanilla instantânea foi removida.

## Conferência no runClient

1. `/tukuna_test possession`: 19 dedos para testar Fuga.
2. Abra Shift+T e escolha Tukuna com Shift+Y; teste 1, segurar 1 e 1→2.
3. Teste 2: mãos sobem, palmas, flecha se forma; após 6 segundos, 2 ou `fuga` lança.
4. Acompanhe a cratera expandindo e o pilar deformando.
5. Teste 3: retorno ao mesmo local após a prévia preta; também reconecte durante a prévia para verificar recuperação.
6. Com permissões de operador, `/spectrum_heat 3200 8` aquece ao redor e `/spectrum_heat` consulta. Coloque madeira e ferro próximos antes do teste.
7. Teste também no Void com relíquia desbloqueada e compare tecla, voz e chat; feche o menu para retomar a hotbar.

Build e regressões matemáticas não substituem verificar poses, cores e equilíbrio no jogo.


## Tukuna: progressão de receptáculos (1.0.6)

- Um mesmo Tukuna pode manter vários receptáculos. Fora de uma possessão, o espírito fica ancorado à visão em primeira pessoa de um receptáculo disponível e F5 é bloqueado.
- Durante a troca, o receptáculo perde foco, a imagem escurece e símbolos/faixas de Tukuna aparecem **pixel por pixel** sobre o modelo, sem depender da skin. No retorno eles desaparecem do mesmo jeito.
- Depois que Tukuna assume, os papéis invertem: ele controla o corpo e pode usar F5; o receptáculo apenas observa. Chat e microfone do receptáculo continuam bloqueados no servidor.
- O receptáculo conserva os dedos incorporados ao morrer. Quando Tukuna já recuperou um corpo próprio e morre, o conjunto de dedos volta ao mundo.
- Tukuna pode comer os próprios dedos enquanto controla um corpo; essa força fica salva no espírito.
- Com um dedo próprio na mão, segurar botão direito mirando outro jogador inicia alimentação forçada. Perder a mira, sair do alcance ou soltar antes do fim cancela.
- O chat não revela contagem ou “quantos faltam”. Cada dedo tem uma mensagem narrativa própria. Marcos: 5 = visão ruim/aura sem posse; 10 = dano/quase-apagão/batida; 15 = posse obrigatória por 5 minutos; 20 = a palavra **corpo** passa a separar Tukuna do receptáculo.
- Depois de **corpo**, o antigo receptáculo fica com os buffs e imune a novas possessões. O estado também fica persistido como gancho para um Spectrum exclusivo futuro.


## Void polish: UI e Purple Nuke

- O menu Spectrum agora é mais compacto e usa 10 slots por página: **1–9 e 0**. Só depois disso abre a próxima página.
- RED preparado não aplica mais o overlay branco opaco; fica apenas uma tonalidade vermelha discreta.
- Se BLUE estiver ativo, inclusive seguindo o mouse, preparar RED captura o BLUE imediatamente, congela seu centro e inicia a fusão.
- A fusão física BLUE + RED voltou a ser **Purple Nuke**: ela carrega no ponto capturado e detona automaticamente. Não vira mais Purple projectile.
- Pressionar a ação Purple quando a nuke já passou da fase madura (72 ticks) antecipa a detonação, mas continua sendo a mesma nuke.
