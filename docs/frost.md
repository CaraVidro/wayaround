# Neve nas construções

Nevascas embranquecem gradualmente as faces expostas dos blocos. A cobertura
tem quatro intensidades e não substitui o bloco: portas, escadas e inventários
continuam funcionando. As faces precisam ter acesso ao exterior: a neve pode
passar sob um beiral curto seguindo uma trajetória inclinada, mas não atravessa
paredes, vidro nem abrigos profundos. Faces inferiores ficam protegidas.

Materiais naturais, vegetação, neve, gelo e blocos já brancos não recebem a
cobertura. Tábuas, tijolos, concretos coloridos e outros materiais de construção
podem receber. As exceções podem ser ajustadas pela tag
`wayaround:frost_immune`. O reconhecimento de material natural é por tipo de
bloco, não por quem o colocou.

Com nevasca local acima de 15%, a varredura cobre a área de 49 × 49 colunas ao
redor de um jogador parado em até 8 segundos de jogo. Cada passagem acrescenta
uma intensidade; a cobertura máxima leva aproximadamente 32 segundos. Neve
leve normal não embranquece construções sozinha.

- Clique com papel para limpar a face atingida: consome uma unidade no survival.
- Use um balde de água para lavar o bloco e colocar a água normalmente.
- Água corrente lava as faces que toca; blocos encharcados também perdem a cobertura.

A cobertura é salva no mundo e sincronizada com quem recebe o chunk. A água é
verificada periodicamente nos chunks próximos dos jogadores. Nevascas também
depositam cobertura nas colunas processadas pelo acumulador de tempestades.

Na Antártida há precipitação visual leve de neve, intensificada pela nevasca.
Ela usa o renderizador de precipitação do Minecraft. O acúmulo no solo acontece
em chunks carregados próximos dos jogadores e não substitui blocos existentes.
A priorita continua protegida contra esse acúmulo.

Conferência no jogo: monte uma parede de tábuas/tijolos e outra de concreto
branco, com um trecho coberto por um telhado. Durante uma nevasca, confira que
as faces externas da primeira embranquecem, as protegidas permanecem limpas e
o concreto branco é ignorado. Limpe faces com papel, lave com água corrente e
reabra o mundo para conferir a persistência. Verifique também um segundo jogador
e uma construção com escadas, lajes e baús.

Para diagnosticar, olhe para um bloco e execute `/frost debug`: informa material,
bioma, intensidade local da nevasca, faces expostas e cobertura salva. `/frost test`
aplica cobertura máxima às faces expostas de um bloco permitido, sem esperar pela
tempestade; serve para separar falhas do acúmulo de falhas visuais. Ambos exigem
permissão de comandos. `gradlew checkFrost` verifica varredura, abrigo e limpeza.
