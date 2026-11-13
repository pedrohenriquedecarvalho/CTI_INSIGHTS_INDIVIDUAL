"""Banco: GRAVA no PostgreSQL (Azure) os clientes, contratos e o JSON do dashboard. As linhas chegam do backend."""
from __future__ import annotations

import json
import logging
import re
from datetime import datetime, timezone

import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

from config import database_url

log = logging.getLogger("analytics")


def criar_engine() -> Engine:
    return create_engine(database_url(), pool_pre_ping=True)


def agora_utc() -> datetime:
    """O Spring Boot grava TIMESTAMP em UTC (sem fuso); fazemos o mesmo."""
    return datetime.now(timezone.utc).replace(tzinfo=None)


# ------------------------------------------------------------------- gravação
def criar_lote(engine: Engine, arquivo_nome: str, recebidas: int,
               invalidas: int, validas: int, com_aviso: int) -> int:
    """Cria a linha em upload_lote com status PROCESSANDO e devolve o id novo."""
    with engine.begin() as conn:
        return conn.execute(
            text("INSERT INTO upload_lote (arquivo_nome, enviado_em, status, total_linhas, "
                 "linhas_validas, linhas_invalidas, linhas_com_aviso, mensagem) "
                 "VALUES (:a, :e, 'PROCESSANDO', :t, :v, :i, :c, 'ETL em andamento...') RETURNING id"),
            {"a": arquivo_nome[:255], "e": agora_utc(), "t": recebidas,
             "v": validas, "i": invalidas, "c": com_aviso}).scalar_one()


def marcar_erro(engine: Engine, lote_id: int, mensagem: str) -> None:
    with engine.begin() as conn:
        conn.execute(text("UPDATE upload_lote SET status = 'ERRO', mensagem = :m WHERE id = :id"),
                     {"id": lote_id, "m": mensagem[:480]})


# ------------------------------------------------------------------------ carga
def gravar_resultado(engine: Engine, lote_id: int, tratado: pd.DataFrame, payload: dict, rejeitadas_etl: int) -> None:
    """Grava tudo em uma única transação: ou o lote inteiro é processado, ou nada muda."""
    with engine.begin() as conn:
        # reprocessar o mesmo lote substitui o resultado anterior
        conn.execute(text("DELETE FROM contrato WHERE id_cliente IN "
                          "(SELECT id_cliente FROM cliente WHERE lote_id = :l)"), {"l": lote_id})
        conn.execute(text("DELETE FROM cliente WHERE lote_id = :l"), {"l": lote_id})
        conn.execute(text("DELETE FROM estatistica_lote WHERE lote_id = :l"), {"l": lote_id})

        consultores: dict[str, int] = {}
        servicos: dict[str, int] = {}

        def id_consultor(nome: str) -> int:
            """Acha o consultor pelo nome ou cria um novo (sem possibilidade de login)."""
            if nome not in consultores:
                achado = conn.execute(text("SELECT id_consultor FROM consultor WHERE nome = :n"),
                                      {"n": nome}).scalar()
                if achado is None:
                    slug = re.sub(r"[^a-z0-9]+", ".", nome.lower()).strip(".") or "consultor"
                    achado = conn.execute(
                        text("INSERT INTO consultor (nome, email, senha, telefone) "
                             "VALUES (:n, :e, '!sem-login', '') RETURNING id_consultor"),
                        {"n": nome, "e": f"{slug}@cti.local"}).scalar_one()
                consultores[nome] = achado
            return consultores[nome]

        def id_servico(nome: str) -> int:
            """Acha o serviço pelo nome ou cria um novo."""
            if nome not in servicos:
                achado = conn.execute(text("SELECT id_servico FROM servico WHERE nome = :n"),
                                      {"n": nome}).scalar()
                if achado is None:
                    achado = conn.execute(
                        text("INSERT INTO servico (nome, categoria) VALUES (:n, 'Geral') RETURNING id_servico"),
                        {"n": nome}).scalar_one()
                servicos[nome] = achado
            return servicos[nome]

        for r in tratado.itertuples(index=False):
            cliente_id = conn.execute(text("""
                INSERT INTO cliente (lote_id, nome_empresa, id_consultor, segmento, nivel, faturamento_anual, status)
                VALUES (:lote, :nome, :consultor, :segmento, :nivel, :fat, 'ATIVO')
                RETURNING id_cliente
            """), {
                "lote": lote_id,
                "nome": r.nome_cliente[:255],
                "consultor": id_consultor(r.consultor[:120]),
                "segmento": r.segmento[:40],
                "nivel": r.nivel_cliente,
                "fat": round(float(r.faturamento_anual), 2),
            }).scalar_one()
            for nome_servico in r.servicos:
                conn.execute(text("""
                    INSERT INTO contrato (id_cliente, id_servico, data_inicio, ativo)
                    VALUES (:c, :s, :d, true)
                """), {"c": cliente_id,
                       "s": id_servico(nome_servico[:120]),
                       "d": r.data_contratacao.date()})

        conn.execute(text("INSERT INTO estatistica_lote (lote_id, gerado_em, resultado) VALUES (:l, :g, :r)"),
                     {"l": lote_id, "g": agora_utc(), "r": json.dumps(payload, ensure_ascii=False)})

        mensagem = f"ETL concluído: {len(tratado)} clientes tratados"
        if rejeitadas_etl:
            mensagem += f", {rejeitadas_etl} linhas recusadas na limpeza"
        conn.execute(text("UPDATE upload_lote SET status = 'PROCESSADO', mensagem = :m WHERE id = :id"),
                     {"id": lote_id, "m": mensagem + "."})
    log.info("Lote %s gravado: %s clientes, %s consultores, %s serviços",
             lote_id, len(tratado), len(consultores), len(servicos))