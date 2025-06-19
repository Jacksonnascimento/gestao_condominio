package br.com.gestaocondominio.api.controller.dto;


public record VotacaoResultadoDTO(
    long votosSim,
    long votosNao,
    long votosAbstencao,
    long totalDeVotos
) {}