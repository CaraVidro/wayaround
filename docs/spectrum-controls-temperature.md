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
