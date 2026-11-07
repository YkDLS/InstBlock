# Arquitetura do InstBlock

## Escopo

Aplicativo Android nativo em Kotlin e Jetpack Compose, organizado em um único módulo `app`. A divisão em pacotes facilita encontrar cada responsabilidade sem introduzir novos módulos ou bibliotecas.

O namespace e o identificador de instalação continuam sendo `com.example.instagramblocker`. O monitor de uso fica em `usage/` e é um serviço em primeiro plano não exportado.

## Organização do código

Os caminhos abaixo são relativos a `app/src/main/java/com/example/instagramblocker/`.

| Arquivo ou pacote | Responsabilidade |
| --- | --- |
| `MainActivity.kt` | Lê a fase do ciclo, consulta as permissões de uso e sobreposição e inicia ações de atualização. |
| `BlockedActivity.kt` | Componente legado preservado para compatibilidade; o monitor atual usa sobreposição. |
| `domain/BlockPolicy.kt` | Calcula bloqueio, liberação de dez minutos e tempo restante monotônico, com proteção contra overflow. |
| `domain/BlockState.kt` | Representa fase, prazo monotônico, boot/checkpoint e contadores. |
| `data/BlockStore.kt` | Registra tentativas e usa uma implementação privada de persistência em `SharedPreferences`. |
| `data/BlockStatePersistence.kt` | Contrato de leitura e gravação usado pelo store e por seus testes. |
| `data/SystemTimer.kt` | Fornece `elapsedRealtime` e o contador de boots do Android. |
| `usage/InstagramMonitor.kt` | Consulta eventos de uso, registra entradas no Instagram e mostra a sobreposição de bloqueio. |
| `usage/UsageAccess.kt` | Confere a autorização de acesso aos dados de uso. |
| `accessibility/InstagramTarget.kt` | Aceita somente o pacote `com.instagram.android`. |
| `updates/UpdateChecker.kt`, `updates/UpdateInfo.kt` | Consultam Releases, representam a atualização, baixam o APK e abrem o instalador. |
| `ui/dashboard/DashboardScreen.kt`, `ui/dashboard/UpdateUiState.kt` | Painel e estado visual das atualizações. |
| `ui/blocked/BlockedScreen.kt` | Tela legada associada a `BlockedActivity`. |
| `notifications/` | Mantém o canal, o cronômetro na notificação, os avisos de transição e a restauração após boot. |
| `ui/components/StatCard.kt` | Card de contagem reutilizado no painel. |
| `ui/theme/Theme.kt` | Paleta e tema Compose do painel. |

As Activities ainda coordenam estado e operações. A extração das funções Compose preserva esse funcionamento; não introduz ViewModels, injeção de dependências ou uma nova camada de repositório.

## Fluxo de bloqueio

1. O serviço em primeiro plano consulta `UsageStatsManager.queryEvents()` a cada segundo.
2. `InstagramTarget` verifica se o pacote é o Instagram oficial.
3. O serviço permite a entrada durante a janela automática de dez minutos e ignora eventos duplicados ou uma tentativa já em processamento.
4. `BlockStore` calcula o novo prazo, incrementa os contadores e grava o estado em uma tarefa de I/O.
5. Após a gravação bem-sucedida, o serviço mostra uma sobreposição sobre o Instagram.
6. Ao detectar outro app em primeiro plano, remove a sobreposição.

Eventos de entrada são deduplicados. Durante a liberação, não geram tentativas. O monitor consulta eventos de todos os pacotes para reconhecer a saída do Instagram, mas só registra tentativas do pacote oficial.

Se a gravação falhar, a tentativa não é considerada concluída. O monitor depende de autorização de uso e sobreposição, e o sistema pode limitar a execução em segundo plano.

## Regra de tempo

```text
restante = máximo(prazo anterior − agora, 0)
novo prazo = agora + mínimo(restante + 5 minutos, 3 horas)
```

Se restam quatro minutos, a próxima tentativa passa a mostrar aproximadamente nove minutos. Depois que a liberação de dez minutos termina, outra tentativa inicia cinco minutos. A implementação também limita a soma para evitar overflow de `Long`.

Quando o cronômetro chega a zero, começa uma janela de liberação de dez minutos corridos, ancorada no prazo persistido. A sobreposição é removida. Durante a liberação, entradas não alteram prazo nem contadores. Depois dela, se o Instagram ainda estiver em primeiro plano e a tela estiver desbloqueada, o monitor registra uma tentativa e inicia cinco minutos de bloqueio imediatamente, sem depender de um novo evento de entrada. Fora do Instagram, a próxima entrada inicia o bloqueio. Os contadores não são zerados quando o tempo termina; a contagem diária usa a data local do aparelho.

## Persistência e ciclo automático

| Arquivo de preferências | Chaves preservadas |
| --- | --- |
| `block_state` | `deadline_ms`, `attempts`, `attempts_by_day`, `timer_phase`, `timer_boot_count`, `timer_checkpoint_remaining_ms` |

`attempts_by_day` mantém a serialização existente de pares `data=contagem` separados por `|`. O painel mostra os últimos sete dias que possuem registros, não necessariamente sete dias consecutivos.

Não existe mais pausa manual. Instalações atualizadas podem conservar o arquivo antigo `block_control`, mas suas chaves deixam de ser lidas e não alteram o ciclo.

O significado de `deadline_ms` depende de `timer_phase`: é o fim monotônico da fase bloqueada ou da liberação atual. O conjunto sobrevive ao encerramento do processo. As permissões especiais são consultadas separadamente do estado do ciclo.

O prazo ativo usa `SystemClock.elapsedRealtime()`, independente da hora civil. A fase, o boot atual e o restante são salvos em checkpoints de cinco segundos. No desligamento normal há um checkpoint imediato. Depois de outro boot, o prazo é reconstruído a partir do restante salvo; em uma queda abrupta podem ser repetidos até cinco segundos, mas nenhum tempo é removido. Estados criados por versões antigas são migrados uma única vez a partir do prazo civil anterior.

O serviço atualiza a notificação do bloqueio a cada segundo e produz avisos ao entrar na liberação ou voltar ao estado pronto. `TimerReceiver` recebe `ACTION_SHUTDOWN` para um checkpoint final e `BOOT_COMPLETED` para reconstruir o prazo e restaurar a notificação. Android 13 ou superior exige a permissão de notificações solicitada pela tela principal; negar a permissão não desativa a política de bloqueio.

## Atualizações e privacidade

O verificador consulta `https://api.github.com/repos/YkDLS/InstBlock/releases/latest` quando o usuário solicita. Seleciona um asset APK de uma versão considerada mais nova, baixa para o cache e abre o instalador pelo `FileProvider` já declarado no manifesto. A autoridade continua baseada em `${applicationId}.fileprovider`.

A comparação de versão existente usa partes numéricas; não é uma implementação completa de Semantic Versioning. O download usa HTTPS e os hosts permitidos no código. A instalação depende do Android e da confirmação do usuário.

O serviço consulta eventos de uso e nomes de pacotes, sem solicitar leitura do conteúdo das telas. Não há conta ou servidor próprio. A internet é usada para as atualizações solicitadas pelo usuário. Veja [SECURITY.md](../SECURITY.md).

## Verificação

Os testes em `app/src/test/java/com/example/instagramblocker/` espelham os pacotes `domain/`, `data/`, `accessibility/` e `updates/`. Cobrem cálculo do prazo, limite, transições de fase, alteração da hora civil, restauração após reboot, persistência, falha de gravação, contadores, identificação do pacote oficial e validação dos metadados de atualização.

O GitHub Actions executa testes locais, montagem do APK de debug e lint. Ciclo de vida Android, deduplicação de eventos reais, ciclo de liberação e instalação de atualizações também exigem a [verificação no aparelho](testing.md).
