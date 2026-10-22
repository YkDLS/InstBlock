# Bloqueador do Instagram

Aplicativo Android nativo em Kotlin e Jetpack Compose que bloqueia somente o aplicativo oficial do Instagram (`com.instagram.android`). Ao detectar uma entrada, mostra uma tela bem-humorada, registra a tentativa e acrescenta cinco minutos ao bloqueio, com limite de três horas.

O painel exibe o cronômetro, o total de tentativas e o histórico por dia. O bloqueio pode ser pausado para uma atividade de trabalho; durante a pausa, o cronômetro fica congelado e continua do mesmo ponto quando o bloqueio é reativado.

## Privacidade

O aplicativo não usa login nem servidor próprio. A internet é usada somente quando você toca em **Verificar atualizações**, para consultar e baixar uma Release do GitHub. O serviço de acessibilidade observa somente o tipo do evento e o nome do pacote da janela ativa. Não lê o conteúdo das telas e só reage ao pacote oficial `com.instagram.android`.

## Requisitos

- Android API 26 ou superior;
- compileSdk e targetSdk 35;
- JDK 17 ou superior;
- Android SDK Platform 35;
- Gradle Wrapper 8.9 incluído no projeto.

## Compilar e testar

No Windows:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

O APK de debug será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Para usar no aparelho, instale o APK, abra o aplicativo e ative **Bloqueio do Instagram** em **Configurações > Acessibilidade > Aplicativos instalados**. A permissão de acessibilidade é ativada manualmente pelo usuário.

## Versionamento

A versão atual é `1.1.1` (`versionCode 4`). Para cada nova versão publicada, incremente o `versionCode` e atualize o `versionName` em `app/build.gradle.kts`.

## Publicação

O projeto está preparado para ser versionado no GitHub. `local.properties`, diretórios de build, arquivos `.idea` e APKs estão no `.gitignore`. Antes de publicar, inicialize o Git, revise os arquivos staged e escolha uma licença para o repositório.
