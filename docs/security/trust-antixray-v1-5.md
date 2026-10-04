# Anti-Xray por evidências — recursos e oclusão visual

WayAround / NeoForge 1.21.1, protocolo **32**. Cliente e servidor precisam usar a mesma versão. Ativo por padrão em multiplayer, inclusive LAN; mundo local em singleplayer não recebe fiscalização/punições. Não adiciona aba ou item.

## O que é analisado

O cliente normal analisa os recursos **efetivos**, após sobreposição dos packs. São 16 tipos de terreno sólido e quatro controles de minério: pedra, deepslate, terra, grama, areia, cascalho, andesito, diorito, granito, tuff, netherrack, blackstone, basalto, end stone, areia vermelha e argila; diamante, ferro, ouro e redstone. Folhas, vidro, água e plantas naturalmente transparentes ficam fora.

Segue os modelos das variantes/multipart nos blockstates, herança de modelos e aliases de textura, incluindo redirecionamento para outro namespace. Mede transparência severa em PNGs e redução severa de volume/área dos modelos. Pelo menos **seis tipos distintos** de terreno precisam estar anormais, com pelo menos **dois minérios preservados**, para acumular uso confirmado. Transparência e modelo anormal no mesmo tipo contam uma vez. Três a cinco tipos anormais geram informação privada para revisão, sem sanção automática. Nomes editáveis de packs não entram na decisão.

Texturas artísticas opacas, uma textura transparente, falta de arquivos, PNG inválido, modelos com loaders customizados, ciclos/limites de resolução ou relatório ausente não condenam. Casos não suportados ficam desconhecidos; não recebem atestado de recursos limpos. A resolução de variantes é conservadora: a alteração precisa afetar pelo menos 75% dos modelos distintos do bloco observado.

Quebras de minério envolto em blocos sólidos são registradas pelo servidor como contexto para o responsável. Mineração eficiente, strip mining, sorte, TNT, máquinas e falta de telemetria **não podem gerar punição por conta própria**. Não existe atualmente análise estatística de trajetórias/veios ou sistema de ofuscação de chunks.

## Captura efêmera e comparação com o servidor

O cliente faz uma captura reduzida de **64 × 36** do mundo e de seu depth buffer, em `AFTER_LEVEL`, antes de HUD, chat e mão. Não captura desktop nem interface. O processamento é matemático, sem IA ou reconhecimento de imagens. As duas leituras da GPU ocupam cerca de 18 KiB temporários, são zeradas/liberadas ao terminar e não são gravadas em arquivos. Nenhum RGB, PNG ou imagem é enviado. O jogador recebe um aviso na primeira solicitação da conexão.

O desafio contém somente um nonce imprevisível, nunca posições de minérios. Ele escolhe **32 pontos** da área central da imagem. A resposta tem **589 bytes**: nonce, disponibilidade, posição/direção da câmera e direção, distância renderizada e brilho de cada ponto. Não contém UUID escolhido pelo cliente, relógio, nome de pack ou pixels. Ausência de captura responde com nove bytes e não incrimina.

O servidor não renderiza uma fotografia vanilla. Calcula a oclusão em seu próprio mundo: percorre os voxels de cada raio até 24 blocos, no máximo 64 células por raio, apenas em chunks já carregados. Se a profundidade renderizada coincidir com a face de um minério real atrás de terreno opaco, registra uma contradição. O brilho só descarta pontos pretos; cores sozinhas nunca identificam minério.

Uma amostra forte exige simultaneamente **12 raios cobertos**, **oito paredes aparentemente ignoradas**, **três coincidências com minérios ocultos** e **dois blocos distintos de minério**. Ignora paredes muito próximas, passagens rasantes e distâncias imprecisas. É necessário repetir o resultado em **três capturas consecutivas válidas**, separadas por no máximo 30 segundos, antes de contribuir para a mesma política de confiança dos packs. A primeira contradição já gera revisão privada; nenhuma captura isolada autoriza punição pública. Cada captura forte a partir da terceira concede no máximo cinco segundos de evidência, sem presumir todo o tempo entre capturas. Packs e capturas não contam o mesmo intervalo duas vezes.

Cadência com jitter: 10–14 segundos no subsolo fora da base conhecida, 35–51 segundos no subsolo da base, 90–134 segundos na superfície, 8–11 segundos após contradição ou histórico ativo. Reutiliza somente a base já calculada pelo sistema de comportamento, com confiança mínima, cama/containers, dimensão e proximidade; não executa novamente a busca custosa. A base altera a frequência, sem dar imunidade.

Capturas ficam fora da decisão em movimento/rotação incompatível entre desafio e resposta, atraso maior que dois segundos, início da sessão, spectator/creative, terceira pessoa, água, veículos, drones, Entity Spectate, sonhos, cinematics, gravação de mídia ou telas abertas. A verificação geométrica atual se limita ao overworld. Iris/Oculus/Immersive Portals desativam a captura visual; a análise independente de recursos permanece. Chunks recém-carregados, quebras, colocações e explosões próximas suspendem a comparação por quatro segundos. Fluidos, block entities e obstáculos opacos não suportados tornam o raio desconhecido. Fingerprints aprovados não geram sanções nem avisos visuais de suspeita.

Orçamento global: uma captura analisada por tick, fila máxima de oito; até 16 jogadores consultados e dois desafios novos por tick. O mapa de alterações tem no máximo 2.048 chunks. Não há geração/carregamento remoto de terreno. Histórico guarda apenas contagens e resumo (`covered`, `missing`, `oreBehind`, `distinctOres`, `unknown`), nunca imagem, raios, profundidades ou brilho.

## Confiança, avisos e reincidência

| Etapa padrão | Condição |
| --- | --- |
| Responsável recebe relatório | Primeira observação suspeita válida, antes do aviso ao jogador |
| Aviso privado ao jogador | 10 segundos confirmados acumulados |
| Nome divulgado no servidor | 30 segundos confirmados na rodada e pelo menos 20 novos segundos após aviso privado |
| Expulsão | 60 segundos confirmados na rodada e pelo menos 20 novos segundos após divulgação |
| Reconexão após expulsão | Nova rodada; nova advertência, divulgação e pelo menos um minuto de uso confirmado antes de nova expulsão |
| Banimento permanente | Terceira expulsão efetivamente aplicada |

Na análise de recursos, o tempo vem do servidor, entre observações consecutivas fortes. Uma amostra nunca concede mais de cinco segundos; intervalo sem resposta maior que oito segundos não acumula. Desconexão, recursos aprovados/limpos/desconhecidos e períodos sem observação forte não contam. O primeiro relatório da sessão estabelece a referência e não inventa tempo prévio. Assim, um minuto confirmado demora um pouco mais que um minuto de relógio desde a conexão. Uso breve observado continua no histórico: desligar o pack ou reconectar não limpa evidências, etapa ou tempo de uma rodada ainda não punida. Um pack limpo/desconhecido não contribui; a verificação visual independente ainda pode contribuir se satisfizer seus critérios. Um fingerprint aprovado suspende ambas as fontes para aquele conteúdo. Reincidência curta ao longo de sessões acumula; não se promete capturar um intervalo menor que a janela de amostragem.

Servidor autoriza identidade pelo jogador da conexão, nonce imprevisível de uso único, prazo e máscaras limitadas. O cliente não escolhe UUID, relógio, punição ou alvo. Falta de relatório por 30 segundos gera registro/revisão privada e pausa a contagem, sem expulsão/banimento por x-ray.

O banimento usa a lista vanilla, permanente, por perfil/UUID. `/pardon <nome>` pelo responsável remove o banimento; no próximo login a escalada é zerada, mantendo auditoria e total histórico. Aprovar um pack não desbane um jogador. Offline mode tem a limitação usual de identidades não autenticadas e não deve ser considerado proteção contra troca de identidade.

## Administração

Arquivo de servidor `config/wayaround-antixray.toml` (COMMON, consumido só no servidor; configuração do cliente não governa decisões):

- `enabled = true`: fiscalização multiplayer.
- `visualOcclusionAudit = true`: captura efêmera e comparação geométrica; `false` mantém somente a análise de recursos.
- `enforce = true`: divulgações, expulsões e bans; `false` apenas registra/revisa, sem consumir expulsões ou fingir avisos entregues. Reativação respeita novas janelas de aviso.
- `privateWarningEvidenceSeconds = 10`, `publicWarningEvidenceSeconds = 30`, `kickEvidenceSeconds = 60`: limites configuráveis; as janelas mínimas de 20 segundos entre avisos e mínimo de 60 segundos por expulsão sempre são respeitadas.
- `approvedFingerprints = []`: fingerprints SHA-256 de conteúdo revisados e aprovados. Nome do pack não concede exceção.

Comandos só com permissão **4** (console/responsável):

```text
/wayanticheat status
/wayanticheat inspect <uuid>
/wayanticheat approve <fingerprint_sha256>
/wayanticheat revoke <fingerprint_sha256>
/pardon <nome>
```

O responsável recebe mensagens privadas se estiver online com permissão 4. Console/log e histórico persistente recebem o relatório mesmo sem responsável online. Não existe uma forma confiável de identificar qual operador é o proprietário; nível 4 é a autoridade configurada. Revogue também uma aprovação duplicada no TOML, se houver.

Histórico no overworld: `data/wayaround_antixray.dat`, salvo pelo ciclo normal de SavedData/autosave, com últimas 64 entradas por UUID, contagens/etapas e aprovações. Logs vanilla (`logs/latest.log` e arquivos rotacionados) incluem `[AntiXray]`, nome, UUID, evento, máscaras, fingerprint, motivo e número da expulsão. Banimento usa `banned-players.json`. Interrupção abrupta antes do autosave pode perder alterações recentes do histórico, como outras SavedData; os logs e a banlist têm seus mecanismos próprios de gravação. Salvamento normal/reinício e desconexão não apagam o histórico.

## Limites de confiança e desempenho

Esta implementação analisa **recursos e capturas em clientes cooperativos/normais**. Cliente adulterado pode falsificar recursos, imagem, profundidade ou brilho, remover o listener ou responder sem captura; nonce evita replay acidental/entre sessões, mas não atesta integridade do cliente. Shaders, alterações fora das amostras e truques não suportados podem escapar. Movimento intenso também pode invalidar as amostras. A checagem não é estatística de mineração e não detecta todo uso breve. Máquinas/pistões e mods que modificam geometria ou profundidade podem exigir desativar a parte visual ou aprovar conteúdo após revisão; eventos acompanhados e três repetições reduzem incompatibilidades, sem garantir zero falsos positivos. Fingerprint identifica os recursos amostrados, não o arquivo ZIP inteiro. Transparência deliberada de terreno para um pack artístico também pode satisfazer critérios; o responsável precisa revisar/aprovar incompatibilidades. Não há uma prova criptográfica de trapaça nem declaração de 100% de acerto.

Análise ocorre uma vez por reload, na preparação em executor de recursos; sem PNG/JSON em ticks ou handlers de rede. Limites: 32 MiB de leitura total por análise, 64 KiB por JSON, 2 MiB por PNG, no máximo 4.194.304 pixels decodificados por PNG, 256 amostras de alpha por textura, oito pais por modelo, 16 aliases, 64 elementos por modelo, 32 variantes distintas por bloco e 256 modelos em cache temporário. Libera imagens nativas imediatamente. Recursos grandes fora do limite ficam desconhecidos, permitindo packs HD sem punição automática por tamanho.

A análise de recursos consulta no máximo 128 jogadores por tick em rodízio. Desafio de oito bytes a cada cinco segundos por jogador; resposta menor que 100 bytes, apenas máscaras/fingerprint. Contexto de mineração consulta seis vizinhos de uma quebra observada, com orçamento global de 64 leituras por tick. A comparação visual tem o orçamento separado e limitado descrito acima. Históricos crescem por UUID participante, com auditoria individual limitada a 64 eventos. Não lê arquivos arbitrários, caminhos locais, screenshots gravadas, voz ou inventário.

## Validação

- `python tests/security/run_policy.py`: falsos positivos, uso breve/retomada, avisos/grace, três rodadas/pardon, pack aprovado, relógios inválidos e todas as 65.536 máscaras possíveis de terreno. Inclui 130.015 verificações geométricas: parede normal, minério exposto/oculto, escuridão, terreno desconhecido, valores inválidos, coordenadas negativas, alcance/orçamento, cadência de base e pontos por nonce.
- `bash gradlew runSecurityGameTestServer`: nove testes de persistência/limite da auditoria, valores malformados, nonce/replay, codec/pacote limitado, banlist vanilla, ausência de condenação por mineração/recursos desconhecidos, geometria em blocos reais, codec visual de tamanho fixo e histórico sem pixels.
- Workflow `Anti-Xray trust validation`: também inicializa o cliente e executa fixtures PNG/modelos reais sobre o ResourceManager de Minecraft: recursos padrão, alpha, arte opaca, alteração isolada, modelos vazios, loaders não suportados, PNG inválido, fingerprints e redirecionamento de blockstates. Depois executa uma fixture OpenGL real: framebuffer colorido/profundidade reduzidos, raios reconstruídos, minérios atrás de parede, visão normal e restauração de estado gráfico. O marcador obrigatório é `VISUAL GPU FIXTURES PASSED`.

A fixture de GPU é uma cena controlada e não substitui teste do pipeline de renderização em gameplay. Gameplay com múltiplos clientes, janela de recarga de packs, avisos/expulsão pela conexão real, compatibilidade com packs externos e tentativa com cliente adulterado exigem verificação manual. As fixtures e testes da política não demonstram FPS nem resistência a cliente comprometido.

Referência de integração: [networking NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/networking/). Usa o pipeline de recursos e bridge client-safe existentes no mod.
