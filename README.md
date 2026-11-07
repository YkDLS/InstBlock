# InstBlock

Aplicativo Android para ajudar a manter o foco longe do Instagram. Desenvolvido em Kotlin e Jetpack Compose, detecta a abertura do aplicativo oficial, registra a tentativa e apresenta uma tela de bloqueio.

## Como funciona

- Bloqueia apenas o aplicativo oficial do Instagram (`com.instagram.android`).
- Cada entrada distinta acrescenta cinco minutos ao cronômetro, com limite de três horas restantes.
- Quando o cronômetro zera, libera o Instagram por dez minutos corridos.
- Usa o relógio monotônico do Android: mudar a hora não reduz a espera e reiniciar restaura o tempo salvo.
- Exibe uma notificação com o tempo restante e avisa quando o Instagram é liberado ou a proteção volta.
- Mostra o total de tentativas, as tentativas de hoje e os últimos sete dias com registros.
- Usa um ciclo automático, sem botão de pausa manual.
- Consulta e baixa atualizações do GitHub quando solicitado pelo usuário.

Ao terminar o bloqueio, a janela de dez minutos começa imediatamente e continua correndo mesmo que o Instagram seja fechado. Depois dela, a próxima entrada inicia um novo bloqueio de cinco minutos. O app não bloqueia o acesso pelo navegador nem outros aplicativos.

## Instalação e uso

1. Acesse as [Releases](https://github.com/YkDLS/InstBlock/releases), escolha a versão e baixe o arquivo `.apk` anexado. Os arquivos automáticos **Source code** não são instaladores. Os APKs atualmente publicados são builds de debug.
2. Instale no aparelho Android. Se solicitado, autorize a instalação para o aplicativo usado no download.
3. Abra o InstBlock e toque em **Configurar próxima permissão necessária**.
4. Conceda **Acesso aos dados de uso** e, em seguida, **Exibir sobre outros apps** nas configurações do Android.
5. Volte ao painel para conferir o estado da proteção e do ciclo automático.

O monitor depende dessas duas permissões e mantém uma notificação enquanto consulta os eventos de uso em segundo plano. O InstBlock é uma ferramenta de autocontrole e pode ser desativado nas configurações do Android.

Para atualizar sem perder o histórico, o novo APK precisa ter o mesmo identificador e uma assinatura compatível. Builds debug gerados com chaves diferentes podem exigir desinstalar a versão anterior; a desinstalação apaga os dados locais.

> **Migração para a v1.2.0:** o APK da v1.1.2 foi gerado com uma chave debug efêmera que não está mais disponível. Por isso, quem já instalou a v1.1.2 precisa desinstalá-la antes de instalar a v1.2.0. Essa instalação limpa apaga o histórico local. As próximas versões devem reutilizar a chave preservada da v1.2.0 ou uma chave de release controlada.

A espera é medida pelo tempo interno desde o boot, não pela hora exibida na tela. Enquanto o aparelho permanece ligado, inclusive com a tela apagada, a contagem continua. Ao desligar ou reiniciar, o último checkpoint é restaurado; uma interrupção inesperada pode acrescentar poucos segundos, mas nunca encurta a espera. No Android 13 ou superior, permita as notificações para acompanhar o cronômetro e as mudanças de fase.

## Privacidade e permissões

Os dados de tentativas e bloqueio ficam em `SharedPreferences` no aparelho. O app não possui login nem servidor próprio.

| Permissão ou recurso | Finalidade |
| --- | --- |
| Acesso aos dados de uso | Consulta o app em primeiro plano para detectar a entrada no Instagram oficial. |
| Exibir sobre outros apps | Mostra a tela de bloqueio quando o Instagram está em primeiro plano. |
| Serviço em primeiro plano | Mantém o monitor ativo e exibe uma notificação persistente. |
| Internet | Consulta Releases e baixa o APK após uma ação do usuário. |
| Instalação de pacotes | Abre o instalador do Android para o APK baixado; a instalação depende da confirmação do usuário. |
| Notificações | Mostra o tempo restante e avisa quando começa a liberação ou quando a proteção volta. |
| Inicialização do aparelho | Reconstrói o prazo monotônico após o boot e restaura a notificação da fase ativa. |

Veja também a [política de segurança e privacidade](SECURITY.md).

O monitor consulta eventos de uso do sistema e aplica o bloqueio somente quando o pacote em primeiro plano é o Instagram oficial. A sobreposição é retirada ao trocar de aplicativo. Bancos podem aplicar regras próprias contra sobreposições ou permissões especiais; confirme a compatibilidade no aparelho.

## Ambiente de desenvolvimento

| Item | Versão/configuração |
| --- | --- |
| Versão do app no código | 1.3.1 (`versionCode 8`) |
| Android mínimo | Android 8.0 / API 26 |
| Android SDK Platform | 35 (`compileSdk` e `targetSdk`) |
| JDK usado no CI | 17 |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3 |
| Gradle Wrapper | 8.9, incluído no repositório |

Clone o projeto e abra a pasta que contém `settings.gradle.kts` no Android Studio. Configure o SDK pelo Android Studio ou por `ANDROID_HOME`. Caso use `local.properties`, esse arquivo deve permanecer local.

### Compilar e verificar

Windows (PowerShell):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

Linux/macOS:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

O Wrapper baixa o Gradle na primeira execução; o build também precisa acessar os repositórios de dependências.

- APK de debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Relatório de testes: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Relatório de lint: `app/build/reports/lint-results-debug.html`.

O [GitHub Actions](https://github.com/YkDLS/InstBlock/actions/workflows/android.yml) executa esses três passos em pushes e pull requests. Esse workflow valida o projeto, mas não publica o APK automaticamente; o artefato de distribuição precisa ser anexado a uma GitHub Release. A verificação manual no aparelho está em [docs/testing.md](docs/testing.md).

### Landing page

A página estática fica em `landing-page/dist/` e não usa dependências npm. Com Node.js instalado, execute na raiz do repositório:

```powershell
node .\landing-page\serve.mjs
```

Abra `http://127.0.0.1:4173`. A demonstração do telefone é composta por HTML e CSS, com telas editáveis em `landing-page/dist/index.html`, estilos em `landing-page/dist/styles.css` e interação/rolagem em `landing-page/dist/app.js`. A página acompanha a navegação por capítulos, e o botão no mockup executa a demonstração interativa. Para validar o servidor e as preferências de movimento, consulte [docs/testing.md](docs/testing.md).

O workflow `Deploy landing page` publica `landing-page/dist/` no GitHub Pages quando há alterações nessa pasta na branch `main`. O endereço público é exibido na configuração Pages do repositório após a primeira execução bem-sucedida.

O workflow [Security audit](https://github.com/YkDLS/InstBlock/actions/workflows/security.yml) executa testes, lint, análise CodeQL e verificações do manifesto/dependências. O Dependabot monitora Gradle e GitHub Actions. Consulte [docs/security-testing.md](docs/security-testing.md) para o escopo e limites.

## Organização do projeto

O projeto usa um único módulo Android, `app`. Os pacotes abaixo ficam em `app/src/main/java/com/example/instagramblocker/`.

| Caminho | Responsabilidade |
| --- | --- |
| `MainActivity.kt` | Painel, permissões e ações do Android. `BlockedActivity.kt` é componente legado sem uso no fluxo atual. |
| `domain/` | Regras de tempo e modelos de estado. |
| `data/` | Persistência local, relógio monotônico e contagem de tentativas. |
| `usage/`, `accessibility/InstagramTarget.kt` | Monitoramento de uso e identificação do pacote oficial do Instagram. |
| `notifications/` | Canal de notificação, cronômetro, avisos de transição e restauração após boot. |
| `updates/` | Consulta de Releases, download e abertura do instalador. |
| `ui/dashboard/`, `ui/blocked/` | Telas em Compose. |
| `ui/components/`, `ui/theme/` | Componentes e tema do painel. |
| `app/src/test/java/com/example/instagramblocker/` | Testes locais com pacotes que espelham o código de produção. |
| `docs/` | Arquitetura e roteiro de verificação manual. |

Os detalhes do fluxo e do armazenamento estão em [docs/architecture.md](docs/architecture.md).

## Contribuição e versões

Consulte [CONTRIBUTING.md](CONTRIBUTING.md) antes de preparar uma alteração. A versão do app é definida por `versionCode` e `versionName` em `app/build.gradle.kts`; o histórico publicado fica nas [Releases](https://github.com/YkDLS/InstBlock/releases).

O repositório ainda não possui um arquivo de licença. A escolha da licença permanece pendente do mantenedor.
