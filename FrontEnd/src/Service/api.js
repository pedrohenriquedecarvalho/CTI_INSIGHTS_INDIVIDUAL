// src/Service/api.js — TODA conversa com o backend (Spring Boot) passa por aqui.
import axios from 'axios'

// Endereço do backend. Antes apontava para http://localhost:3000 (não existe): o Spring roda na 8080, em /api.
// Pode ser trocado criando FrontEnd/.env com VITE_API_URL (veja .env.example).
const BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '')

export const api = axios.create({ baseURL: BASE, timeout: 60000 })

// Antes de cada requisição, envia o token do login (se existir) para o backend saber quem chamou
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Em toda resposta: se o token venceu (401 numa rota privada), limpa o login e volta para a tela de entrada
api.interceptors.response.use(
  (resposta) => resposta,
  (erro) => {
    const url = erro.config?.url || ''
    const publica = url.includes('/auth/login') || url.includes('/auth/register') // nessas, 401 = senha errada
    if (erro.response?.status === 401 && !publica) {
      localStorage.removeItem('token')
      localStorage.removeItem('perfil')
      localStorage.removeItem('nome')
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    return Promise.reject(erro)
  }
)

// Transforma qualquer erro numa frase para a tela. O backend responde { "erro": "texto", "campos": { campo: "texto" } }.
export function mensagemDeErro(erro) {
  const dados = erro?.response?.data
  if (dados?.campos && Object.keys(dados.campos).length) return Object.values(dados.campos).join(' ')
  if (dados?.erro) return dados.erro
  if (erro?.response) return `Erro ${erro.response.status} no servidor.`
  return 'Não consegui falar com o servidor. Ele está rodando? (mvn spring-boot:run)'
}

const corpo = (resposta) => resposta.data

export const authService = {
  entrar: (email, senha) => api.post('/auth/login', { email, senha }).then(corpo),
  cadastrar: (dados) => api.post('/auth/register', dados).then(corpo),
  eu: () => api.get('/auth/me').then(corpo),
}

export const adminService = {
  listar: (status) => api.get('/admin/usuarios', { params: { status } }).then(corpo),
  alterarStatus: (id, status) => api.patch(`/admin/usuarios/${id}/status`, { status }).then(corpo),
}

export const uploadService = {
  enviarValidadas: (dados) => api.post('/uploads/validadas', dados).then(corpo),
}

export const dashboardService = {
  obter: (loteId) => api.get('/dashboard', { params: { loteId } }).then(corpo),
  lotes: () => api.get('/dashboard/lotes').then(corpo),
}

export default api // os arquivos antigos importam "api" como padrão
