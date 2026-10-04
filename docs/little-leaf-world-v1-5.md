# Little Leaf World — implementação v1.5

Implementado sobre `feature/nexus-fractured-complex-v1-5` (Minecraft 1.21.1 / NeoForge).

## Jogabilidade

A aba **Little Leaf World** reúne o núcleo de colônia, fungo cultivado, fragmento de folha, mel de formiga, quatro ovos e poções de inversão. Formigueiros naturais aparecem em florestas, selvas, planícies e savanas; savanas favorecem os cupinzeiros, mais altos. A estrutura inicial é de terra, com núcleo na superfície, abertura pequena e uma câmara de fungo três blocos abaixo que pode ser escavada.

Formigas pretas, vermelhas, de mel e cupins usam trabalhadores, soldados e rainha. Trabalhadores compartilham uma rota descoberta e um tronco de subida; o alvo de navegação fica acima das folhas sólidas, e o corte acontece ao alcançar qualquer face, procuram folhas baixas com uma busca incremental pelas colunas vizinhas (cupins também madeira), cortam fragmentos visuais e levam uma carga visível. O corte não destrói repetidamente blocos de árvores nem gera itens soltos por viagem. Operárias pequenas entram na câmara por um período curto e saem novamente; rainhas permanecem no abrigo. A família de mel ocasionalmente produz mel comestível; cupins têm abdome, textura e ninho distintos.

Use o fungo numa **brewing stand vanilla**, com poção estranha, para preparar inversão. Pólvora converte o frasco em arremessável pelo preparo vanilla. A inversão é instantânea e persistente: humanos ficam minúsculos; insetos pequenos ficam grandes. Uma segunda aplicação desfaz a transformação individual. A escala usa os atributos vanilla e altera modelo, colisão e câmera, preservando o valor original.

Uma formiga ampliada individualmente não entra no ninho pequeno, nem entrega diretamente ao fungo. Ela deixa fragmentos maiores ao lado, com um limite local de itens no chão. Lançar a poção arremessável no núcleo, no monte ou nas muralhas inverte a própria colônia: **os próximos nascimentos** adotam a escala grande. Não transforma retroativamente todos os habitantes. Uma nova dose pode reverter os próximos nascimentos. A colônia continua com seu histórico de trabalho.

| Nível | Trabalho acumulado | Exterior |
|---|---:|---|
| Básico | 0 | Pequeno monte de terra |
| Maior | 24 | Monte e população maiores |
| Muralha | 96 | Parede externa e passagens |
| Gigante | 256 | Abrigo ampliado |
| Fortaleza | 640 | Muralha com ameias e torres |

Colônias sem inversão ficam limitadas ao nível maior. Cargas grandes contam oito unidades; pequenas, uma. A atividade agregada num habitat favorável soma uma unidade a cada 1.200 ticks, conservando frações entre amostras e recuperando até trinta dias por carregamento. `/timetick` também avança esse relógio, respeitando limites. Não é necessário manter entidades ou chunks distantes rodando. Estruturas novas recebem uma história inicial variada.

Minúsculo, clique com a mão vazia no núcleo: o jogador entra numa **dimensão interna em escala de jogador**. Cada colônia ganha uma célula isolada e um retorno persistente. Há um fungo monumental, raízes, câmara da rainha e galerias menores conectadas. Os insetos internos são grandes, com população limitada; são neutros até uma agressão ou aproximação da câmara da rainha. A colônia salva a memória dos últimos dezesseis intrusos. A morte da rainha interrompe nascimentos e crescimento agregado. Insetos abandonam uma perseguição distante e voltam ao trabalho, mantendo a memória do intruso.

Atravesse o buraco no túnel lateral do vestíbulo para retornar minúsculo. A saída é aberta, sem bloco ou clique obrigatório. Interiores antigos recebem esse túnel na entrada, no login ou no respawn; depois disso as alterações do jogador são preservadas. Morrer dentro mantém o vínculo e volta ao vestíbulo; logout/reinício não perde as coordenadas. Destruir o núcleo exterior não apaga a saída. Um retorno vedado recebe uma pequena saída de emergência acima do antigo núcleo. O interior é gerado uma vez, e revisitas preservam alterações.

Um núcleo craftável usa oito terras e um fungo. Um ovo usado no núcleo escolhe a família da colônia; ovos individuais fora da colônia são insetos sem vínculo. Nove fragmentos podem virar musgo. O registro permanece estável, e o seletor de sistemas tem **Little Leaf World**.

## Limites e validação

A população visível é uma amostra, não milhares de insetos simulados. Limites: 6–14 indivíduos externos por núcleo, 12 internos, 64 insetos no bairro de nascimento, duas tentativas de nascimento, 128 sondagens de alimento e 64 operações de construção por tick/dimensão. Inspeções usam chunks já presentes. A geração escreve somente o chunk/célula correspondente e não executa buscas remotas. A construção expande aos poucos e respeita blocos existentes, água e fronteiras não carregadas. Paredes colocadas pela própria colônia são registradas e reconstruídas na mudança de nível; blocos diferentes construídos pelo jogador são preservados.

O modelo possui três segmentos, seis patas articuladas, antenas, mandíbulas e cargas. Distância reduz o detalhe das formigas minúsculas; acima de quarenta blocos elas não são desenhadas.

Verificações automatizadas cobrem regras de crescimento e galerias, registro real da dimensão, fungo/saída, isolamento e persistência das rotas, relógio fracionário/carregamento tardio, níveis, memória, castas/cargas e escala/colisão/reversão. Sete GameTests adicionais exercitam colheita sem duplicação, corte e entrega de cargas, poção no núcleo, entrada/saída real de uma operária e hostilidade/morte da rainha, coleta completa por IA e escalada de uma copa elevada. Build e inicialização cliente/servidor são executados em CI. A aparência em jogo, rotas sobre árvores e viagens em multiplayer ainda exigem conferência manual; não há afirmação de FPS medido.

Novos formigueiros naturais aparecem em chunks gerados depois desta versão; chunks existentes não são reescritos.


### Species entrances and encounters

Black, red and honey ants and termites have separate craftable colony blocks with distinct surfaces and a visible entrance. Natural mounds and new interior colonies use the species block; the old `colony_core` remains registered for saved worlds and as the crafting ingredient. Each specific core keeps its species when placed and drops its own item.

Exterior births begin above the core. When that location is obstructed, a bounded local search requires a solid supporting face, dry feet and a collision-free body; failed births wait instead of materializing in midair. Workers returning from the fungus chamber use the same supported entrance. Giant inhabitants use a wider local search.

A worker stalled for a second attempts a lateral route. After a sustained stall it searches only its nearby loaded terrain for a supported free position and abandons an unreachable food route without losing a carried fragment.

Workers and defenders periodically detect visible rival species nearby (4 blocks for tiny insects, 16 for giants) and fight with actual melee damage. Queens retaliate when attacked; same-species neighbors remain friendly. Encounters are local, with capped candidate checks and no remote chunk loads.

Four additional GameTests cover species defaults/shared block-entity compatibility, supported births, actual rival damage, friendly same-species neighbors and obstacle-to-delivery behavior (birth/registration share one test). The nature suite now has 40 required tests.
