import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:polimarket_app/main.dart';

void main() {
  testWidgets('Solicita credenciales y valida antes de conectar', (tester) async {
    await tester.pumpWidget(const PoliMarketApp());
    expect(find.text('PoliMarket'), findsOneWidget);
    expect(find.byType(TextField), findsNWidgets(3));
    await tester.tap(find.text('Ingresar'));
    await tester.pump();
    expect(find.text('Ingresa una URL válida, usuario y contraseña.'), findsOneWidget);
  });
}

