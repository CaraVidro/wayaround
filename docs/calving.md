# Falésias e calving

A costa alterna trechos suaves com muralhas quase verticais. Algumas têm uma
saliência de gelo com cerca de 9–13 blocos de espessura. No interior, platôs
isolados de 24–43 blocos de altura aparecem em aproximadamente uma de cada
12 células de 768 × 768 blocos; ocupam uma pequena parte de cada célula.
Essas mudanças de terreno aparecem em chunks novos.

O servidor procura paredes expostas em até 48 blocos dos jogadores, uma vez
por minuto, sem carregar chunks para essa busca. A fragilidade é salva por
chunk: tempo aumenta o desgaste, construções sobre o gelo acrescentam carga
e nevascas aceleram o processo. Saliências também desgastam mais rápido.
Pequenos pedaços caem antes da ruptura, seguida de um aviso de 4,5 segundos.
O desgaste natural só avança com jogadores próximos em chunks carregados.

Na ruptura, uma fatia de gelo e construções apoiadas nela se desprendem.
A queda usa o solo real sob a área de destino, tanto em terra quanto no mar.
O gelo é recolocado como destroços; blocos estruturais de construções viram
drops, peças frágeis quebram e contêineres são deslocados com seu conteúdo.
A nuvem de partículas acompanha a queda. Ao fechar o mundo normalmente,
quedas já iniciadas são concluídas antes do salvamento final.

Para conferir no jogo, com permissão de comandos:

- `/calving locate 5000`: estima a posição de uma falésia, sem gerar chunks.
- `/calving debug`: verifica gelo disponível e fragilidade local.
- `/calving here`: inicia a ruptura de uma parede adequada próxima.

Para conferir desgaste natural, construa sobre uma saliência, permaneça
próximo e acompanhe a fragilidade durante uma nevasca. O comando `here`
antecipa a ruptura e não valida o tempo de desgaste.

`gradlew checkCalving` testa frequência dos campos de terreno, paredes,
saliências e cálculo de pouso. A aparência das partículas, a captura de
construções e a geração interpolada do Minecraft precisam de conferência no jogo.
