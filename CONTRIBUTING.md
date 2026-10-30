# Como contribuir

## Preparar o ambiente

Siga os requisitos e os comandos do [README](README.md#ambiente-de-desenvolvimento). Use JDK 17 e Android SDK Platform 35. O Gradle Wrapper está incluído; não é necessário instalar o Gradle globalmente.

Crie uma branch a partir da `main` atualizada. Exemplos de nomes: `fix/attempt-count`, `feat/daily-history` e `docs/setup`.

## Onde colocar as alterações

- `domain/`: regras de tempo e modelos sem dependências de Android ou Compose.
- `data/`: armazenamento local e operações que persistem o estado.
- `accessibility/`: integração com eventos e ações de acessibilidade do Android.
- `updates/`: integração com GitHub Releases e instalação de APK.
- `ui/`: telas, componentes e tema em Compose.

Mantenha os testes nos pacotes correspondentes dentro de `app/src/test/java/`. Atualize a documentação quando mudar o comportamento ou a preparação do ambiente. Veja [a arquitetura](docs/architecture.md) antes de alterar o fluxo de bloqueio.

## Antes de abrir um pull request

1. Execute os testes, a compilação e o lint pelos comandos do README.
2. Para mudanças de comportamento Android, execute os cenários aplicáveis de [docs/testing.md](docs/testing.md).
3. Confira o diff e descreva o problema, a mudança e os resultados das verificações. Informe quais verificações não puderam ser executadas.
4. Aguarde o resultado do GitHub Actions e a revisão antes do merge.

Não inclua `local.properties`, tokens, senhas, chaves de assinatura, keystores ou APKs nos arquivos versionados. Artefatos de distribuição devem ser anexados às Releases, não adicionados ao código-fonte.

## Compatibilidade e publicação

O `applicationId` atual é `com.example.instagramblocker`. Não o altere em uma reorganização: uma mudança de identificador faz o Android tratar o app como outra instalação.

Preserve os nomes dos arquivos e das chaves de `SharedPreferences`, ou prepare uma migração explícita. Uma atualização também precisa manter uma assinatura compatível com a instalação anterior para preservar os dados sem reinstalar.

Para publicar uma nova versão, incremente `versionCode`, atualize `versionName` em `app/build.gradle.kts` e use uma tag correspondente, como `v1.1.3`. Compile, valide no aparelho e publique o APK conforme a estratégia de assinatura do mantenedor. Não substitua silenciosamente um artefato já publicado. Os APKs atualmente disponíveis nas Releases são builds de debug; esta organização não muda o processo de assinatura.

A escolha de uma licença cabe ao mantenedor; este documento não concede uma licença de uso.
