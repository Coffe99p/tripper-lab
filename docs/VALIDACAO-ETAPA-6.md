# Etapa 6 — falhas e medição de bateria

Entrega em 12/11/2026, versão **0.5.0** (`versionCode=8`). Melhorias de software implementadas, verificadas e instaladas no Pixel. **A comparação real de consumo está pendente: o usuário escolheu deixar as duas coletas para depois.** A etapa 7 não foi iniciada. O aceite parcial da etapa 5 permanece válido, sem converter seus testes pendentes em aprovações.

## Tratamento de falhas e trabalho em segundo plano

- Desligar o Bluetooth durante uma sessão passa a encerrá-la explicitamente. O app cancela tarefas, descarta telas pendentes, fecha cliente/servidor Bluetooth, encerra o envio do Maps, libera o wakelock e remove a notificação da sessão.
- A mensagem para o erro 147 orienta reconexão e eventual teste com novo PIN se a falha repetir perto da moto. A causa da falha observada na etapa 5 continua indeterminada; não há alegação de correção do problema físico.
- O motivo do último encerramento fica disponível ao reabrir a tela. Se o processo termina durante uma sessão, a próxima abertura informa que houve interrupção e exige novo início pelo usuário. Não há reconexão automática ou repetição infinita de tentativas.
- Duração escolhida antes de iniciar: 10 minutos por padrão ou 30 minutos para permitir uma coleta de consumo de 20–25 minutos. Alterações durante a sessão ficam desabilitadas. Pedidos de duração não reconhecidos usam 10 minutos.
- Prazo fixo por relógio monotônico, incluindo suspensão, verificado também antes de novos envios e na manutenção da conexão. Ativar Maps ou receber instruções não renova o prazo. Wakelock permanece limitado à duração selecionada mais 5 segundos.
- Gravações do log agrupadas em intervalos de até 5 segundos; saída normal e falha forçam a gravação final. Respostas repetitivas do Tripper entram no log na primeira ocorrência e a cada 30 respostas, mantendo a contagem total. Nenhum payload de resposta ou PIN é salvo. Uma morte abrupta do processo pode perder os últimos segundos de log.

Essas mudanças reduzem trabalho de diagnóstico e melhoram o encerramento; **não demonstram economia de bateria**. A frequência de manutenção exigida pelo mostrador foi preservada.

## Medidor

Nova tela **Medir bateria** na página inicial. Dois cenários declarados pelo usuário: Maps sem Tripper e Maps com Tripper. O mesmo medidor participa de ambos; portanto, a referência não é uma medição laboratorial do Maps isolado.

- Leituras do celular inteiro: porcentagem, contador de carga quando suportado, temperatura, alimentação externa e duração com tela acesa. Usa início/fim e broadcasts do sistema; não cria serviço foreground, timer de sondagem ou wakelock de medição.
- Medição sem Tripper exige sessão Bluetooth do app encerrada. Medição com Tripper exige envio do Maps ativo; seu encerramento finaliza a amostra naquele instante. Iniciar sessão Bluetooth durante o cenário sem Tripper invalida essa amostra.
- Captura textual/visual de diagnóstico deve permanecer desligada. Ativá-la durante a coleta invalida e encerra a medição.
- Início bloqueado com USB/carregador conectado ou alimentação desconhecida. Carregamento observado no meio, aumento do contador/porcentagem ou relógio inconsistente invalidam a comparação.
- Menos de 20 ou mais de 60 minutos não gera taxa comparável. Ausência de variação detectável é descrita como resolução insuficiente, não consumo zero. Ausência do contador não vira carga zero; nesse caso, fica disponível apenas a variação percentual, quando existente.
- A variação em mAh é calculada pela diferença do contador em µAh dividida por 1.000. A taxa em mAh/h só é apresentada quando a amostra passa pelos critérios e tem variação positiva. É taxa do celular inteiro, sem atribuição exclusiva ao Tripper Lab.
- Se o processo reiniciar, uma coleta pendente é marcada como interrompida, sem completar o intervalo com dados inventados. Histórico de até 20 resultados no armazenamento privado excluído de backup, com exportação manual em JSON.
- O medidor não verifica trajeto, tráfego, sinal, brilho, outros apps nem se o Maps estava navegando. Comparabilidade depende também de condições semelhantes e repetição dos cenários. Não calcula automaticamente uma porcentagem de economia.

Referências utilizadas: [BatteryManager: propriedades e valores indisponíveis](https://developer.android.com/reference/android/os/BatteryManager), [SystemClock: relógio incluindo suspensão](https://developer.android.com/reference/android/os/SystemClock), [receivers de broadcasts do Android](https://developer.android.com/develop/background-work/background-tasks/broadcasts).

## Verificações concluídas

- Compilação, lint e **50 testes unitários** aprovados: 11 da medição de bateria, 3 de prazo de sessão e os 36 testes anteriores.
- Casos novos: carga em mAh/taxa horária, carregador no meio do intervalo, contador indisponível, resolução insuficiente, limites de duração, tela ligada/desligada, aumento de carga, dados ausentes, interrupções e relógio regressivo. Prazo padrão, pedido inválido e escolha explícita de 30 minutos verificados.
- APK assinado e verificado; atualização instalada no Pixel 8 Pro / Android 17. Versão 0.5.0 confirmada. A tela interna de bateria é não exportada: foi aberta pelo botão da tela inicial, como no uso normal.
- Tela de bateria inspecionada visualmente e por hierarquia de interface. Leitura pontual durante a conferência: 59%, 28,1 °C e alimentação por USB. **Essa leitura não é uma medição de consumo.**
- Ao tocar em iniciar a medição com USB conectado, apareceu a orientação para desconectar o carregador; a tela permaneceu sem medição ativa e sem resultados. Evidências: `artifacts/tripper-stage6-usb.png` e `artifacts/tripper-stage6-usb.xml`.
- Opções de 10/30 minutos verificadas na interface. Foi iniciada somente uma busca BLE, sem selecionar nem conectar dispositivo, com limite de 30 minutos registrado às 19:30:25; encerrada manualmente às 19:30:43. O serviço ficou sem pedido de execução (`startRequested=false`) e deixou de aparecer após sair da tela. Registro: `artifacts/tripper-stage6-scan-stop.txt`.
- Consulta de AndroidRuntime restrita ao processo do Tripper Lab não retornou erros nessa conferência. Nenhuma sessão Bluetooth, captura de diagnóstico ou medição foi deixada ativa.
- APK: `artifacts/Tripper-Lab-0.5.0.apk`. SHA-256: `9677BDE6F4788F403969C06B996C7230F82554BC483B714E89D0C9B0A555200F`.

## Pendências aceitas para coleta posterior

O usuário escolheu **deixar a coleta de consumo para depois**. Não existem ainda duas amostras reais comparáveis nem resultado de economia. A saída por Bluetooth desligado durante uma conexão real, sessões de 30 minutos no mostrador, interrupção do medidor pelo Android e exportação das novas medições ainda precisam de conferência completa no aparelho; cálculo/política têm cobertura automatizada, sem equivaler a esses testes físicos.

Roteiro: [TESTE-ETAPA-6.md](TESTE-ETAPA-6.md). Encerrada a entrega de software desta etapa, mantendo a coleta e limitações registradas. Parar antes da etapa 7.
