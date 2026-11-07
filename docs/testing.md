# Verificação do InstBlock

## Verificações automatizadas

Execute os três comandos Gradle descritos no [README](../README.md#compilar-e-verificar): `:app:testDebugUnitTest`, `:app:assembleDebug` e `:app:lintDebug`. O GitHub Actions também os executa em pushes e pull requests, mas não publica o APK automaticamente.

Os testes locais cobrem a regra de cinco minutos, o limite de três horas, a liberação de dez minutos, relógio alterado, restauração após reboot, expiração, persistência, falha de gravação e seleção do pacote oficial. Eles não substituem a verificação do monitor em um aparelho Android.

As verificações de segurança automatizadas estão detalhadas em [security-testing.md](security-testing.md): testes unitários da política de atualização, Android Lint, validação do manifesto, inventário de dependências e CodeQL no GitHub Actions.

## Landing page

Inicie o servidor local com `node .\landing-page\serve.mjs` e acesse `http://127.0.0.1:4173`. Confira o carregamento de todas as seções, o mockup editável do aparelho, a navegação por capítulos, o botão de demonstração, o menu móvel e o estado `prefers-reduced-motion`. Para o servidor, confirme que métodos diferentes de GET/HEAD são recusados, que caminhos fora de `landing-page/dist/` não são servidos e que as respostas incluem Content-Security-Policy, `X-Content-Type-Options` e `X-Frame-Options`.

## Preparação no aparelho

Use um aparelho ou emulador Android API 26 ou superior. O teste completo de detecção exige o Instagram oficial; imagens de emulador sem Play Store ainda servem para validar instalação, telas, permissões, persistência e notificações. Instale o APK e ative o serviço conforme o README. Para verificar o estado inicial, use uma instalação limpa de teste; não apague os dados de uma instalação pessoal que precise preservar.

Para uma configuração reproduzível no Android Studio, abra **Device Manager**, crie um **Pixel 7** x86_64 com **Android 15 / API 35** e inicie o dispositivo virtual. Escolha uma imagem com Google Play se o cenário exigir o Instagram oficial. A imagem **Google APIs** sem Play Store é suficiente para os demais cenários, mas não comprova a interceptação do pacote real.

Com ADB disponível, confirme o alvo e instale o APK:

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No Android 13 ou superior, conceda notificações quando solicitado. Conceda também acesso aos dados de uso e permissão de sobreposição nas configurações do Android.

Registre a versão do Android, o modelo, a versão do app e o commit testado. Os cenários abaixo são um roteiro a executar, não resultados já confirmados.

| Cenário | Resultado esperado |
| --- | --- |
| Instalação limpa, antes de qualquer tentativa | Cronômetro zerado e contadores zerados. |
| Permissões ausentes | Painel informa que a proteção está desativada; a abertura do Instagram não é bloqueada. |
| Conceder as duas permissões e voltar ao painel | O painel mostra proteção ativa e o monitor inicia em primeiro plano. |
| Negar notificações | O bloqueio continua funcionando; apenas cronômetro e avisos do sistema ficam ocultos. |
| Primeira entrada no Instagram | Mostra a sobreposição de bloqueio, registra uma tentativa e aproximadamente cinco minutos. |
| Abrir o Instagram em entradas sucessivas | A mensagem motivadora é sorteada a cada entrada, permanece estável durante a contagem e não se repete na entrada imediatamente seguinte. |
| Nova entrada após voltar ao launcher | Registra mais uma tentativa e soma cinco minutos ao tempo restante. |
| Eventos repetidos da mesma entrada | Não multiplica a contagem para uma única entrada. |
| Sair do Instagram pela navegação do Android | A sobreposição desaparece quando outro app entra em primeiro plano. |
| Tocar em Voltar para a tela inicial | O launcher abre e a sobreposição desaparece. |
| Abrir outro aplicativo ou Instagram no navegador | O InstBlock não bloqueia esses pacotes. |
| Acumular tentativas até o limite | Tempo restante não supera três horas; o contador continua aumentando. |
| Deixar o cronômetro chegar a zero | A sobreposição fecha e começa uma liberação de dez minutos corridos. |
| Abrir o Instagram durante a liberação | O acesso é permitido e não registra outra tentativa. |
| Deixar o Instagram aberto até o fim dos dez minutos de liberação | A sobreposição reaparece em até um segundo, registra uma tentativa e inicia cinco minutos de bloqueio sem exigir sair e entrar novamente. |
| Terminar a liberação fora do Instagram | O painel volta ao estado pronto; a próxima entrada inicia cinco minutos de bloqueio. |
| Terminar a liberação com a tela apagada ou bloqueada | A sobreposição não aparece sobre a tela bloqueada; se o Instagram ainda estiver aberto ao desbloquear, o bloqueio começa então. |
| Alterar a hora do aparelho durante o bloqueio | O tempo restante não diminui nem aumenta por causa da hora civil. |
| Apagar a tela sem desligar o aparelho | O relógio interno continua avançando e a espera continua normalmente. |
| Reiniciar durante o bloqueio | O restante salvo volta após o boot e nunca fica menor por causa do reinício. |
| Desligar normalmente e ligar depois | A contagem continua do checkpoint feito no desligamento; o período desligado não é descontado. |
| Permitir notificações e iniciar um bloqueio | A notificação mostra o restante e é atualizada durante a espera. |
| Terminar bloqueio e liberação | Recebe avisos de Instagram liberado e de proteção restaurada. |
| Encerrar e reabrir o app | Prazo, contadores e fase do ciclo são recuperados. |
| Reiniciar o aparelho | Dados persistidos permanecem; conferir a conexão do serviço conforme o comportamento do Android no aparelho. |
| Consultar o histórico | Contagem de hoje corresponde à data local; histórico mostra até sete datas com registros. |
| Verificar atualização sem rede | Exibe falha de consulta sem encerrar o app. |
| Verificar atualização com uma versão mais nova disponível | Oferece o download; após baixar, abre o instalador, que exige confirmação. |
| Verificar atualização sem uma versão mais nova | Informa que a versão está atualizada. |
| Abrir um banco durante o bloqueio | A sobreposição desaparece ao sair do Instagram; o banco deve abrir sem tela do InstBlock sobre ele. |
| Banco recusar sobreposições ou acesso ao uso | Registrar banco, aparelho, Android e mensagem exata; a política do banco pode exigir retirar a permissão especial. |

## Compatibilidade de atualização

Atualize uma instalação de teste existente com um APK de assinatura compatível e `versionCode` adequado. Confirme a manutenção do histórico e dos dados de bloqueio. Preferências antigas de pausa deixam de ser usadas. Não desinstale antes desse teste: a desinstalação remove os dados e não demonstra compatibilidade de atualização.

Confira que as novas permissões foram concedidas, que o estado antigo é migrado para o relógio monotônico e que o download usa o mesmo `FileProvider`. O `applicationId`, os contadores e o arquivo `block_state` permanecem preservados.

Uma atualização real exige um asset mais novo e assinatura compatível. Builds debug produzidos por chaves diferentes não são atualizáveis entre si; esse caso exige desinstalação e perde os dados, portanto não serve como teste de migração.

## Verificação antes de uma Release

1. Confirme `versionName`, `versionCode` e o pacote dentro do APK com `aapt2 dump badging`.
2. Execute testes unitários, montagem e lint sem falhas.
3. Execute os cenários manuais aplicáveis e registre aparelho, Android e commit.
4. Calcule e registre o SHA-256 do APK publicado.
5. Anexe o APK à Release; os arquivos automáticos **Source code** não substituem o instalador.
6. Baixe o asset publicado em um aparelho de teste e faça uma instalação limpa e, quando houver assinatura compatível, uma atualização sobre a versão anterior.
