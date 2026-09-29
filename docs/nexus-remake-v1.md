# Nexus Remake V1 — estado, direção e contrato arquitetural

Data de referência: 2026-09-29  
Base: `feature/agua-world-polish` @ `6614744`  
Minecraft: 1.21.1  
NeoForge: 21.1.250  
Way Around: 1.2.0

## 1. Regra principal

O remake NÃO deve apagar sistemas que já funcionam apenas para recriá-los.

Antes de alterar qualquer parte do Nexus:

1. localizar a implementação atual;
2. identificar o estado persistente já salvo;
3. reutilizar registries, networking, portal e renderer quando servirem;
4. separar responsabilidades muito grandes;
5. migrar mundos de teste quando a estrutura de dados mudar;
6. manter dedicated server e build CI funcionando.

A pergunta não é "como fazer um Nexus novo do zero?".  
É "como transformar o Nexus atual em um sistema que pertença ao Way Around inteiro?".

## 2. Onde estamos

O ramo vivo atual é `feature/agua-world-polish`.

Ele já contém, entre outros:

- World State persistente;
- Assembly e montagem física;
- tempo/envelhecimento;
- clima, Antártica, gelo, neve e vento;
- ecologia e fauna aquática;
- profundezas oceânicas;
- Kraken;
- mídia, câmera, TV, VHS e Black Box;
- barcos/navios experimentais;
- Spectrums, Jujutsu e voice intent;
- Seu Velho Amigo;
- menu de features por mundo;
- Warfare Outpost;
- The Nexus.

O Nexus atual NÃO é só conceito. Já existem:

- `NexusContent`;
- `NexustorBaseBlockEntity`;
- `NexustorStructure`;
- `NexustorRenderer`;
- `NexusEventManager`;
- `NexusPortalManager`;
- dimensão `wayaround:nexus`;
- bioma `wayaround:nexus_caverns`;
- efeitos de céu/neblina;
- portal sincronizado;
- sala segura de chegada;
- evento global;
- cinco ondas;
- integridade do reator;
- boss bar;
- sludge;
- advancements globais;
- retorno bloqueável ao desligar o Nexustor.

## 3. Problema do Nexus atual

Ele funciona, porém concentra responsabilidades demais.

### NexusEventManager

Hoje controla ao mesmo tempo:

- registro de reatores ativos;
- temporização;
- ondas;
- infected;
- dano do núcleo;
- progressão;
- boss bar;
- sludge;
- efeitos;
- conclusão/falha;
- sincronização de estado;
- manutenção do portal após conclusão.

Isso transforma uma feature em um "deus-classe".

### NexusPortalManager

Hoje resolve:

- estado do portal;
- entrada;
- saída;
- retorno;
- morte dentro do Nexus;
- cooldown;
- criação da câmara de chegada;
- sincronização das duas pontas.

A lógica está correta como protótipo, mas a dimensão e a infraestrutura crescerão melhor se geração e vínculo de portal forem responsabilidades separadas.

### Nexustor

A montagem já possui estágios persistentes, mas visualmente é quase toda virtual no BlockEntityRenderer.

Isso é diferente do Assembly central do Way Around, no qual montagem, erro, alinhamento, material e estado físico devem importar.

## 4. O objetivo do remake

O Nexus deve virar um dos primeiros exemplos de uma feature que atravessa vários motores do Way Around.

Fluxo desejado:

Assembly
→ Nexustor construído
→ estado persistente
→ ativação global
→ evento físico
→ mundo reage
→ portal nasce
→ dimensão responde
→ exploração
→ descoberta
→ consequências retornam ao mundo principal
→ tudo pode ser registrado por câmera / VHS / Black Box
→ o servidor ganha história.

## 5. Arquitetura proposta

### 5.1 NexusReactorState

Responsável somente pelo estado persistente do Nexustor:

- peças instaladas;
- integridade;
- estágio;
- energia;
- conexão;
- portal;
- vínculo dimensional;
- timestamps relevantes.

O BlockEntity continua sendo a âncora física.

### 5.2 NexusActivationController

Orquestra a ativação:

- inicia;
- avança fase;
- falha;
- conclui.

Não deve renderizar, gerar dimensão nem controlar UI diretamente.

Fases sugeridas:

1. IDLE
2. PRIMING
3. BREACH
4. DEFENSE
5. STABILIZING
6. LINKED
7. FAILED

### 5.3 NexusDefenseController

Extrair do EventManager:

- ondas;
- infected;
- pressão sobre o núcleo;
- dificuldade;
- resolução de kills.

O sistema pode continuar usando zombies inicialmente.

### 5.4 NexusPresentation

Cliente e networking:

- tremor;
- céu vermelho;
- pulsos;
- coluna;
- shockwaves;
- áudio;
- HUD/boss bar.

Apresentação não decide regra de jogo.

### 5.5 NexusLink

Representa a ligação entre:

- dimensão de origem;
- posição do Nexustor;
- dimensão Nexus;
- âncora de destino;
- estado ligado/desligado.

O retorno continua dependendo do Nexustor.

### 5.6 NexusArrivalGenerator

Extrair a escultura da câmara de chegada do PortalManager.

Responsável somente por:

- preparar local seguro;
- conectar a cavernas;
- colocar marcadores persistentes;
- nunca regenerar a mesma área destrutivamente.

## 6. Remake visual do Nexustor

Não transformar o Nexustor em um bloco mágico instantâneo.

Manter a ideia de máquina montada em etapas, mas aproximá-la do Assembly:

- Base;
- armação;
- corpo/câmara;
- quatro braços/fingers;
- cabeça;
- contenção;
- Nexustoetor/núcleo;
- painel.

Cada estágio deve ser fisicamente legível.

O renderer procedural pode continuar existindo. O objetivo não é trocar tudo por centenas de blocos.

Melhoria central: o renderer deve ser uma representação do estado da máquina, não o sistema inteiro da máquina.

### Visual

Identidade:

- ferro pesado;
- aço escuro;
- rebites;
- cabos;
- vidro grosso;
- latão/cobre em pequenas quantidades;
- energia impossível presa em tecnologia antiga.

Evitar estética de laboratório sci-fi moderno.

O Nexustoetor deve parecer algo que a máquina está tentando conter, não uma bateria bonita.

## 7. Remake da ativação

A ativação atual de cinco ondas pode sobreviver, mas deve virar uma sequência mais legível.

### PRIMING

- braços se conectam;
- vibração crescente;
- instrumentos começam a responder;
- pequenas falhas elétricas;
- Black Boxes próximas podem registrar o começo.

### BREACH

- pulso global;
- céu/atmosfera alterados;
- som distante para jogadores do servidor;
- World State registra o acontecimento.

### DEFENSE

- ondas continuam;
- o núcleo pode sofrer dano;
- incêndio, fumaça e estruturas próximas podem ser afetados futuramente;
- câmera/VHS registra normalmente.

### STABILIZING

- menos inimigos;
- foco visual no reator;
- espiral começa a formar;
- portal ainda não é seguro.

### LINKED

- portal utilizável;
- Nexustor se torna uma infraestrutura persistente;
- desligar realmente rompe o caminho de volta.

## 8. Remake da dimensão Nexus

A dimensão atual usa `minecraft:caves` com bioma fixo. Isso é suficiente como placeholder.

O remake deve manter a ideia já definida:

- complexo de cavernas enorme;
- teto alto, mas visível;
- quase preto;
- sensação de profundidade absurda;
- portal como única saída normal.

Primeira versão NÃO precisa encher o Nexus de conteúdo.

### V1 da dimensão

- cavernas largas;
- câmaras verticais;
- pilares naturais;
- abismos;
- rios/poças do material Nexus;
- névoa muito baixa;
- iluminação rara;
- silêncio pesado;
- nenhum "bioma colorido".

### Regra de escala

O jogador deve entrar e pensar:

"isso é grande demais para ter sido feito para mim."

## 9. O que NÃO colocar ainda

Não adicionar no primeiro remake:

- cidade completa;
- vinte mobs;
- boss final;
- lore inteira;
- árvore tecnológica Nexus;
- dezenas de minérios;
- máquinas Nexus craftáveis;
- segunda rede de portais.

Primeiro fazer o lugar existir e funcionar.

## 10. Integrações futuras

Depois do vertical slice:

### World State

Registrar:

- primeira ativação;
- quem ativou;
- local;
- falhas;
- desligamentos;
- reativações.

### Event Engine

Transformar ativação do Nexus em acontecimento histórico global.

### Media

Black Box e VHS podem registrar a ativação e acidentes.

### Structural Engine

Shockwaves podem danificar estruturas próximas de forma hierárquica.

### Time/Aging

Um Nexustor abandonado pode enferrujar, falhar ou exigir manutenção.

### Assembly

Qualidade de montagem pode alterar:

- estabilidade;
- tempo de ativação;
- chance de falha;
- consumo;
- tremor.

## 11. Caminhos possíveis a partir daqui

### Caminho A — estabilização

Antes de feature grande:

- trazer o único commit ausente de `develop-v1`;
- manter CI verde;
- rodar dedicated server;
- congelar regressões do Kraken/oceano;
- revisar versão e changelog.

É o caminho mais seguro.

### Caminho B — Nexus Remake vertical slice

Recomendado como próximo grande bloco.

Ordem:

1. separar estado/controladores sem mudar gameplay;
2. extrair ArrivalGenerator;
3. criar fases de ativação;
4. reorganizar apresentação;
5. melhorar dimensão;
6. remodelar Nexustor;
7. integrar World State.

O jogador deve perceber melhora antes de receber conteúdo novo.

### Caminho C — fundações do mundo

Focar antes em:

- Structural Engine;
- Event Engine genérico;
- integração World State;
- destruição persistente.

Esse caminho fortalece Blue, guerras, Nexus e ruínas ao mesmo tempo.

### Caminho D — oceano / Kraken

Continuar o bloco recém-aberto:

- estabilidade das profundezas;
- Kraken mais raro e memorável;
- naufrágios;
- integração com navios;
- ecologia;
- sons;
- performance.

É o caminho de menor troca de contexto.

## 12. Direção recomendada

Fazer duas etapas:

### Etapa 1 — estabilização curta

Não adicionar sistemas.

- sincronizar a linhagem;
- confirmar CI;
- confirmar dedicated server;
- documentar estado atual.

### Etapa 2 — Nexus Remake vertical slice

O Nexus é uma boa próxima feature porque toca vários pilares sem exigir terminar o mod inteiro.

Ele pode virar o laboratório arquitetural para:

- eventos globais;
- infraestrutura persistente;
- Assembly avançado;
- dimensões;
- mídia;
- história do servidor.

Se esse remake ficar sólido, sistemas absurdos futuros deixam de ser exceções e começam a caber naturalmente no mesmo mundo.

## 13. Critério de pronto para a primeira fase do remake

A primeira fase termina quando:

- gameplay atual continua funcionando;
- estados antigos carregam;
- CI está verde;
- dedicated server inicia;
- NexusEventManager perde responsabilidades;
- PortalManager não gera mais a câmara diretamente;
- ativação possui fases explícitas;
- portal mantém retorno persistente;
- dimensão continua acessível;
- desligar o Nexustor continua podendo prender jogadores;
- nenhum segundo sistema paralelo foi criado.
