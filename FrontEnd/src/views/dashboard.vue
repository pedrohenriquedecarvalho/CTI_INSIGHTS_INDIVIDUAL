<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import NavSidebar from '@/components/sidebar.vue'
import { dashboardService, mensagemDeErro } from '../Service/api'

// Caminho do dado: Python grava no banco -> Spring GET /api/dashboard -> esta tela.
// (Antes esta tela usava números de mentira escritos no código.)

const moeda = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }))
const numero = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 2 }))
const dataHora = (t) => (t ? new Date(t).toLocaleString('pt-BR') : '—')

const lote = ref(null)          // dados do lote (arquivo, status, totais)
const d = ref(null)             // JSON que o Python gravou (null enquanto não termina)
const erro = ref('')
const carregando = ref(false)
let relogio = null

async function buscar() {
  carregando.value = true
  erro.value = ''
  try {
    const resp = await dashboardService.obter()
    lote.value = resp.lote
    d.value = resp.estatisticas
  } catch (e) {
    lote.value = null
    d.value = null
    erro.value = mensagemDeErro(e) // 404 = ainda não existe nenhum lote
  } finally {
    carregando.value = false
  }
  // Se o Python ainda está trabalhando, pergunta de novo a cada 5 s
  const processando = lote.value?.status === 'PROCESSANDO'
  if (processando && !relogio) relogio = setInterval(buscar, 5000)
  if (!processando && relogio) { clearInterval(relogio); relogio = null }
}

onMounted(buscar)
onBeforeUnmount(() => relogio && clearInterval(relogio))

// ---------- dados para a tela ----------
const segmentos = computed(() => d.value?.porSegmento ?? [])
const niveis = computed(() => {
  const cores = { A: '#6d5bf8', B: '#38bdf8', C: '#f59e0b' }
  return (d.value?.porNivel ?? []).map((n) => ({ nome: 'Nível ' + n.nivel, valor: n.clientes, cor: cores[n.nivel] || '#94a3b8' }))
})
const meses = computed(() => d.value?.porMes ?? [])
const consultores = computed(() => d.value?.porConsultor ?? [])
const topClientes = computed(() => d.value?.topClientes ?? [])
const insights = computed(() => d.value?.insights ?? [])

const kpis = computed(() => {
  const r = d.value?.resumo
  const a = (d.value?.porNivel ?? []).find((n) => n.nivel === 'A')?.clientes ?? 0
  const total = r?.totalClientes ?? 0
  return [
    { label: 'Clientes', value: numero(total), delta: `${d.value?.qualidade?.linhasInvalidas ?? 0} linhas descartadas` },
    { label: 'Faturamento total', value: moeda(r?.faturamentoTotal), delta: `Média ${moeda(r?.media)}` },
    { label: 'Consultores', value: consultores.value.length, delta: 'com carteira no lote' },
    { label: 'Clientes nível A', value: a, delta: total ? `${((a / total) * 100).toFixed(1).replace('.', ',')}% da base` : '—' },
  ]
})

// donut
const raio = 52
const circunferencia = 2 * Math.PI * raio
const totalNiveis = computed(() => niveis.value.reduce((s, n) => s + n.valor, 0))
const donutSegmentos = computed(() => {
  let acumulado = 0
  return niveis.value.map((n) => {
    const fracao = totalNiveis.value ? n.valor / totalNiveis.value : 0
    const item = { ...n, comprimento: fracao * circunferencia, offset: -acumulado * circunferencia }
    acumulado += fracao
    return item
  })
})

// linha (clientes por mês)
const pontos = computed(() => {
  const v = meses.value.map((m) => m.clientes)
  if (v.length < 2) return ''
  const max = Math.max(...v), min = Math.min(...v), faixa = max - min || 1
  return v.map((y, i) => `${(i / (v.length - 1)) * 100},${100 - ((y - min) / faixa) * 100}`).join(' ')
})

const maiorSegmento = computed(() => Math.max(1, ...segmentos.value.map((s) => s.clientes)))
const maiorConsultor = computed(() => Math.max(1, ...consultores.value.map((c) => c.faturamentoTotal)))
</script>

<template>
  <div class="min-h-screen bg-[#070a17] text-slate-100 flex">
    <NavSidebar />

    <div class="flex-1 pt-16 md:pt-0">
      <div class="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 sm:py-8 lg:px-8">

        <header class="mb-6">
          <h1 class="text-2xl font-bold text-white sm:text-3xl">Dashboard</h1>
          <p v-if="lote" class="mt-1 text-sm text-slate-400">
            Arquivo <strong class="text-slate-200">{{ lote.arquivoNome }}</strong> · enviado em {{ dataHora(lote.enviadoEm) }} · {{ lote.status }}
          </p>
        </header>

        <p v-if="carregando && !lote && !erro" class="text-slate-400 text-sm">Carregando...</p>

        <!-- Sem dados ainda / erro -->
        <section v-if="erro" role="alert" class="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-6">
          <p class="text-sm font-semibold text-amber-300">{{ erro }}</p>
          <RouterLink to="/upload" class="mt-3 inline-block text-sm text-indigo-400 hover:text-indigo-300 font-medium">Ir para o upload</RouterLink>
        </section>

        <p v-else-if="lote && lote.status === 'PROCESSANDO'" class="text-slate-400 text-sm">
          O Python ainda está processando a planilha. Esta tela atualiza sozinha.
        </p>
        <p v-else-if="lote && lote.status === 'ERRO'" class="text-red-300 text-sm" role="alert">
          O processamento falhou: {{ lote.mensagem }}
        </p>

        <template v-if="d">
          <!-- KPIs -->
          <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <article v-for="kpi in kpis" :key="kpi.label" class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5">
              <p class="text-sm text-slate-400">{{ kpi.label }}</p>
              <p class="mt-2 text-2xl font-bold text-white break-words">{{ kpi.value }}</p>
              <p class="mt-2 text-xs font-medium text-indigo-400">{{ kpi.delta }}</p>
            </article>
          </section>

          <!-- Evolução + níveis -->
          <section class="mt-6 grid gap-4 lg:grid-cols-[2fr_1fr]">
            <article class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5 sm:p-6">
              <h2 class="font-bold text-white">Contratações por mês</h2>
              <p class="text-xs text-slate-400">Clientes que contrataram em cada mês</p>
              <svg v-if="pontos" viewBox="0 0 100 100" preserveAspectRatio="none" class="mt-6 h-48 w-full">
                <polyline :points="pontos" fill="none" stroke="#6d5bf8" stroke-width="1.5" vector-effect="non-scaling-stroke" />
              </svg>
              <p v-else class="mt-6 text-sm text-slate-500">Dados insuficientes para o gráfico (é preciso mais de um mês).</p>
              <div class="mt-2 flex justify-between gap-1 overflow-x-auto text-xs text-slate-500">
                <span v-for="m in meses" :key="m.mes">{{ m.mes }}</span>
              </div>
            </article>

            <article class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5 sm:p-6">
              <h2 class="font-bold text-white">Distribuição por nível</h2>
              <div class="mt-4 flex justify-center">
                <svg viewBox="0 0 120 120" class="h-32 w-32 -rotate-90">
                  <circle v-for="seg in donutSegmentos" :key="seg.nome" cx="60" cy="60" :r="raio" fill="none" :stroke="seg.cor"
                    stroke-width="14" :stroke-dasharray="`${seg.comprimento} ${circunferencia}`" :stroke-dashoffset="seg.offset" />
                </svg>
              </div>
              <ul class="mt-4 space-y-3">
                <li v-for="n in niveis" :key="n.nome" class="flex items-center justify-between text-sm">
                  <span class="flex items-center gap-2 text-slate-300">
                    <span class="h-2 w-2 rounded-full" :style="{ backgroundColor: n.cor }"></span>{{ n.nome }}
                  </span>
                  <span class="font-semibold text-white">{{ n.valor }}</span>
                </li>
              </ul>
            </article>
          </section>

          <!-- Segmentos -->
          <section class="mt-6 grid gap-4 lg:grid-cols-2">
            <article class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5 sm:p-6">
              <h2 class="font-bold text-white">Clientes por segmento</h2>
              <div class="mt-6 space-y-4">
                <div v-for="s in segmentos" :key="s.segmento">
                  <div class="mb-1 flex justify-between text-xs text-slate-400">
                    <span>{{ s.segmento }}</span><span>{{ s.clientes }} ({{ numero(s.percentual) }}%)</span>
                  </div>
                  <div class="h-3 w-full overflow-hidden rounded-full bg-[#070a17]">
                    <div class="h-full bg-indigo-400" :style="{ width: (s.clientes / maiorSegmento) * 100 + '%' }"></div>
                  </div>
                </div>
              </div>
            </article>

            <article class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5 sm:p-6">
              <h2 class="font-bold text-white">Faturamento por consultor</h2>
              <div class="mt-6 space-y-4">
                <div v-for="c in consultores" :key="c.consultor">
                  <div class="mb-1 flex justify-between text-xs text-slate-400">
                    <span>{{ c.consultor }} · {{ c.clientes }} clientes</span><span>{{ moeda(c.faturamentoTotal) }}</span>
                  </div>
                  <div class="h-3 w-full overflow-hidden rounded-full bg-[#070a17]">
                    <div class="h-full bg-sky-400" :style="{ width: (c.faturamentoTotal / maiorConsultor) * 100 + '%' }"></div>
                  </div>
                </div>
              </div>
            </article>
          </section>

          <!-- Tabela de segmentos (no lugar da matriz segmento x nível, que o Python não calcula) -->
          <section class="mt-6 overflow-hidden rounded-2xl border border-indigo-500/10 bg-[#0e1226]">
            <header class="border-b border-indigo-500/10 p-4 sm:p-5"><h2 class="font-bold text-white">Resumo por segmento</h2></header>
            <div class="overflow-x-auto">
              <table class="min-w-full text-sm">
                <thead class="bg-[#070a17]">
                  <tr>
                    <th class="px-4 py-3 text-left font-semibold text-slate-300">Segmento</th>
                    <th class="px-4 py-3 text-left font-semibold text-slate-300">Clientes</th>
                    <th class="px-4 py-3 text-left font-semibold text-slate-300">% da base</th>
                    <th class="px-4 py-3 text-left font-semibold text-slate-300">Faturamento médio</th>
                    <th class="px-4 py-3 text-left font-semibold text-slate-300">Mediana</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in segmentos" :key="s.segmento" class="border-t border-indigo-500/10">
                    <td class="px-4 py-3 font-semibold text-white">{{ s.segmento }}</td>
                    <td class="px-4 py-3 text-slate-300">{{ s.clientes }}</td>
                    <td class="px-4 py-3 text-indigo-400">{{ numero(s.percentual) }}%</td>
                    <td class="px-4 py-3 text-slate-300">{{ moeda(s.media) }}</td>
                    <td class="px-4 py-3 text-slate-300">{{ moeda(s.mediana) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <!-- Maiores clientes + insights -->
          <section class="mt-6 grid gap-4 lg:grid-cols-2">
            <article class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5 sm:p-6">
              <h2 class="font-bold text-white">Maiores clientes</h2>
              <ul class="mt-4 space-y-3 text-sm">
                <li v-for="c in topClientes" :key="c.codigo" class="flex justify-between gap-3">
                  <span class="text-slate-300">{{ c.nome }} <span class="text-slate-500">({{ c.codigo }})</span></span>
                  <span class="font-semibold text-white whitespace-nowrap">{{ moeda(c.faturamento) }}</span>
                </li>
              </ul>
            </article>

            <article v-if="insights.length" class="rounded-2xl border border-indigo-500/10 bg-[#0e1226] p-5 sm:p-6">
              <h2 class="font-bold text-white">Insights</h2>
              <ul class="mt-4 list-disc space-y-2 pl-5 text-sm text-slate-300">
                <li v-for="(frase, i) in insights" :key="i">{{ frase }}</li>
              </ul>
            </article>
          </section>
        </template>

      </div>
    </div>
  </div>
</template>
