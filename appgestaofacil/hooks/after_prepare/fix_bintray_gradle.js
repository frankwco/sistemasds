#!/usr/bin/env node

/**
 * O JFrog Bintray/JCenter foi desativado em 2021, mas o template do
 * cordova-android 8.x/9.x ainda referencia o "gradle-bintray-plugin" (usado
 * só pra PUBLICAR a CordovaLib no Bintray, não pra compilar o app) no
 * platforms/android/CordovaLib/build.gradle. Isso quebra qualquer build,
 * mesmo local, com "Could not find com.jfrog.bintray.gradle:...".
 *
 * Esse arquivo é gerado do zero toda vez que a plataforma android é
 * (re)adicionada ou preparada — não dá pra só editar ele uma vez. Esse hook
 * roda depois de "cordova prepare" e remove as referências ao bintray,
 * deixando o resto do arquivo intacto. Idempotente: se já não tiver
 * "bintray" no arquivo, não faz nada.
 *
 * Cordova executa hooks/after_prepare/*.js como script simples de linha de
 * comando (node script.js <projectRoot>), não como módulo com "context" —
 * por isso o código roda direto no topo do arquivo, sem module.exports.
 */

const fs = require('fs');
const path = require('path');

const raiz = process.argv[2] || process.cwd();
const gradlePath = path.join(raiz, 'platforms', 'android', 'CordovaLib', 'build.gradle');

if (!fs.existsSync(gradlePath)) {
  process.exit(0); // plataforma android não foi (ainda) adicionada
}

let conteudo = fs.readFileSync(gradlePath, 'utf8');

if (conteudo.includes('bintray')) {
  conteudo = conteudo
    // dependência do plugin em si (não existe mais em lugar nenhum acessível)
    .replace(/\s*classpath 'com\.jfrog\.bintray\.gradle:gradle-bintray-plugin:[^']*'\n/, '\n')
    // aplicação do plugin
    .replace(/\s*apply plugin: 'com\.jfrog\.bintray'\n/, '\n')
    // bloco de configuração do bintray (só usado no "publish", nunca no build normal do app)
    .replace(/\nbintray \{[\s\S]*?\n\}\n/, '\n');

  fs.writeFileSync(gradlePath, conteudo, 'utf8');
  console.log('[hooks/after_prepare] Removidas referências ao Bintray (desativado) de CordovaLib/build.gradle');
}
