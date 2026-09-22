# Vento nas particulas da Antartica

Durante a nevasca, as particulas soltas recebem deslocamento horizontal com rajadas,
incluindo fumaca, chamas, detritos, nuvens e snowflakes. O gancho fica no motor de
particulas depois do tick, portanto alcanca tambem as classes que substituem o tick
original. O deslocamento converge para uma velocidade limitada, preservando a
animacao vertical e evitando aceleracao acumulada sem limite.

A precipitacao nativa (`rain`/neve desenhada pelo LevelRenderer) usa a mesma direcao
e rajada: o topo das colunas inclina contra o vento, e a base permanece na superficie.
O antigo impulso horizontal de 4.5 blocos/tick dos snowflakes foi retirado para nao
somar dois ventos. As nuvens mantem somente sua pequena turbulencia aleatoria original.

A posicao de cada particula precisa estar ao ar livre, acima do WORLD_SURFACE, em
regiao antartica (ou seu bioma) e fora de fluidos. Telhados, inclusive vidro, e cavernas bloqueiam
o vento. A colisao do deslocamento impede atravessar paredes. A verificacao de
exposicao e compartilhada por bloco durante cada tick para reduzir consultas.

A intensidade visual ainda vem do pacote de nevasca recebido pelo jogador: e uma
aproximacao local perto da borda da tempestade, nao uma simulacao meteorologica de
cada particula distante. Entrar num abrigo nao interrompe o vento das particulas que
continuam la fora. Ao sair do mundo, o estado visual e descartado.

A bolha de priorita em crescimento balanca suavemente, presa ao liquido para coincidir
com o ponto de explosao e dano no servidor; a fumaca solta quando estoura deriva livremente.

Verificacao visual: em uma nevasca ativa, observe fumaca e neve do lado de fora e
repita sob um telhado de vidro, em uma caverna e em outro bioma. A neve nativa deve
ficar inclinada na mesma direcao que os snowflakes; durante tempo calmo retorna ao
comportamento original. O teste `BlizzardWindTest` verifica limites, convergencia,
ausencia de vento em tempo calmo e inclinacao da precipitacao.
