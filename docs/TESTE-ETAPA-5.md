# Etapa 5 — Maps no Tripper

Em 12/11/2026, o usuário aceitou a entrega com validação parcial e adiou o teste em trajeto. Este roteiro permanece para eventual retomada; sua conclusão não bloqueia a etapa 6. Não considerar os cenários pendentes como aprovados.

Versão 0.4.0. Primeiro teste com a moto parada. O acesso às notificações já autorizado na etapa 4 é reaproveitado. Não é necessário iniciar captura de diagnóstico nem exportar textos para usar a integração.

## Conectar e conferir espera

1. Sem rota ativa no Maps, abra **Conectar Tripper e usar Maps**.
2. Ligue a ignição e toque em **Reconectar ao Tripper salvo**. Se necessário, desligue e ligue a ignição para uma nova tentativa.
3. Toque em **Usar Maps no Tripper**. Antes de uma instrução completa, o mostrador deve apresentar o símbolo de espera/recálculo, sem seta antiga. Confira fisicamente: um envio aceito pelo Android não confirma essa exibição.
4. Se o dispositivo ainda não estiver salvo, primeiro envie uma simulação e confirme a exibição pelo botão **Funcionou no mostrador: salvar Tripper**. Um pareamento novo continua exigindo o PIN.

## Instrução real e tela bloqueada

1. Abra o Maps e inicie uma rota que mostre uma conversão e distância. Compare a manobra e a distância do Maps com o Tripper. A instrução deve vir do Maps sem apertar o botão de simulação.
2. Se o Maps mostrar somente `Head toward...`, direção sem distância ou um formato sem correspondência, o app mostrará espera; não deve inventar uma orientação.
   Em 12/11, foi confirmado que o teste parado permanece em `Head toward [via]`, sem distância. Nesse caso, repetir a mesma rota sem deslocamento não comprova o envio de uma conversão. Quando houver disponibilidade, configurar antes de sair e observar uma etapa seguinte de um trajeto curto; conferir ou ajustar o celular somente após parar. A sessão tem limite de 10 minutos e não exige USB durante o percurso.
3. Bloqueie o Pixel por 60 segundos. O mostrador deve continuar recebendo instruções enquanto o Maps as publicar. Se não houver nova publicação por 30 segundos, o símbolo de espera substitui a instrução antiga; isso não significa necessariamente falha de Bluetooth.
4. Desbloqueie e confira o estado do app. O limite total da sessão é de 10 minutos desde sua abertura, não desde o início da rota.

## Encerrar

1. Cancele a rota no Maps. Quando a notificação atual for removida, o app deve substituir a orientação por espera, encerrar o Bluetooth e remover sua notificação de sessão.
2. A volta direta ao relógio nativo não é prometida. A sessão não reconecta sozinha; pode ser necessário desligar/ligar a ignição para novo teste.
3. Em outra sessão, **Encerrar teste** ou **Encerrar** na notificação deve interromper o envio mesmo que o Maps continue navegando. Novas publicações não devem reativar a sessão.

## Símbolos adicionais e limites

As simulações incluem curvas suaves, manter à esquerda/direita e entrada em via para comparação visual. A rotatória de segunda saída continua disponível para teste. O leitor envia saídas numeradas de 1 a 9 usando a convenção de circulação pela direita (Brasil); cada variante ainda não conferida no mostrador deve ser tratada como pendência de cobertura física.

Rotatória sem número de saída e instrução isolada de sair da rotatória usam espera nesta versão, pois não há correspondência inequívoca implementada. `Arriving` significa aproximação, sem inventar distância zero; chegada explícita ou remoção da notificação encerram a sessão. Se o acesso às notificações cair, a sessão é encerrada. Em falha de Bluetooth, o app pode não conseguir substituir a última imagem; não afirma confirmação física nem tenta reconectar automaticamente.

## Critério de conclusão

Confirmar espera sem seta antiga, transmissão de uma instrução real com distância, comportamento com tela bloqueada e encerramento da rota/sessão. Registrar exatamente quais símbolos foram vistos e quais continuam sem teste físico. Medição de consumo, reconexão automática, viagens longas e tratamento ampliado de falhas ficam para a etapa 6.
