#!/usr/bin/env node

/**
 * O build do Angular/Ionic (`ionic build`) gera www/index.html do zero a
 * cada vez, sem NENHUMA referência a cordova.js — ele não sabe (nem precisa
 * saber) que roda dentro do Cordova. Normalmente é o próprio "cordova
 * prepare" que injeta essa tag automaticamente na cópia final; nesse
 * projeto isso não estava acontecendo de forma confiável (às vezes um
 * rebuild do Angular sobrescreve o index.html já "preparado" antes de um
 * novo prepare rodar de novo em cima) — resultado: window.cordova nunca
 * existe, e QUALQUER plugin nativo (câmera, áudio, etc.) falha com "Cordova
 * is not available", mesmo com tudo mais (plugin, permissão, manifest)
 * correto.
 *
 * Esse hook garante a tag em TODAS as cópias relevantes de index.html
 * depois de cada "cordova prepare" — idempotente (não duplica se já tiver).
 */

const fs = require('fs');
const path = require('path');

const raiz = process.argv[2] || process.cwd();

const candidatos = [
  path.join(raiz, 'platforms', 'android', 'app', 'src', 'main', 'assets', 'www', 'index.html'),
  path.join(raiz, 'platforms', 'android', 'platform_www', 'index.html'),
];

for (const arquivo of candidatos) {
  if (!fs.existsSync(arquivo)) {
    continue;
  }

  let html = fs.readFileSync(arquivo, 'utf8');
  if (html.includes('cordova.js')) {
    continue; // já tem a tag — nada a fazer
  }

  if (!html.includes('<script')) {
    continue; // sem nenhuma tag de script pra usar como referência de posição
  }

  // insere cordova.js logo ANTES do primeiro <script> existente, pra
  // garantir que o bridge nativo esteja pronto antes de qualquer código
  // da aplicação que possa chamar um plugin
  html = html.replace('<script', '<script src="cordova.js"></script><script', 1);
  fs.writeFileSync(arquivo, html, 'utf8');
  console.log('[hooks/after_prepare] Inserida tag <script src="cordova.js"> em ' + arquivo);
}
