# Teste físico da etapa 3

Use a moto parada e o motor desligado. Basta ligar a ignição para alimentar o Tripper. Deixe o celular próximo ao mostrador.

1. Encerre a conexão do app Royal para que ele não dispute o Tripper.
2. Abra **Tripper Lab**, toque em **Procurar Tripper** e autorize dispositivos próximos. Autorize notificações para ver o botão de encerramento com o celular bloqueado.
3. Durante os 20 segundos de busca, desligue e ligue a ignição. Selecione o `RE_DISP` encontrado. Se nada aparecer, aguarde alguns segundos antes de repetir a busca.
4. Digite no app o PIN exibido no Tripper e toque em **Enviar PIN**. Esse PIN não é guardado.
5. Quando aparecer **Link pronto para teste**, envie **Direita • 200 m**. Confira o mostrador, pois o contador de envios não garante que ele exibiu o comando.
6. Teste também **Esquerda • 50 m**, **Em frente • 999 m** e **Em frente • 1,5 km**. Verifique que esquerda e direita não estão trocadas. A rotatória é um teste adicional experimental.
7. Se a seta e a distância aparecerem corretamente, toque em **Funcionou no mostrador: salvar Tripper** e confirme. Isso habilita a reconexão manual ao mesmo aparelho sem repetir o PIN, se o firmware aceitar.
8. Com uma seta ativa, bloqueie a tela do Pixel por pelo menos 60 segundos. Confira se o Tripper continua exibindo o comando. Desbloqueie e envie uma manobra diferente para conferir a atualização.
9. Toque em **Encerrar teste**. A notificação deve desaparecer. Para testar novamente, pode ser necessário desligar e ligar a ignição e tocar em **Reconectar ao Tripper salvo**.

A sessão termina sozinha em 10 minutos. O app não reinicia o teste sozinho após uma falha ou após ser removido dos recentes.

Se falhar, use **Ver registro do teste** e **Copiar diagnóstico**, anotando também o que apareceu no Tripper. Não inclua o PIN no relato. Não use esta versão para navegação: todas as orientações são simuladas.

## Critério de conclusão

- [x] Tripper físico encontrado e selecionado (relato do usuário em 03/11/2026).
- [x] Comunicação e primeira instrução funcionando no mostrador (relato do usuário; PIN não coletado).
- [x] Setas esquerda, direita e em frente e distâncias m/km exibidas corretamente (relato do usuário).
- [x] Tela bloqueada por 60 segundos sem queda, seguida por atualização visível (relato do usuário).
- [x] Encerramento libera a conexão e remove a notificação (fluxo confirmado pelo usuário).
- [x] Reconexão manual ao Tripper salvo e novo envio, sem repetir PIN (relato do usuário).

Critérios da etapa 3 atendidos em 03/11/2026. Consulte [VALIDACAO-ETAPA-3.md](VALIDACAO-ETAPA-3.md) para a separação entre verificações automáticas e testes relatados pelo usuário.

A etapa 4 (Maps) permanece fora do escopo até nova solicitação.
