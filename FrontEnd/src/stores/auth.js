// src/stores/auth.js
import { defineStore } from 'pinia'
import { authService } from '../Service/api.js'

export const useAuthStore = defineStore('auth', {

  // STATE: lê do localStorage para o login continuar valendo ao recarregar a página
  state: () => ({
    token: localStorage.getItem('token') || '',   // Token devolvido pela API no login
    perfil: localStorage.getItem('perfil') || '', // 'ADMIN' ou 'USER'
    nome: localStorage.getItem('nome') || ''      // Nome para exibir na tela
  }),

  getters: {
    estaLogado: (state) => Boolean(state.token),
    ehAdmin: (state) => state.perfil === 'ADMIN'
  },

  actions: {

    // Chama a API e guarda o resultado. Se a API recusar (senha errada, conta pendente ou bloqueada),
    // o erro sobe para a tela de login mostrar a mensagem.
    // O backend responde { token, tipo, expiraEmSegundos, usuario: { nome, perfil, ... } }
    // (antes o código lia data.perfil e data.nome, que não existem nessa resposta).
    async login(email, senha) {
      const data = await authService.entrar(email, senha)

      this.token = data.token
      this.perfil = data.usuario.perfil
      this.nome = data.usuario.nome

      // O api.js lê o token daqui para enviar em todas as requisições
      localStorage.setItem('token', this.token)
      localStorage.setItem('perfil', this.perfil)
      localStorage.setItem('nome', this.nome)
    },

    logout() {
      this.token = ''
      this.perfil = ''
      this.nome = ''

      localStorage.removeItem('token')
      localStorage.removeItem('perfil')
      localStorage.removeItem('nome')
    }
  }
})
