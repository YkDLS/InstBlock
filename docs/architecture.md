# Arquitetura do InstBlock

## Escopo

Aplicativo Android nativo em Kotlin e Jetpack Compose, organizado em um único módulo `app`. A divisão em pacotes facilita encontrar cada responsabilidade sem introduzir novos módulos ou bibliotecas.

O namespace e o identificador de instalação continuam sendo `com.example.instagramblocker`. As Activities permanecem nesse pacote, e o serviço permanece em `accessibility/`, preservando os componentes declarados no manifesto.

## Organização do código

Os caminhos abaixo são relativos a `app/src/main/java/com/example/instagramblocker/`.

| Arquivo ou pacote | Responsabilidade |
| --- | --- |
| `MainActivity.kt` | Lê o estado, coordena pausa e retomada, consulta a acessibilidade e inicia ações de atualização. |
| `BlockedActivity.kt` | Carrega o estado da tela de bloqueio e executa a ação de voltar à tela inicial. |
| `domain/BlockPolicy.kt` | Calcula o prazo e o tempo restante, com proteção contra overflow. |
| `domain/BlockState.kt` | Representa prazo, contador acumulado e tentativas por dia. |
| `domain/BlockingState.kt` | Representa ativação do bloqueio e tempo congelado na pausa. |
| `data/BlockStore.kt` | Registra tentativas e usa uma implementação privada de persistência em `SharedPreferences`. |
| `data/BlockStatePersistence.kt` | Contrato de leitura e gravação usado pelo store e por seus testes. |
| `data/BlockControlStore.kt` | Persiste ativação, pausa e retomada. |
| `accessibility/InstagramBlockService.kt` | Observa entradas, evita contagens repetidas, persiste a tentativa e bloqueia a janela. |
| `accessibility/InstagramTarget.kt` | Aceita somente o pacote `com.instagram.android`. |
| `accessibility/ServiceConnectionState.kt` | Expõe a conexão do serviço no processo atual por `StateFlow`. |
| `updates/UpdateChecker.kt`, `updates/UpdateInfo.kt` | Consultam Releases, representam a atualização, baixam o APK e abrem o instalador. |
| `ui/dashboard/DashboardScreen.kt`, `ui/dashboard/UpdateUiState.kt` | Painel e estado visual das atualizações. |
| `ui/blocked/BlockedScreen.kt` | Conteúdo visual da tela de bloqueio. |
| `ui/components/StatCard.kt` | Card de contagem reutilizado no painel. |
| `ui/theme/Theme.kt` | Paleta e tema Compose do painel. |

As Activities ainda coordenam estado e operações. A extração das funções Compose preserva esse funcionamento; não introduz ViewModels, injeção de dependências ou uma nova camada de repositório.

## Fluxo de bloqueio

1. O serviço recebe um evento `TYPE_WINDOW_STATE_CHANGED`.
2. `InstagramTarget` verifica se o pacote é o Instagram oficial.
3. O serviço ignora a entrada quando o bloqueio está pausado, quando a mesma entrada já foi contada ou quando há uma tentativa em processamento.
4. `BlockStore` calcula o novo prazo, incrementa os contadores e grava o estado em uma tarefa de I/O.
5. Após a gravação bem-sucedida, o serviço tenta abrir `BlockedActivity`.
6. Se essa abertura falhar, tenta a ação global HOME e, em seguida, uma Intent para a tela inicial.

Eventos consecutivos do Instagram não geram novas tentativas. Uma janela de outro aplicativo ou do launcher precisa assumir o foco para que outra entrada seja contada. A própria tela de bloqueio não libera essa trava.

Se a gravação falhar, a tentativa não é considerada concluída e o serviço permite uma nova tentativa de processamento. A sequência crítica entre gravação e bloqueio usa `NonCancellable` para evitar uma interrupção entre as duas ações.

## Regra de tempo

```text
restante = máximo(prazo anterior − agora, 0)
novo prazo = agora + mínimo(restante + 5 minutos, 3 horas)
```

Se restam quatro minutos, a próxima tentativa passa a mostrar aproximadamente nove minutos. Após a expiração, outra tentativa inicia cinco minutos. A implementação também limita a soma para evitar overflow de `Long`.

O cronômetro não controla a liberação do Instagram: quando chega a zero, o serviço continua bloqueando novas entradas enquanto estiver ativo e não pausado. Os contadores não são zerados quando o tempo termina; a contagem diária usa a data local do aparelho.

## Persistência, pausa e retomada

| Arquivo de preferências | Chaves preservadas |
| --- | --- |
| `block_state` | `deadline_ms`, `attempts`, `attempts_by_day` |
| `block_control` | `blocking_enabled`, `paused_remaining_ms` |

`attempts_by_day` mantém a serialização existente de pares `data=contagem` separados por `|`. O painel mostra os últimos sete dias que possuem registros, não necessariamente sete dias consecutivos.

Ao pausar, `BlockControlStore` salva o tempo restante e desativa o bloqueio sem desligar a acessibilidade. O painel mostra esse tempo congelado. Ao retomar, a Activity obtém um novo prazo a partir do tempo salvo e o persiste em `BlockStore`.

O estado persistido sobrevive ao encerramento do processo. A conexão exposta por `ServiceConnectionState` representa apenas o processo atual e é consultada separadamente da configuração de acessibilidade do Android.

## Atualizações e privacidade

O verificador consulta `https://api.github.com/repos/YkDLS/InstBlock/releases/latest` quando o usuário solicita. Seleciona um asset APK de uma versão considerada mais nova, baixa para o cache e abre o instalador pelo `FileProvider` já declarado no manifesto. A autoridade continua baseada em `${applicationId}.fileprovider`.

A comparação de versão existente usa partes numéricas; não é uma implementação completa de Semantic Versioning. O download usa HTTPS e os hosts permitidos no código. A instalação depende do Android e da confirmação do usuário.

O serviço observa mudanças de janela e nomes de pacotes, sem solicitar leitura do conteúdo das telas. Não há conta ou servidor próprio. A internet é usada para as atualizações solicitadas pelo usuário. Veja [SECURITY.md](../SECURITY.md).

## Verificação

Os testes em `app/src/test/java/com/example/instagramblocker/` espelham os pacotes `domain/`, `data/` e `accessibility/`. Cobrem cálculo do prazo, limite, expiração, persistência, falha de gravação, contadores e identificação do pacote oficial.

O GitHub Actions executa testes locais, montagem do APK de debug e lint. Ciclo de vida Android, deduplicação de eventos reais, pausa e instalação de atualizações também exigem a [verificação no aparelho](testing.md).
