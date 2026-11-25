package br.com.dsresiduos.gestaofacil

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App aberto pela 1a vez no botao "Abrir" do instalador: depois, tocar no icone cria outra
        // tela por cima da que ja existe, e o app "volta" para o inicio. Descarta a repetida.
        if (!isTaskRoot && intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "gestaofacil/externo").setMethodCallHandler { chamada, resposta ->
            if (chamada.method != "abrir") {
                resposta.notImplemented()
                return@setMethodCallHandler
            }
            // Em tarefa propria: o WhatsApp nao fica empilhado dentro do app, entao o voltar e o
            // icone do app devolvem para a mesma tela de onde saiu.
            val intencao = Intent(Intent.ACTION_VIEW, Uri.parse(chamada.argument<String>("url")))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intencao)
                resposta.success(true)
            } catch (e: ActivityNotFoundException) {
                resposta.success(false)
            }
        }
    }
}
