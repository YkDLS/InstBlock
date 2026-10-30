# Verificação do InstBlock

## Verificações automatizadas

Execute os três comandos Gradle descritos no [README](../README.md#compilar-e-verificar): `:app:testDebugUnitTest`, `:app:assembleDebug` e `:app:lintDebug`. O GitHub Actions também os executa em pushes e pull requests.

Os testes locais cobrem a regra de cinco minutos, o limite de três horas, a expiração, o armazenamento por um contrato de persistência, a falha de gravação e a seleção do pacote oficial. Eles não substituem os testes do serviço de acessibilidade no Android.

## Preparação no aparelho

Use um aparelho ou emulador Android API 26 ou superior com o Instagram oficial instalado. Instale o APK e ative o serviço conforme o README. Para verificar o estado inicial, use uma instalação limpa de teste; não apague os dados de uma instalação pessoal que precise preservar.

Registre a versão do Android, o modelo, a versão do app e o commit testado. Os cenários abaixo são um roteiro a executar, não resultados já confirmados.

| Cenário | Resultado esperado |
| --- | --- |
| Instalação limpa, antes de qualquer tentativa | Cronômetro zerado e contadores zerados. |
| Serviço desativado | Painel informa que a acessibilidade está desativada; a abertura do Instagram não é bloqueada pelo InstBlock. |
| Ativar o serviço e voltar ao painel | O painel reflete a conexão do serviço; pode mostrar espera antes da conexão. |
| Primeira entrada no Instagram | Abre a tela de bloqueio, registra uma tentativa e aproximadamente cinco minutos. |
| Nova entrada após voltar ao launcher | Registra mais uma tentativa e soma cinco minutos ao tempo restante. |
| Eventos repetidos da mesma entrada | Não multiplica a contagem para uma única entrada. |
| Voltar da tela de bloqueio | Retorna à tela inicial do Android. |
| Abrir outro aplicativo ou Instagram no navegador | O InstBlock não bloqueia esses pacotes. |
| Acumular tentativas até o limite | Tempo restante não supera três horas; o contador continua aumentando. |
| Deixar o cronômetro chegar a zero | O tempo zera; uma nova entrada no Instagram ainda é bloqueada e inicia cinco minutos. |
| Pausar com tempo restante | O painel mostra a pausa e congela o cronômetro; o Instagram pode ser usado. |
| Retomar depois de aguardar em pausa | O cronômetro continua do tempo congelado e as novas entradas voltam a ser bloqueadas. |
| Encerrar e reabrir o app | Prazo, contadores e estado de pausa são recuperados. |
| Reiniciar o aparelho | Dados persistidos permanecem; conferir a conexão do serviço conforme o comportamento do Android no aparelho. |
| Consultar o histórico | Contagem de hoje corresponde à data local; histórico mostra até sete datas com registros. |
| Verificar atualização sem rede | Exibe falha de consulta sem encerrar o app. |
| Verificar atualização com uma versão mais nova disponível | Oferece o download; após baixar, abre o instalador, que exige confirmação. |
| Verificar atualização sem uma versão mais nova | Informa que a versão está atualizada. |

## Compatibilidade após reorganização

Atualize uma instalação de teste existente com um APK de assinatura compatível e `versionCode` adequado. Confirme a manutenção do histórico, da pausa e dos dados de bloqueio. Não desinstale antes desse teste: a desinstalação remove os dados e não demonstra compatibilidade de atualização.

Confira também que o serviço continua disponível nas configurações de acessibilidade e que o download consegue usar o mesmo `FileProvider`. A reorganização preserva `applicationId`, caminhos das Activities e do serviço, nomes das preferências e chaves persistidas.

Uma atualização real exige um asset mais novo e assinatura compatível. Não publique uma Release apenas para executar o roteiro sem uma decisão explícita do mantenedor.
