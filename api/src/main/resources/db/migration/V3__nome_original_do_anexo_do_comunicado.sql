-- Nome com que o anexo do comunicado foi enviado, para o download sair com ele. O arquivo continua guardado com um
-- nome gerado (COM_CAMINHO_ANEXO); nos comunicados antigos, sem o nome original, o download usa esse nome gerado.

ALTER TABLE gc_comunicado ADD COLUMN com_nome_anexo VARCHAR(255);
