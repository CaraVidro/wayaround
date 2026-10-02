# WAYPERF — profiling do Way Around

O `/wayperf` mede somente caminhos instrumentados do Way Around. Ele fica desligado por padrão; quando desligado, cada ponto instrumentado faz apenas a checagem barata de que o profiler não está ativo.

## Comandos

- `/wayperf start` — captura 10 segundos.
- `/wayperf start <2..300>` — captura uma janela definida.
- `/wayperf status` — mostra se a captura está ativa.
- `/wayperf stop` — encerra antes do tempo e mostra o relatório.
- `/wayperf report` — mostra a captura ativa até agora ou a última captura finalizada.
- `/wayperf dump` — grava CSV em `logs/wayaround-profile-YYYYMMDD-HHMMSS.csv`.

O relatório ordena pelo tempo total observado e mostra chamadas, média, máximo e chamadas por segundo. Os tempos são **inclusivos**: por exemplo, uma máquina pode chamar a rede mecânica ou o sistema de água, então não se deve somar todas as linhas como se fossem tempo exclusivo.

## Subsistemas instrumentados

Ecologia/sucessão, fauna, interações marinhas, regras de spawn de peixes, gerenciamento de deep ocean, fluxo mecânico da água, pipework, grafo de transmissão mecânica, simulação da water wheel, simulação das máquinas industriais, iluminação real do submarino, blizzards, voice relay, clima local, nuvens, overlay de água e render procedural das máquinas.

## Como comparar otimizações

Faça a comparação no mesmo mundo, posição, render distance, quantidade aproximada de entidades e cenário. Antes da primeira captura deixe o jogo rodar por cerca de 30 segundos para o JVM/JIT aquecer.

Cenários úteis:

1. **idle/base:** parado numa área simples, sem máquinas grandes.
2. **ocean:** deep ocean com muitos peixes e submarino/luzes.
3. **machine hall:** crusher, mill, pump, washer, water wheel, eixos e gears operando.
4. **weather:** nuvens visíveis e, separadamente, uma blizzard forte.
5. **ecology:** área com bastante fauna/vegetação carregada.

Use janelas de 15–30 segundos. Rode cada cenário pelo menos 3 vezes e compare a mediana, principalmente `total_ms`, `avg_us`, `max_ms`, GC e heap delta.

Não use o profiler para medir FPS bruto de terceiros: ele mede os caminhos instrumentados do Way Around. Para FPS/frametime global, combine o CSV com Spark, VisualVM, JFR ou o profiler do próprio Minecraft.
