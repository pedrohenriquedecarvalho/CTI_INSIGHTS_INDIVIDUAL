// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import home from "/src/views/home.vue"
import login from "/src/views/login.vue"
import upload from "/src/views/upload.vue"
import relatorios from "/src/views/relatorios.vue"
import dashboard from "/src/views/dashboard.vue"
import Cadastro from "/src/views/cadastro.vue"
import usuario from "/src/views/usuarios.vue"

const routes = [
  { path: '/', name: 'home', component: home },
  { path: '/login', name: 'login', component: login },
  { path: '/cadastro', name: 'cadastro', component: Cadastro },

  // Telas internas: exigem estar logado (antes qualquer pessoa abria /dashboard e /upload sem login)
  { path: '/upload', name: 'upload', component: upload, meta: { requiresAuth: true } },
  { path: '/relatorios', name: 'relatorios', component: relatorios, meta: { requiresAuth: true } },
  { path: '/dashboard', name: 'dashboard', component: dashboard, meta: { requiresAuth: true } },

  // Só ADMIN abre esta tela (o backend também confere em /api/admin/**; o guard só esconde a página)
  { path: '/usuario', name: 'usuario', component: usuario, meta: { requiresAuth: true, requiresAdmin: true } },

  // Qualquer endereço desconhecido volta para o início
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

// Guard de rota: lê o token e o perfil que o login salva no localStorage
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const perfil = localStorage.getItem('perfil')

  if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !token) return { name: 'login' } // não logado
  if (to.meta.requiresAdmin && perfil !== 'ADMIN') return { name: 'dashboard' }            // logado, mas não é admin

  return true
})

export default router
