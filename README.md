# Tripper Lab

O Tripper Lab é um aplicativo Android independente que leva as indicações de navegação do Google Maps ao Tripper Pod da Royal Enfield Meteor 350. Durante uma rota, o app lê a notificação de navegação no celular, interpreta a manobra e a distância e envia a indicação ao mostrador por Bluetooth Low Energy (BLE).

**Versão pessoal 0.6.0** (`versionCode 9`), instalada em um Pixel 8 Pro com Android 17 em 19/11/2026. O pareamento, as setas simuladas e a reconexão ao Tripper salvo já foram confirmados no equipamento. A exibição de uma conversão real do Maps com distância ainda precisa de confirmação em trajeto. [Validação desta versão](docs/VALIDACAO-ETAPA-7.md).

## Clonar e preparar o ambiente

Para compilar, tenha Git, Android Studio com JDK 17 ou superior, Android SDK Platform 36 e Android SDK Build-Tools 36.0.0. O Gradle Wrapper do projeto usa Gradle 9.3.1 e o Android Gradle Plugin é o 9.1.0. A primeira compilação precisa de acesso à internet para baixar as dependências. Você pode compilar e executar os testes unitários sem conectar o celular ou a moto.

1. No GitHub, copie a URL HTTPS em **Code**. Em um terminal PowerShell, na pasta onde deseja guardar o projeto, execute os comandos abaixo. Quem já tem o código local pode começar pelo passo 2.

   ```powershell
   $url = Read-Host "Cole a URL HTTPS do repositório"
   git clone $url tripper-lab
   Set-Location tripper-lab
   ```

2. No Android Studio, abra a pasta que contém `settings.gradle.kts`. No **SDK Manager**, confira os componentes indicados acima. Para instalar pelo terminal e usar `adb`, instale também **Android SDK Platform-Tools** e inclua a pasta `platform-tools` no `PATH`.
3. Aguarde a sincronização do Gradle. Se precisar configurar o SDK manualmente, copie `local.properties.example` para `local.properties` na raiz e ajuste `sdk.dir` para o diretório do SDK mostrado no SDK Manager. O arquivo de exemplo usa um caminho fictício. [Modelo de configuração](local.properties.example).

## Gerar e atualizar o instalador

No Windows, execute na raiz do projeto:

```powershell
.\scripts\build.ps1
```

O script usa o JDK de `JAVA_HOME` ou procura o Java incluído na instalação padrão do Android Studio. Ele gera o APK de depuração, executa os testes JUnit e roda a análise estática do Android (lint). No Linux ou macOS, configure `JAVA_HOME` e execute as mesmas tarefas com o wrapper:

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

| Resultado | Onde conferir após a compilação |
|---|---|
| Instalador de desenvolvimento | `app/build/outputs/apk/debug/app-debug.apk` |
| Relatório dos testes unitários | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Relatório do lint | `app/build/reports/lint-results-debug.html` |

A validação da versão 0.6.0 registrou **50 testes aprovados** e lint sem ocorrências. Para repetir apenas os testes durante uma alteração, execute `.\gradlew.bat :app:testDebugUnitTest` no PowerShell com o JDK configurado.

## Executar e conferir a instalação

1. Use um aparelho ou emulador com **Android 12 (API 31) ou superior**. Para o teste via USB, ative a depuração USB no celular e aceite a autorização do computador.
2. No Android Studio, selecione o módulo **app**, escolha o dispositivo e clique em **Run**. Para instalar pelo terminal, use os comandos abaixo com um único dispositivo listado por `adb devices` no estado `device`.

   ```powershell
   adb devices
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Abra **Tripper Lab** pelo ícone. Confira o estado da conexão e do acesso às notificações na tela inicial. Toque em **Como usar e resolver problemas**, volte e abra **Mostrar ferramentas e diagnóstico**. Essa conferência da interface pode ser feita sem o Tripper.
4. Para testar a navegação no equipamento, prepare o celular com Google Maps, Bluetooth e acesso às notificações. Com a moto parada, procure o Tripper, digite o PIN exibido no mostrador e confirme uma seta simulada antes de ativar **Usar Maps no Tripper**. [Guia do primeiro pareamento](docs/GUIA-DE-USO.md).

**Atualização do aparelho pessoal.** O Android exige a mesma chave de assinatura para atualizar a instalação existente preservando os dados. Se aparecer `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, use um aparelho de teste ou emulador para o novo APK e preserve a instalação do Pixel. O instalador pessoal original fica em `artifacts/Tripper-Lab-0.6.0.apk` na máquina de desenvolvimento. Seu SHA-256 é `BE4ACCB865A7551309D896535DE7FE0B551740D24A3613E3217186A054449817`.

## Onde trabalhar no projeto

Para acompanhar uma instrução do Maps até o mostrador, localizar os dados no aparelho e escolher os testes de uma mudança, consulte o [guia de desenvolvimento](docs/DESENVOLVIMENTO.md).

O projeto tem um único módulo Android, `app`. Os nomes de classes na tabela correspondem aos arquivos Kotlin no pacote `com.joaofabijack.tripper`.

| Local | O que você encontra |
|---|---|
| [Código Kotlin](app/src/main/java/com/joaofabijack/tripper) | `MainActivity` abre a interface. `MapsNotificationListener` recebe notificações, `MapsInstructionParser` interpreta manobras e `MapsRouteState` acompanha a rota. `TripperService` mantém a conexão BLE e `TripperProtocol` monta as mensagens do mostrador. |
| [Recursos da interface](app/src/main/res) | `layout/` contém as telas em XML. Em `values/` ficam textos, cores e temas. |
| [Manifesto Android](app/src/main/AndroidManifest.xml) | Declara a atividade inicial, os serviços de Bluetooth e de leitura de notificações e as permissões do app. |
| [Testes unitários](app/src/test/java/com/joaofabijack/tripper) | Seis suítes cobrem interpretação e estado da rota, protocolo, prazo da sessão, captura de diagnóstico e medição de bateria. |
| [Configuração do módulo](app/build.gradle.kts) | Define versão do app, níveis de API, dependências e configuração do lint. |
| [Gradle Wrapper](gradle/wrapper) e [script de compilação](scripts/build.ps1) | Fixam a versão do Gradle e reúnem os comandos de verificação usados no Windows. |
| [Documentação](docs) | Guia de uso, protocolo Bluetooth, roteiros de teste e registros das validações físicas. |

Para alterar o reconhecimento de uma instrução do Maps, comece por `MapsInstructionParser.kt` e `MapsInstructionParserTest.kt`. Para investigar uma seta interpretada corretamente que aparece diferente no mostrador, consulte `TripperProtocol.kt`, os testes de protocolo e a [descrição das mensagens Bluetooth](docs/PROTOCOLO.md).

## Usar o Tripper salvo

1. Com a moto parada, ligue a ignição e o Bluetooth do Pixel.
2. Abra **Tripper Lab** e confira o acesso às notificações.
3. Escolha a duração de 10 ou 30 minutos e toque em **Reconectar ao Tripper salvo**.
4. Toque em **Usar Maps no Tripper**, depois em **Abrir Google Maps**, e inicie uma rota.
5. Para parar, use **Encerrar sessão** na tela principal ou **Encerrar** na notificação.

Não é necessário iniciar captura de diagnóstico. No primeiro pareamento, procure o Tripper, informe o PIN e confirme uma simulação no mostrador antes de ativar o Maps. Veja o [passo a passo completo](docs/GUIA-DE-USO.md).

O prazo conta desde o início da busca ou reconexão e não é renovado por instruções. Não há reconexão automática. Nenhuma sessão retoma sozinha após reinício do processo.

## Comportamento e limitações

O app envia manobras com distância completa e reconhecida. **“Head toward” sem distância**, dados desconhecidos ou sem atualização por 30 segundos mostram espera/recálculo. A distância anterior não é reutilizada. Saídas numeradas de rotatória seguem a circulação pela direita, como no Brasil; rotatórias sem número e instrução de sair da rotatória não são traduzidas.

Remoção da notificação da rota, chegada explícita ou perda do acesso às notificações encerram a integração. “Arriving” indica aproximação. O app tenta substituir a seta por espera antes de desconectar; não promete volta imediata ao relógio.

As simulações da etapa 3 foram confirmadas pelo usuário, incluindo tela bloqueada e reconexão sem novo PIN. A integração Maps → Tripper foi entregue com **validação parcial aceita**: continuam pendentes a confirmação física inequívoca de conversões reais, bloqueio da tela durante a integração, encerramento visual e alguns símbolos. A desconexão com erro 147 observada em 12/11 não teve sua causa comprovada. A finalização da interface não transforma essas pendências em aprovações.

O medidor compara Maps sem Tripper e Maps com Tripper em sessões semelhantes de 20–25 minutos, sem carregador e sem captura de diagnóstico. A coleta real foi adiada pelo usuário. **Não há consumo medido nem economia comprovada**, inclusive em relação ao app Royal. [Roteiro da coleta](docs/TESTE-ETAPA-6.md).

## Dados e permissões

O app solicita dispositivos próximos e notificações. A conexão usa um serviço com notificação e botão para encerrar, limitado a 10 ou 30 minutos. Não solicita internet nem localização. O PIN e o conteúdo das respostas Bluetooth não são salvos.

O envio normal do Maps não grava ruas ou destinos. A captura opcional registra textos e ícones por até 20 minutos no armazenamento privado excluído de backup; pode conter dados do trajeto. Exportação e cópia são manuais. Parar captura não encerra a sessão Bluetooth. Medições ficam no aparelho, com histórico e exportação próprios.

## Histórico do planejamento

Consulte [o planejamento](docs/PLANEJAMENTO.md). A implementação planejada foi entregue, mantendo separadas as pendências físicas e de consumo:

- Etapa 1: comparação com o app Royal pulada a pedido do usuário.
- [Etapa 2](docs/VALIDACAO-ETAPA-2.md): projeto e instalação inicial.
- [Etapa 3](docs/VALIDACAO-ETAPA-3.md): comunicação e simulações confirmadas no mostrador.
- [Etapa 4](docs/VALIDACAO-ETAPA-4.md): inspeção dos formatos reais de notificações coletados em inglês; [análise do trajeto](docs/ANALISE-TRAJETO-2026-11-10.md).
- [Etapa 5](docs/VALIDACAO-ETAPA-5.md): integração entregue com validação parcial aceita.
- [Etapa 6](docs/VALIDACAO-ETAPA-6.md): tratamento de falhas e medidor; coleta adiada.
- [Etapa 7](docs/VALIDACAO-ETAPA-7.md): interface, instalador e guia da versão pessoal.

Instaladores e registros anteriores foram preservados. Detalhes da comunicação: [protocolo](docs/PROTOCOLO.md).
