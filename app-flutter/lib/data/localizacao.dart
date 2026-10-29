import 'package:geolocator/geolocator.dart';

/// Posição do aparelho no momento da coleta.
class Posicao {
  const Posicao(this.latitude, this.longitude);

  final double latitude;
  final double longitude;
}

/// Obtém a posição atual do aparelho. Abstrato para os testes não dependerem do GPS.
abstract class Localizador {
  /// Posição atual, ou `null` se a permissão foi negada, o GPS está desligado ou
  /// não houve resposta a tempo. Nunca lança: a coleta não pode falhar por causa disso.
  Future<Posicao?> obter();
}

class LocalizadorGps implements Localizador {
  const LocalizadorGps({this.limite = const Duration(seconds: 8)});

  final Duration limite;

  @override
  Future<Posicao?> obter() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) return null;
      var permissao = await Geolocator.checkPermission();
      if (permissao == LocationPermission.denied) {
        permissao = await Geolocator.requestPermission();
      }
      if (permissao == LocationPermission.denied || permissao == LocationPermission.deniedForever) {
        return null;
      }
      final p = await Geolocator.getCurrentPosition(
        locationSettings: LocationSettings(accuracy: LocationAccuracy.high, timeLimit: limite),
      );
      return Posicao(p.latitude, p.longitude);
    } catch (_) {
      // Sem GPS a tempo: cai na última posição conhecida, se houver.
      try {
        final p = await Geolocator.getLastKnownPosition();
        return p == null ? null : Posicao(p.latitude, p.longitude);
      } catch (_) {
        return null;
      }
    }
  }
}

/// Usado quando não há localização (testes, ou recurso desligado).
class SemLocalizador implements Localizador {
  const SemLocalizador();

  @override
  Future<Posicao?> obter() async => null;
}
