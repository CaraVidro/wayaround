# Then Days Break

Evento global persistente, ativado exclusivamente por operador:

- `/then_days_break start`
- `/then_days_break stop`
- `/then_days_break status`

A ativação fica salva no Overworld e é sincronizada na entrada, troca de dimensão, respawn e a cada cinco segundos. Não há sorteio nem ativação automática. Dimensões sem céu ou com horário fixo não recebem efeitos solares.

De dia, o céu fica vermelho como no Nexus, com sol circular vermelho e borda vermelha escura. O raio dobra a cada 1.200 ticks, até 12 vezes o tamanho inicial; o limite mantém a geometria finita. A malha tem 64 setores, independente do tamanho. A música Ogg enviada pelo usuário é reproduzida em streaming, em loop, na categoria Música. Para à noite, ao desativar e ao desconectar.

Entidades expostas diretamente ao sol recebem dano progressivo; seres vivos também recebem lentidão crescente, até nível V. O raio solar considera o ângulo do sol, coberturas opacas e chunks já carregados. Vidro deixa a luz passar. Noite e abrigo param o dano e reduzem a exposição acumulada; a lentidão residual expira em três segundos. As imunidades normais de entidades e jogadores permanecem em vigor.

A fila tem no máximo 4.096 entidades e processa 32 por tick, com até 128 consultas por raio solar. Em cenários com mais de 640 entidades ativas, a cadência de dano pode ficar mais lenta. A inspeção não carrega chunks distantes.

À noite, a lua conserva as fases, com cor avermelhada. O céu fica totalmente preto, sem estrelas, nuvens ou luz ambiente lunar. Luz real de blocos e lanternas continua útil. Sol, lua e nuvens habituais retornam ao desligar o evento. Nuvens procedurais são ocultadas durante o evento para não encobrir os astros.

Validação: contratos puros do calendário, crescimento e exposição; GameTests de persistência, comandos, dano, lentidão, abrigo e segurança noturna; compilação, servidor dedicado e inicialização do cliente no CI. Aparência, áudio e interação com shaders externos exigem teste dentro do mundo.
