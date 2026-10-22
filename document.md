# Documentação do projeto — Bloqueador do Instagram

## 1. Objetivo

Aplicativo Android nativo que detecta a abertura do aplicativo oficial do Instagram (\`com.instagram.android\`), registra a tentativa e envia o usuário à tela inicial. A primeira tentativa cria uma penalidade de 5 minutos. Cada nova entrada distinta acrescenta 5 minutos ao tempo que ainda resta, com limite de 3 horas restantes.

O cronômetro mostra a penalidade acumulada. Ele \*\*não\*\* libera o Instagram quando chega a zero: enquanto o serviço estiver ativo, a próxima abertura também será dispensada e iniciará uma nova penalidade de 5 minutos.

## 2. Comportamento esperado

1. Na primeira abertura do bloqueador, o painel mostra \`00:00:00\` e zero tentativas.
2. O usuário ativa \*\*Bloqueio do Instagram\*\* nas configurações de acessibilidade do Android.
3. Ao abrir o Instagram, o serviço salva uma tentativa e um novo prazo, depois aciona a volta à tela inicial.
4. Ao sair do Instagram e tentar entrar novamente, o serviço acrescenta 5 minutos ao tempo restante.
5. Eventos repetidos enquanto a janela do Instagram continua ativa não acrescentam tempo. Outra janela precisa ficar ativa antes de uma nova entrada ser considerada uma nova tentativa.
6. O tempo restante é limitado a 3 horas, mesmo após muitas tentativas.
7. O prazo e o número total de tentativas persistem após o encerramento do processo e a reinicialização do aparelho.

### Regra de tempo

\`novo prazo = agora + mínimo(máximo(prazo anterior − agora, 0) + 5 minutos, 3 horas)\`

Por exemplo, se restam 4 minutos, uma nova tentativa passa a mostrar aproximadamente 9 minutos. Se o prazo já expirou, uma nova tentativa inicia 5 minutos. O limite de 3 horas é aplicado ao \*\*tempo restante\*\*, não ao número de tentativas. O número de tentativas exibido é cumulativo e não é zerado quando o cronômetro chega a zero.

## 3. Tecnologias e configuração

| Item | Configuração no projeto |
| --- | --- |
| Linguagem | Kotlin 2.0.21 |
| Interface | Jetpack Compose e Material 3 |
| Plataforma | Android nativo, módulo único \`app\` |
| Identificador do app | \`com.example.instagramblocker\` |
| Android mínimo | API 26 |
| \`compileSdk\` / \`targetSdk\` | API 35 / API 35 |
| Android Gradle Plugin | 8.7.3 |
| Gradle previsto | 8.9 |
| Java | 17 |
| Armazenamento | \`SharedPreferences\` local |
| Detecção | \`AccessibilityService\` com eventos de mudança de janela |

Não há servidor, conta de usuário, acesso à internet ou coleta do conteúdo das telas. O serviço lê apenas o tipo do evento e o nome do pacote da janela ativa.

## 4. Estrutura do código

| Arquivo | Responsabilidade |
| --- | --- |
| \`app/src/main/java/com/example/instagramblocker/MainActivity.kt\` | Tela, cronômetro, estado do serviço e acesso às configurações de acessibilidade. |
| \`app/src/main/java/com/example/instagramblocker/BlockPolicy.kt\` | Cálculo puro do novo prazo e do tempo restante. |
| \`app/src/main/java/com/example/instagramblocker/BlockStore.kt\` | Leitura e gravação local do prazo e do contador. |
| \`app/src/main/java/com/example/instagramblocker/accessibility/InstagramBlockService.kt\` | Detecção de entradas no Instagram e retorno à tela inicial. |
| \`app/src/main/java/com/example/instagramblocker/accessibility/ServiceConnectionState.kt\` | Informa à tela se o serviço está conectado no processo atual. |
| \`app/src/main/AndroidManifest.xml\` | Declara a Activity e o serviço protegido por \`BIND\_ACCESSIBILITY\_SERVICE\`. |
| \`app/src/main/res/xml/accessibility\_service\_config.xml\` | Limita o serviço a eventos de mudança de janela, sem leitura de conteúdo. |
| \`app/src/main/res/values/strings.xml\` | Textos da interface em português. |
| \`app/src/test/java/com/example/instagramblocker/BlockPolicyTest.kt\` | Testes locais da primeira tentativa, soma, limite e expiração. |

### Fluxo interno

\`\`\`text
Instagram entra em primeiro plano
  → AccessibilityService recebe TYPE\_WINDOW\_STATE\_CHANGED
  → verifica o pacote com.instagram.android
  → ignora repetição da mesma entrada
  → BlockStore grava o novo prazo e o contador
  → serviço executa a ação global HOME
  → MainActivity lê o estado salvo e atualiza o painel
\`\`\`

A gravação é feita em uma tarefa de I/O antes de mandar o usuário à tela inicial. Se a gravação falhar, o serviço não considera a tentativa concluída. Se a ação global HOME falhar, ele tenta abrir a tela inicial por uma \`Intent\`.

## 5. Preparar e executar

1. Extraia o projeto e abra no Visual Studio Code ou Android Studio a pasta que contém \`settings.gradle.kts\`.
2. Instale/configure JDK 17 ou superior e Android SDK Platform 35. No VS Code, o SDK precisa ser instalado separadamente; o editor não o fornece.
3. Instale Gradle 8.9. O projeto contém \`gradle/wrapper/gradle-wrapper.properties\`, mas ainda não contém os scripts nem o arquivo binário do Gradle Wrapper. Gere-os com \`gradle wrapper --gradle-version 8.9\` na raiz do projeto.
4. Compile e execute os testes locais:

   \`\`\`bash
   ./gradlew :app:assembleDebug :app:testDebugUnitTest
   \`\`\`

   No Windows, use \`gradlew.bat\` após gerar o wrapper.

5. Instale \`app/build/outputs/apk/debug/app-debug.apk\` em um aparelho ou emulador Android.
6. Abra o bloqueador, toque em \*\*Abrir configurações de acessibilidade\*\* e ative \*\*Bloqueio do Instagram\*\*. Essa ativação depende do usuário; o aplicativo não pode fazê-la automaticamente.

O painel distingue três estados: serviço desativado, habilitado mas ainda sem conexão com o processo e conectado. O tempo restante é atualizado aproximadamente a cada segundo enquanto a tela está aberta.

## 6. Verificação recomendada no dispositivo

| Cenário | Resultado esperado |
| --- | --- |
| Antes de qualquer tentativa | Cronômetro \`00:00:00\`, zero tentativas. |
| Primeira abertura do Instagram | Volta à tela inicial, uma tentativa, cerca de 5 minutos restantes. |
| Segunda abertura após voltar à tela inicial | Duas tentativas e 5 minutos adicionados ao restante. |
| Muitos eventos da mesma janela | Uma única tentativa para aquela entrada. |