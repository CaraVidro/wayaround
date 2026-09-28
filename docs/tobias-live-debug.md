# Tobias: reconhecimento ao vivo

O fluxo contínuo de Void/Tukuna recebe PCM de 40 ms enquanto o jogador fala.
Ativar o debug de voz também habilita esse fluxo para diagnóstico sem Spectrum.

## Alterações

- Prepara um decodificador Vosk instalado antes de começar a falar; não baixa modelos.
- Envia o pre-roll ao detectar voz, sem esperar pelo próximo frame.
- Evita snapshots especulativos redundantes enquanto o decodificador contínuo está ativo.
- Agrupa áudio pendente em uma fila limitada a 2 segundos. As tarefas de abrir/fechar o
  decodificador não são mais descartadas quando há sobrecarga. Uma perda de áudio por
  sobrecarga gera aviso no log.
- Mostra transcrições parciais em uma linha do HUD com o debug ativado.
- Impede que uma confirmação de domínio seja descartada pelo intervalo genérico de
  Blue/Red. O servidor continua validando acesso e estado.
- Abre a introdução uma vez por fala; aceita confirmação por até 4 segundos após a
  abertura de 22 ticks, sem repetir toda a animação.

## Teste no cliente

1. Ative Voice Chat e o debug de voz nas configurações. Use um modelo PT-BR já instalado.
2. Com o Spectrum disponível, abra Shift+T.
3. Diga lentamente `expansão... de domínio`, continuando a falar depois da frase.
   A linha `Tobias AO VIVO` deve mudar antes de soltar o PTT ou ficar em silêncio.
4. Repita com `domínio de expansão`. Uma palavra isolada deve apenas preparar.
5. Diga `não quero expansão de domínio`: não deve confirmar a habilidade.
6. Teste também ativação por voz, duas falas consecutivas e desconexão durante a fala.

`fila` mede a espera do áudio no worker. `decode` mede a chamada ao Vosk.
Não incluem toda a latência do microfone, a estabilização acústica das palavras,
a rede ou a duração da animação. A transcrição final tradicional continua disponível
para contexto, tratos, testemunhos e diagnóstico. Este patch não foi medido com
microfone real neste ambiente; o teste de percepção precisa ser feito no cliente.
