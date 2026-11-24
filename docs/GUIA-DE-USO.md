# Tripper Lab — guia da versão pessoal

Versão 0.8.0 para o Pixel 8 Pro e o Tripper Pod deste projeto. Configure o aplicativo com a moto parada.

## Atualizar ou instalar

**Em uma cópia recém-clonada**, siga o [README](../README.md) para compilar o APK. O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`. Para instalá-lo em um emulador ou aparelho de teste conectado, execute `adb install -r app/build/outputs/apk/debug/app-debug.apk` na raiz do projeto.

**No Pixel pessoal**, o APK assinado para atualização fica em `artifacts/Tripper-Lab-0.8.0.apk` na máquina de desenvolvimento. Para atualizar o app existente, instale por cima, sem desinstalar nem apagar dados. Isso preserva o Tripper salvo, os registros e as medições. O PIN nunca é salvo.

No celular, abra o APK pelo gerenciador de arquivos e, se o Android pedir, autorize essa origem a instalar aplicativos. Após instalar, essa autorização pode ser desativada novamente. No computador já autorizado para depuração USB, a atualização também pode ser feita com `adb install -r artifacts/Tripper-Lab-0.8.0.apk`.

Este instalador pessoal usa a mesma assinatura de desenvolvimento das versões anteriores. Não é uma publicação na Play Store. Para gerar futuras atualizações compatíveis, preserve a chave local usada pelo projeto; ela não está incluída no APK como chave privada nem deve ser compartilhada. Se o Android indicar assinatura incompatível, não desinstale para contornar: confira a chave utilizada na nova compilação.

## Primeira conexão

1. Abra **Tripper Lab**. Na tela principal, autorize o acesso às notificações e volte ao app.
2. Encerre a conexão do app Royal e deixe o Bluetooth do Pixel ligado.
3. Com a ignição desligada, toque em **Procurar Tripper** e ligue a ignição durante a busca. Autorize dispositivos próximos e notificações se solicitado.
4. Selecione **RE_DISP** e digite o PIN exibido no mostrador diretamente no app.
5. Envie uma seta simulada e confira a seta e a distância no Tripper. Só então toque em **Funcionou no mostrador: salvar Tripper** e confirme.

## Usar o Tripper já salvo

1. Com o Bluetooth ligado, abra o app e confira **Aguardando a ignição**.
2. Ligue a ignição. Quando o Tripper salvo anunciar sua presença por Bluetooth, o app iniciará a conexão. A primeira conexão ainda exige PIN e confirmação no mostrador.
3. Quando aparecer **Tripper conectado**, toque em **Usar Maps no Tripper**.
4. Toque em **Abrir Google Maps** e inicie uma rota. Não é necessário iniciar captura de diagnóstico.
5. Confira o estado **Maps:** na tela principal. Uma instrução completa e reconhecida pode ser enviada ao mostrador.
6. Ao terminar, use **Encerrar sessão** ou **Encerrar** na notificação do Tripper Lab. Fechar a rota retira a seta e mantém a conexão disponível para outra rota.

Você pode abrir o app com a moto já ligada, desde que o Tripper ainda esteja anunciando. A espera funciona nas telas do Tripper Lab e termina ao sair do app. A conexão segue ativa com o Maps aberto ou a tela bloqueada. Encerrar a sessão manualmente pausa a espera até você sair e voltar ao app. Para conectar antes disso, toque em **Reconectar ao Tripper salvo**. A validação física da espera pela ignição e da navegação com tela bloqueada continua pendente.

## Se aparecer espera em vez de seta

Abra a notificação expandida do Maps com a moto parada. O texto pode ser diferente daquele mostrado dentro do Maps.

- **“Head toward” sem distância:** não há uma instrução completa para enviar. O app exibe espera.
- **Dados sem atualização por 30 segundos, recálculo ou texto desconhecido:** também geram espera. Se o Maps apenas omitir a distância ao repetir a mesma instrução, o painel mantém a seta e a última metragem por até oito segundos desde a medição. A próxima instrução completa é enviada imediatamente.
- **Rotatória sem número da saída ou instrução de sair da rotatória:** ainda não é traduzida. Saídas numeradas usam a convenção de circulação pela direita, como no Brasil.
- **Chegada:** “Arriving” significa aproximação; não é confirmação de chegada. A remoção da notificação limpa a orientação e o Bluetooth continua conectado.

Ao encerrar, o app tenta substituir a seta por espera antes de desconectar. A volta imediata ao relógio não é garantida.

## Se desconectar ou não ativar o Maps

Confira a mensagem na tela principal, ignição, proximidade, Bluetooth do Pixel e conexão do app Royal. Após uma queda, o app tenta reconectar ao mostrador salvo e retomar o Maps. Se a tentativa falhar, use **Reconectar ao Tripper salvo**. Se a falha persistir, abra **Mostrar ferramentas e diagnóstico → Esquecer Tripper / pedir novo PIN** e refaça a primeira conexão. Isso apaga o dispositivo salvo e encerra a sessão atual.

Se o Maps não ativar, confira o acesso às notificações e se a exibição do primeiro pareamento foi confirmada no mostrador. Não é necessário confirmar uma simulação a cada reconexão ao Tripper já salvo.

## Ferramentas e privacidade

**Como usar e resolver problemas** abre este tipo de orientação dentro do app, sem internet.

**Mostrar ferramentas e diagnóstico** reúne:

- **Diagnóstico do Maps:** captura opcional de até 20 minutos; exportação e exclusão do registro. Pode conter ruas, destinos, textos e ícones. **Parar captura** não encerra a sessão Bluetooth. Uma captura ativa é sinalizada na tela principal.
- **Medir bateria:** duas sessões semelhantes de 20–25 minutos, sem USB/carregador e sem captura de diagnóstico. Veja [o roteiro de medição](TESTE-ETAPA-6.md).
- **Registro do teste, diagnóstico do aparelho e novo PIN:** recursos para investigar problemas.

As simulações ficam em **Testar símbolos com a moto parada**, disponível depois de conectar e antes de ativar o Maps. Em um novo pareamento, os controles aparecem automaticamente para a confirmação inicial. Durante o Maps, não há envio de simulações.

O app não solicita internet nem localização. O envio normal não salva ruas ou destinos; o PIN não é salvo. Os registros e medições ficam no aparelho. Exportação e cópia são ações manuais; apagar um registro no app não apaga cópias exportadas. O acesso às notificações pode ser revogado nas configurações do Android.

## O que ainda não foi validado

A entrega da interface e do instalador não transforma testes adiados em resultados aprovados. Permanecem pendentes a confirmação física inequívoca de conversões reais no fluxo integrado, o bloqueio da tela durante a integração, o encerramento visual e alguns símbolos; a comparação real de bateria também foi adiada. Não há economia comprovada, nem garantia de funcionamento em outros mostradores.

Histórico: [etapa 5](VALIDACAO-ETAPA-5.md), [etapa 6](VALIDACAO-ETAPA-6.md) e [entrega da etapa 7](VALIDACAO-ETAPA-7.md).
