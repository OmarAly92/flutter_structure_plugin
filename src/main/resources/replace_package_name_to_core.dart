import 'dart:io';

void main(List<String> args) {
  if (args.length != 1) {
    exit(1);
  }

  final newPackageName = args[0];
  final oldPackageName = 'package:my_structure/';
  final newPackageImport = 'package:$newPackageName/';

  final dir = Directory('lib/core');
  for (var file in dir.listSync(recursive: true)) {
    if (file is File && file.path.endsWith('.dart')) {
      final content = file.readAsStringSync();
      final updated = content.replaceAll(oldPackageName, newPackageImport);
      file.writeAsStringSync(updated);
    }
  }
}