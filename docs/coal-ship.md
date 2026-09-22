# Navio a carvão

`wayaround:coal_ship` é um barco com baú de 27 espaços e um pequeno motor metálico,
chaminé e hélice animada. Usa o casco/modelo vanilla de pinheiro e texturas vanilla.

- Coloque na água como um barco; também funciona em um ejetor voltado para a água.
- Clique para pilotar. Shift + clique abre o baú, ou aperte a tecla de inventário enquanto pilota.
- Guarde carvão ou carvão vegetal no baú. Shift + clique segurando carvão coloca uma unidade no baú.
- O motor consome automaticamente o carvão do baú ao acelerar para frente na água.
  Cada unidade dá 80 segundos de aceleração; parado, em terra ou remando para trás não consome combustível.
- Velocidade em linha reta aproximada: 16 blocos/segundo com motor, contra 8 a remo.
  Sem combustível continua funcionando como um barco comum.
- O inventário e o restante da unidade de combustível acesa persistem ao salvar/recarregar o mundo.
  Ao quebrar, o casco e a carga caem como itens conforme as regras vanilla; o combustível já aceso se perde.
  O casco mantém as regras vanilla de queda/dano: quedas altas podem despedaçar o casco em madeira e gravetos.

Receita: ferro/pistão/ferro, barco/fornalha/baú, ferro/ferro/ferro.

Verificação no jogo: abastecer, medir velocidade, largar a tecla para pausar consumo,
sair/reabrir o mundo, descarregar carvão com a unidade ainda acesa, quebrar em survival e
conferir se a carga cai uma única vez. Testar também um ejetor e um segundo cliente como observador.
