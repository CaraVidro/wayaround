# Frio e exposição na Antártica

O servidor calcula a exposição uma vez por segundo de jogo, para jogadores vivos em sobrevivência/aventura. Criativo e espectador são isentos. A região é o continente definido por `AntarcticField`, ou o bioma `antarctic_ice_sheet`, somente no Overworld.

É necessário estar ao ar livre, com a cabeça em uma coluna sem teto. Cavernas, subsolo e tetos (inclusive vidro) interrompem o acúmulo. Dormir também interrompe a exposição. O frio salvo no jogador diminui um ponto por segundo protegido, sem ser apagado ao sair e entrar no servidor.

O frio varia de 0 a 120. A partir de 10 aparecem tremores; em 30, lentidão I; em 60, frostbite; em 90, lentidão II. Frostbite causa meio coração de dano de congelamento a cada dois segundos. Não utiliza os ticks de neve fofa, evitando duplicar o dano desse mecanismo. O dano e a lentidão param poucos segundos depois de entrar em abrigo (até 3 e 2,25 segundos, respectivamente).

Tempos aproximados partindo aquecido, sob condições constantes:

| Condição | Lentidão | Frostbite |
| --- | --- | --- |
| Dia sem nevasca | 250 s | 500 s |
| Noite sem nevasca | 50 s | 100 s |
| Dia com nevasca máxima | 20 s | 40 s |
| Noite com nevasca máxima | 15 s | 30 s |

A noite segue a mesma transição gradual usada na iluminação polar. Os tremores aumentam em quatro estágios visuais e diminuem gradualmente em abrigo, podendo persistir por até três minutos após exposição intensa. Afetam a câmera e o modelo do personagem, inclusive para outros jogadores, sem mudar a rotação real ou enviar alterações de mira ao servidor. Os efeitos usam a sincronização vanilla; os ícones reutilizam os recursos vanilla de lentidão e fraqueza.

Validação manual: em sobrevivência, compare `/time set noon` e `/time set midnight`, depois uma nevasca forte. Entre em uma caverna ou sob um teto de vidro: o frio deve parar de acumular, o dano deve cessar em até três segundos e os tremores devem diminuir. Repita saindo e retornando ao mundo, e observe outro jogador tremendo em terceira pessoa. Não precisa gerar novos chunks.

`checkCold` testa os tempos, progressão, limites, ciclo do dia, imunidade de abrigos e recuperação dos tremores. A inspeção visual precisa ser feita no jogo.
