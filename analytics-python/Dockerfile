# Serviço de ETL em Python para o deploy no Render.
# No Render: Root Directory = analytics-python e Language/Runtime = Docker.

# 3.12: versão com "wheel" pronta para pandas, numpy, scipy e psycopg. Em
# versões muito novas do Python o pip tenta COMPILAR esses pacotes e o build
# estoura o tempo/memória do Render.
FROM python:3.12-slim

WORKDIR /app

# Copia só o requirements primeiro para aproveitar o cache de camadas.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Informativo: a porta real vem da variável PORT do Render.
EXPOSE 8000
# Forma "sh -c" porque o ${PORT} precisa ser trocado pelo shell.
# --host 0.0.0.0: sem isso a API só atenderia dentro do contêiner e o Render
# marcaria o deploy como "no open ports".
CMD ["sh", "-c", "uvicorn api:app --host 0.0.0.0 --port ${PORT:-8000}"]
