# Documentação funcional — InstBlock v1.3.0

Este documento resume o comportamento do aplicativo para produto, desenvolvimento e validação. A descrição técnica detalhada está em [docs/architecture.md](docs/architecture.md), e o roteiro de testes no aparelho está em [docs/testing.md](docs/testing.md).

## 1. Objetivo

O InstBlock é um aplicativo Android nativo de autocontrole. Ele detecta a abertura do aplicativo oficial do Instagram (`com.instagram.android`), registra a tentativa e apresenta uma tela de bloqueio. Não bloqueia o Instagram no navegador nem outros pacotes.

O monitor usa `UsageStatsManager` para detectar a entrada do Instagram oficial e uma sobreposição para bloquear seu uso. Não solicita acessibilidade nem leitura do conteúdo da tela. A compatibilidade com apps bancários que restringem sobreposições precisa ser confirmada no aparelho.

## 2. Ciclo automático

1. A instalação limpa começa em **Proteção pronta**, com cronômetro e contadores zerados.
2. A primeira entrada no Instagram registra uma tentativa e inicia cinco minutos de bloqueio.
3. Cada nova entrada bloqueada acrescenta cinco minutos ao tempo restante, até o máximo de três horas.
4. Eventos recebidos no intervalo de um segundo são tratados como duplicados.
5. Quando o bloqueio termina, começa uma liberação de dez minutos corridos. Durante esse período, entradas no Instagram não geram novas tentativas.
6. Ao final da liberação, a proteção volta sozinha. A próxima entrada inicia um novo bloqueio de cinco minutos.

Não existe botão de pausa manual. A sobreposição é retirada quando o período bloqueado termina ou quando o usuário sai do Instagram pela navegação do Android.

### Regra de acréscimo

```text
restante = máximo(prazo monotônico − agora monotônico, 0)
novo prazo = agora monotônico + mínimo(restante + 5 minutos, 3 horas)
```

Os contadores total e diário continuam acumulados entre ciclos. O painel mostra o total, as tentativas do dia local e até sete datas com registros.

## 3. Relógio resistente a alterações

O bloqueio usa `SystemClock.elapsedRealtime()`, e não a data ou hora mostrada pelo aparelho. Adiantar ou atrasar o relógio civil não altera a espera.

A fase, o contador de boots e o restante são persistidos em `SharedPreferences`. O serviço cria checkpoints a cada cinco segundos e também tenta salvar no desligamento normal. Depois de reiniciar, reconstrói o prazo com o restante salvo:

- o tempo em que o aparelho ficou desligado não é descontado;
- um desligamento inesperado pode repetir até aproximadamente cinco segundos;
- reiniciar nunca reduz a espera.

Estados criados por versões anteriores são migrados uma única vez a partir do prazo antigo.

## 4. Notificações

O canal **Tempo de foco** apresenta:

- uma notificação persistente com o tempo restante durante o bloqueio;
- a contagem da janela de liberação;
- um aviso quando o Instagram é liberado;
- um aviso quando a proteção volta.

Android 13 ou superior exige que o usuário conceda `POST_NOTIFICATIONS`. Negar essa permissão oculta os avisos, mas não desativa a regra de bloqueio.

## 5. Instalação pelo GitHub

1. Abra [Releases](https://github.com/YkDLS/InstBlock/releases) no celular.
2. Escolha a versão e baixe o arquivo APK anexado — não os arquivos automáticos **Source code**.
3. Autorize a instalação para o navegador ou gerenciador de arquivos, se o Android solicitar.
4. Instale e abra o InstBlock.
5. Permita notificações quando solicitado.
6. Toque em **Configurar próxima permissão necessária**, conceda acesso aos dados de uso e permissão para exibir sobre outros apps.

Uma atualização preserva dados somente quando usa o mesmo `applicationId` e uma assinatura compatível. Builds debug assinados com chaves diferentes podem exigir a desinstalação da versão anterior; a desinstalação apaga o histórico local.

### Migração da v1.1.2 para a v1.2.0

O APK publicado da v1.1.2 foi assinado com uma chave debug efêmera que não está mais disponível. A v1.2.0, portanto, exige desinstalar a versão anterior e fazer uma instalação limpa, com perda do histórico local. A assinatura da v1.2.0 deve ser preservada para que as próximas atualizações possam ser instaladas sem apagar dados.

## 6. Tecnologias e versão

| Item | Configuração |
| --- | --- |
| Versão | `1.3.0` (`versionCode 7`) |
| Linguagem | Kotlin 2.0.21 |
| Interface | Jetpack Compose e Material 3 |
| Identificador | `com.example.instagramblocker` |
| Android mínimo | Android 8.0 / API 26 |
| `compileSdk` / `targetSdk` | 35 / 35 |
| Android Gradle Plugin | 8.7.3 |
| Gradle Wrapper | 8.9 |
| Java | 17 |
| Persistência | `SharedPreferences` local |
| Detecção | `UsageStatsManager` e serviço em primeiro plano |

## 7. Compilar e verificar

O repositório já contém o Gradle Wrapper. Com JDK 17 e Android SDK 35 configurados, execute:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

Em Linux ou macOS:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. Não versione APKs, keystores, tokens ou `local.properties`.

## 8. Emulador e aparelho

Para instalar no dispositivo reconhecido pelo ADB:

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Como configuração de referência, crie no Device Manager do Android Studio um Pixel 7 x86_64 com Android 15 / API 35. Prefira uma imagem com Google Play quando precisar instalar o Instagram oficial. Imagens sem Play Store podem validar instalação, telas, persistência e notificações, mas não substituem o teste do bloqueio no pacote real.

## 9. Atualizações dentro do aplicativo

Quando o usuário toca em **Verificar atualizações**, o aplicativo consulta a Release mais recente do repositório, seleciona um asset APK válido, baixa-o para o cache e abre o instalador do Android pelo `FileProvider`. A instalação sempre depende da confirmação do usuário e de uma assinatura compatível.
