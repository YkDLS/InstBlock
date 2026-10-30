# InstBlock

Aplicativo Android para ajudar a manter o foco longe do Instagram. Desenvolvido em Kotlin e Jetpack Compose, detecta a abertura do aplicativo oficial, registra a tentativa e apresenta uma tela de bloqueio.

## Como funciona

- Bloqueia apenas o aplicativo oficial do Instagram (`com.instagram.android`).
- Cada entrada distinta acrescenta cinco minutos ao cronômetro, com limite de três horas restantes.
- Mostra o total de tentativas, as tentativas de hoje e os últimos sete dias com registros.
- Permite pausar o bloqueio para trabalho, congelando o tempo restante até a retomada.
- Consulta e baixa atualizações do GitHub quando solicitado pelo usuário.

**O cronômetro zerado não libera o Instagram.** Enquanto o serviço estiver ativo e o bloqueio não estiver pausado, uma nova entrada será bloqueada e iniciará mais cinco minutos. O app não bloqueia o acesso pelo navegador nem outros aplicativos.

## Instalação e uso

1. Acesse as [Releases](https://github.com/YkDLS/InstBlock/releases) e baixe o APK da versão desejada. Os APKs atualmente publicados são builds de debug.
2. Instale no aparelho Android. Se solicitado, autorize a instalação para o aplicativo usado no download.
3. Abra o InstBlock e toque em **Abrir configurações de acessibilidade**.
4. Ative **Bloqueio do Instagram** nas configurações de acessibilidade do Android. Os nomes dos menus podem variar conforme o aparelho.
5. Volte ao painel para conferir a conexão do serviço. Use a opção de pausa quando precisar acessar o Instagram para trabalho.

O serviço depende da ativação manual pelo usuário. O InstBlock é uma ferramenta de autocontrole e pode ser pausado ou desativado nas configurações.

## Privacidade e permissões

Os dados de tentativas e bloqueio ficam em `SharedPreferences` no aparelho. O app não possui login nem servidor próprio.

| Permissão ou recurso | Finalidade |
| --- | --- |
| Serviço de acessibilidade | Observa mudanças de janela e compara o nome do pacote com o Instagram oficial; não solicita leitura do conteúdo da tela. |
| Internet | Consulta Releases e baixa o APK após uma ação do usuário. |
| Instalação de pacotes | Abre o instalador do Android para o APK baixado; a instalação depende da confirmação do usuário. |

Veja também a [política de segurança e privacidade](SECURITY.md).

## Ambiente de desenvolvimento

| Item | Versão/configuração |
| --- | --- |
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
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Linux/macOS:

```bash
bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

O Wrapper baixa o Gradle na primeira execução; o build também precisa acessar os repositórios de dependências.

- APK de debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Relatório de testes: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Relatório de lint: `app/build/reports/lint-results-debug.html`.

O [GitHub Actions](https://github.com/YkDLS/InstBlock/actions/workflows/android.yml) executa esses três passos em pushes e pull requests. A verificação manual no aparelho está em [docs/testing.md](docs/testing.md).

## Organização do projeto

O projeto usa um único módulo Android, `app`. Os pacotes abaixo ficam em `app/src/main/java/com/example/instagramblocker/`.

| Caminho | Responsabilidade |
| --- | --- |
| `MainActivity.kt`, `BlockedActivity.kt` | Integração das telas com o ciclo de vida e as ações do Android. |
| `domain/` | Regras de tempo e modelos de estado. |
| `data/` | Persistência local, contagem de tentativas e controle de pausa. |
| `accessibility/` | Detecção do Instagram, bloqueio e estado da conexão do serviço. |
| `updates/` | Consulta de Releases, download e abertura do instalador. |
| `ui/dashboard/`, `ui/blocked/` | Telas em Compose. |
| `ui/components/`, `ui/theme/` | Componentes e tema do painel. |
| `app/src/test/java/com/example/instagramblocker/` | Testes locais com pacotes que espelham o código de produção. |
| `docs/` | Arquitetura e roteiro de verificação manual. |

Os detalhes do fluxo e do armazenamento estão em [docs/architecture.md](docs/architecture.md).

## Contribuição e versões

Consulte [CONTRIBUTING.md](CONTRIBUTING.md) antes de preparar uma alteração. A versão do app é definida por `versionCode` e `versionName` em `app/build.gradle.kts`; o histórico publicado fica nas [Releases](https://github.com/YkDLS/InstBlock/releases).

O repositório ainda não possui um arquivo de licença. A escolha da licença permanece pendente do mantenedor.
